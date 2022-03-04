package com.warmer.kgmaker.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.csvreader.CsvWriter;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.warmer.kgmaker.config.WebAppConfig;
import com.warmer.kgmaker.entity.QAEntityItem;
import com.warmer.kgmaker.query.GraphQuery;
import com.warmer.kgmaker.service.IKGGraphService;
import com.warmer.kgmaker.service.IKnowledgegraphService;
import com.warmer.kgmaker.util.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.Transaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

@Controller
@RequestMapping(value = "/")
public class KGManagerController extends BaseController {
	@Autowired
	private Neo4jUtil neo4jUtil;
	@Autowired
	private IKGGraphService KGGraphService;
	@Autowired
	private IKnowledgegraphService kgservice;
	@Autowired
	private Driver neo4jDriver;
	@GetMapping("/")
	public String home(Model model) {
		return "kg/home";
	}
	@GetMapping("/kg/index")
	public String index(Model model) {
		return "kg/index";
	}

	@ResponseBody
	@PostMapping("/saveProperties")
	public R saveProperties(@RequestParam("label")String label, @RequestParam("id") String id, @RequestParam("properties") String properties){
		R result = new R();
		JSONObject jsonObject = JSON.parseObject(properties);
		try{
			if (KGGraphService.saveProperties(label, id, jsonObject)!= null){
				result.code = 200;
				result.setMsg("保存成功");
			}else{
				result.code = 400;
				result.msg = "属性值错误";
				result.msg = "属性值错误";
			}


		}catch (Exception e){
			result.code = 500;
			result.msg = "服务器错误";
		}
		return result;
	}
	@ResponseBody
	@GetMapping("/getProperties")
	public R<Map<String, Object>>getProperties(String label, String id){
		R<Map<String, Object>> result = new R<>();
		try{
			Map<String, Object> temp = KGGraphService.getProperties(label, id);
			if(temp == null){
				result.code = 404;
				result.setMsg("无对应记录");
			}else{
				result.code = 200;
				result.setMsg("查询成功");
				result.setData(temp);
			}
		}catch (Exception e){
			e.printStackTrace();
			result.code = 500;
			result.setMsg("服务器错误");
		}
		return result;
	}
	@ResponseBody
	@RequestMapping(value = "/getgraph") // call db.labels
	public R<GraphPageRecord<Map<String, Object>>> getgraph(GraphQuery queryItem) {
		R<GraphPageRecord<Map<String, Object>>> result = new R<GraphPageRecord<Map<String, Object>>>();
		GraphPageRecord<Map<String, Object>> resultRecord = new GraphPageRecord<Map<String, Object>>();
		try {
			String name = "tc";
			PageHelper.startPage(queryItem.getPageIndex(), queryItem.getPageSize(), true);
			List<Map<String, Object>> domainList = kgservice.getDomainList(queryItem.getDomain(), name);
			PageInfo<Map<String, Object>> pageInfo = new PageInfo<Map<String, Object>>(domainList);
			long total = pageInfo.getTotal();
			resultRecord.setPageIndex(queryItem.getPageIndex());
			resultRecord.setPageSize(queryItem.getPageSize());
			resultRecord.setTotalCount(new Long(total).intValue());
			resultRecord.setNodeList(pageInfo.getList());
			result.code = 200;
			result.setData(resultRecord);
		} catch (Exception e) {
			e.printStackTrace();
			result.code = 500;
			result.setMsg("服务器错误");
		}

		return result;
	}

	@ResponseBody
	@RequestMapping(value = "/getdomaingraph")
	public R<HashMap<String, Object>> getDomainGraph(GraphQuery query) {
		R<HashMap<String, Object>> result = new R<HashMap<String, Object>>();
		try {
			HashMap<String, Object> graphData = KGGraphService.getdomaingraph(query);
			result.code = 200;
			result.data = graphData;
			
		} catch (Exception e) {
			e.printStackTrace();
			result.code = 500;
			result.setMsg("服务器错误");
		}
		return result;
	}
	@ResponseBody
	@RequestMapping(value = "/getcypherresult")
	public R<HashMap<String, Object>> getcypherresult(String cypher) {
		R<HashMap<String, Object>> result = new R<HashMap<String, Object>>();
		String error="";
		try {
			HashMap<String, Object> graphData = neo4jUtil.GetGraphNodeAndShip(cypher);
			result.code = 200;
			result.data = graphData;
		} catch (Exception e) {
			e.printStackTrace();
			result.code = 500;
			error=e.getMessage();
			result.setMsg("服务器错误");
		}
		finally {
			if(StringUtil.isNotBlank(error)){
				result.code = 500;
				result.setMsg(error);
			}
		}
		return result;
	}

	@ResponseBody
	@RequestMapping(value = "/getrelationnodecount")
	public R<String> getrelationnodecount(String domain, long nodeid) {
		R<String> result = new R<String>();
		try {
			long totalcount = 0;
			if (!StringUtil.isBlank(domain)) {
				totalcount = KGGraphService.getrelationnodecount(domain, nodeid);
				result.code = 200;
				result.setData(String.valueOf(totalcount));
			}
		} catch (Exception e) {
			e.printStackTrace();
			result.code = 500;
			result.setMsg("服务器错误");
		}
		return result;
	}

	@ResponseBody
	@RequestMapping(value = "/createdomain")
	public R<String> createdomain(String domain) {
		R<String> result = new R<String>();
		try {
			if (!StringUtil.isBlank(domain)) {
				List<Map<String, Object>> domainItem = kgservice.getDomainByName(domain);
				if (domainItem.size() > 0) {
					result.code = 300;
					result.setMsg("领域已存在");
				} else {
					String name = "tc";
					Map<String, Object> maps = new HashMap<String, Object>();
					maps.put("name", domain);
					maps.put("nodecount", 1);
					maps.put("shipcount", 0);
					maps.put("status", 1);
					maps.put("createuser", name);
					kgservice.saveDomain(maps);// 保存到mysql
					KGGraphService.createdomain(domain);// 保存到图数据
					result.code = 200;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			result.code = 500;
			result.setMsg("服务器错误");
		}
		return result;
	}

	@ResponseBody
	@RequestMapping(value = "/getmorerelationnode")
	public R<HashMap<String, Object>> getmorerelationnode(String domain, String nodeid) {
		R<HashMap<String, Object>> result = new R<HashMap<String, Object>>();
		try {
			if (!StringUtil.isBlank(domain)) {
				HashMap<String, Object> graphModel = KGGraphService.getmorerelationnode(domain, nodeid);
				if (graphModel != null) {
					result.code = 200;
					result.setData(graphModel);
					return result;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			result.code = 500;
			result.setMsg("服务器错误");
		}
		return result;
	}

	@ResponseBody
	@RequestMapping(value = "/updatenodename")
	public R<HashMap<String, Object>> updatenodename(String domain, String nodeid, String nodename) {
		R<HashMap<String, Object>> result = new R<HashMap<String, Object>>();
		HashMap<String, Object> graphNodeList = new HashMap<String, Object>();
		try {
			if (!StringUtil.isBlank(domain)) {
				graphNodeList = KGGraphService.updatenodename(domain, nodeid, nodename);
				if (graphNodeList.size() > 0) {
					result.code = 200;
					result.setData(graphNodeList);
					return result;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			result.code = 500;
			result.setMsg("服务器错误");
		}
		return result;
	}
	@ResponseBody
	@RequestMapping(value = "/updateCorrdOfNode")
	public R<String> updateCorrdOfNode(String domain, String uuid, Double fx, Double fy) {
		R<String> result = new R<String>();
		try {
			KGGraphService.updateCorrdOfNode(domain, uuid, fx, fy);
			result.code = 200;
		} catch (Exception e) {
			e.printStackTrace();
			result.code = 500;
			result.setMsg("服务器错误");
		}
		return result;
	}
	@ResponseBody
	@RequestMapping(value = "/createnode")
	public R<HashMap<String, Object>> createnode(QAEntityItem entity, HttpServletRequest request,
			HttpServletResponse response) {
		R<HashMap<String, Object>> result = new R<HashMap<String, Object>>();
		HashMap<String, Object> graphNode = new HashMap<String, Object>();
		try {
			String domain = request.getParameter("domain");
			if (!StringUtil.isBlank(domain)) {
				List<Map<String, Object>> domainItem = kgservice.getDomainByName(domain);
				if (domainItem.size() <= 0) {
					String name = "tc";
					Map<String, Object> maps = new HashMap<String, Object>();
					maps.put("name", domain);
					maps.put("nodecount", 1);
					maps.put("shipcount", 0);
					maps.put("status", 1);
					maps.put("createuser", name);
					kgservice.saveDomain(maps);
				}
			}else{
				result.code = 300;
				result.setMsg("领域已存在");
				return result;
			}
			graphNode=KGGraphService.createnode(domain, entity);
			if (graphNode!=null&&graphNode.size() > 0) {
				result.code = 200;
				result.setData(graphNode);
				return result;
			}
		} catch (Exception e) {
			e.printStackTrace();
			result.code = 500;
			result.setMsg("服务器错误");
		}

		return result;
	}

	@ResponseBody
	@RequestMapping(value = "/batchcreatenode")
	public R<HashMap<String, Object>> batchcreatenode(String domain, String sourcename, String[] targetnames,
			String relation) {
		R<HashMap<String, Object>> result = new R<HashMap<String, Object>>();
		HashMap<String, Object> rss = new HashMap<String, Object>();
		try {
			rss= KGGraphService.batchcreatenode(domain, sourcename, relation, targetnames);
			result.code = 200;
			result.setData(rss);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			result.code = 500;
			result.setMsg("服务器错误");
		}

		return result;
	}

	@ResponseBody
	@RequestMapping(value = "/batchcreatechildnode")
	public R<HashMap<String, Object>> batchcreatechildnode(String domain, String sourceid, Integer entitytype,
			String[] targetnames, String relation) {
		R<HashMap<String, Object>> result = new R<HashMap<String, Object>>();
		HashMap<String, Object> rss = new HashMap<String, Object>();
		try {
			rss= KGGraphService.batchcreatechildnode(domain, sourceid, entitytype, targetnames, relation);
			result.code = 200;
			result.setData(rss);
			return result;

		} catch (Exception e) {
			e.printStackTrace();
			result.code = 500;
			result.setMsg("服务器错误");
		}

		return result;
	}

	@ResponseBody
	@RequestMapping(value = "/batchcreatesamenode")
	public R<List<HashMap<String, Object>>> batchcreatesamenode(String domain, Integer entitytype,
			String[] sourcenames) {
		R<List<HashMap<String, Object>>> result = new R<List<HashMap<String, Object>>>();
		List<HashMap<String, Object>> rss = new ArrayList<HashMap<String, Object>>();
		try {
			rss=KGGraphService.batchcreatesamenode(domain, entitytype, sourcenames);
			result.code = 200;
			result.setData(rss);
		} catch (Exception e) {
			e.printStackTrace();
			result.code = 500;
			result.setMsg("服务器错误");
		}

		return result;
	}

	@ResponseBody
	@RequestMapping(value = "/createlink")
	public R<HashMap<String, Object>> createlink(String domain, long sourceid, long targetid, String ship) {
		R<HashMap<String, Object>> result = new R<HashMap<String, Object>>();
		try {
			HashMap<String, Object> cypherResult = KGGraphService.createlink(domain, sourceid, targetid, ship);
			result.code = 200;
			result.setData(cypherResult);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			result.code = 500;
			result.setMsg("服务器错误");
		}

		return result;
	}

	@ResponseBody
	@RequestMapping(value = "/updatelink")
	public R<HashMap<String, Object>> updatelink(String domain, long shipid, String shipname) {
		R<HashMap<String, Object>> result = new R<HashMap<String, Object>>();
		try {
			HashMap<String, Object> cypherResult = KGGraphService.updatelink(domain, shipid, shipname);
			result.code = 200;
			result.setData(cypherResult);
		} catch (Exception e) {
			e.printStackTrace();
			result.code = 500;
			result.setMsg("服务器错误");
		}
		return result;
	}

	@ResponseBody
	@RequestMapping(value = "/deletenode")
	public R<List<HashMap<String, Object>>> deletenode(String domain, long nodeid) {
		R<List<HashMap<String, Object>>> result = new R<List<HashMap<String, Object>>>();
		try {
			List<HashMap<String, Object>> rList = KGGraphService.deletenode(domain, nodeid);
			result.code = 200;
			result.setData(rList);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			result.code = 500;
			result.setMsg("服务器错误");
		}
		return result;
	}

	@ResponseBody
	@RequestMapping(value = "/deletedomain")
	public R<List<HashMap<String, Object>>> deletedomain(Integer domainid, String domain) {
		R<List<HashMap<String, Object>>> result = new R<List<HashMap<String, Object>>>();
		try {
			kgservice.deleteDomain(domainid);
			KGGraphService.deleteKGdomain(domain);
			result.code = 200;
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			result.code = 500;
			result.setMsg("服务器错误");
		}
		return result;
	}

	@ResponseBody
	@RequestMapping(value = "/deletelink")
	public R<HashMap<String, Object>> deletelink(String domain, long shipid) {
		R<HashMap<String, Object>> result = new R<HashMap<String, Object>>();
		try {
			KGGraphService.deletelink(domain, shipid);
			result.code = 200;
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			result.code = 500;
			result.setMsg("服务器错误");
		}
		return result;
	}

	@ResponseBody
	@RequestMapping(value = "/getnodeimage")
	public R<List<Map<String, Object>>> getNodeImagelist(int domainid, int nodeid) {
		R<List<Map<String, Object>>> result = new R<List<Map<String, Object>>>();
		try {
			List<Map<String, Object>> images = kgservice.getNodeImageList(domainid, nodeid);
			result.code = 200;
			result.setData(images);
		} catch (Exception e) {
			e.printStackTrace();
			result.code = 500;
			result.setMsg("服务器错误");
		}
		return result;
	}

	@ResponseBody
	@RequestMapping(value = "/getnodecontent")
	public R<Map<String, Object>> getNodeContent(int domainid, int nodeid) {
		R<Map<String, Object>> result = new R<Map<String, Object>>();
		try {
			List<Map<String, Object>> contents = kgservice.getNodeContent(domainid, nodeid);
			if (contents != null && contents.size() > 0) {
				result.code = 200;
				result.setData(contents.get(0));
			}
		} catch (Exception e) {
			e.printStackTrace();
			result.code = 500;
			result.setMsg("服务器错误");
		}
		return result;
	}

	@ResponseBody
	@RequestMapping(value = "/getnodedetail")
	public R<Map<String, Object>> getNodeDetail(int domainid, int nodeid) {
		R<Map<String, Object>> result = new R<Map<String, Object>>();
		try {
			Map<String, Object> res = new HashMap<String, Object>();
			res.put("content", "");
			res.put("imagelist", new String[] {});
			List<Map<String, Object>> contents = kgservice.getNodeContent(domainid, nodeid);
			if (contents != null && contents.size() > 0) {
				res.replace("content", contents.get(0).get("Content"));
			}
			List<Map<String, Object>> images = kgservice.getNodeImageList(domainid, nodeid);
			if (images != null && images.size() > 0) {
				res.replace("imagelist", images);
			}
			result.code = 200;
			result.setData(res);
		} catch (Exception e) {
			e.printStackTrace();
			result.code = 500;
			result.setMsg("服务器错误");
		}
		return result;
	}

	@ResponseBody
	@RequestMapping(value = "/savenodeimage")
	public R<String> saveNodeImage(@RequestBody Map<String, Object> params) {
		R<String> result = new R<String>();
		try {
			String username = "tc";
			int domainid = (int) params.get("domainid");
			String nodeid = params.get("nodeid").toString();
			String imagelist = params.get("imagelist").toString();
			List<Map<String, Object>> domainList = kgservice.getDomainById(domainid);
			if (domainList != null && domainList.size() > 0) {
				String domainName = domainList.get(0).get("name").toString();
				kgservice.deleteNodeImage(domainid, Integer.parseInt(nodeid));
				List<Map> imageItems = JSON.parseArray(imagelist, Map.class);
				List<Map<String, Object>> submitItemList = new ArrayList<Map<String, Object>>();
				if (!imageItems.isEmpty()) {
					for (Map<String, Object> item : imageItems) {
						String file = item.get("fileurl").toString();
						int sourcetype = 0;
						Map<String, Object> sb = new HashMap<String, Object>();
						sb.put("file", file);
						sb.put("imagetype", sourcetype);
						sb.put("domainid", domainid);
						sb.put("nodeid", nodeid);
						sb.put("status", 1);
						sb.put("createuser", username);
						sb.put("createtime", DateUtil.getDateNow());
						submitItemList.add(sb);
					}
				}
				if (submitItemList != null && submitItemList.size() > 0) {
					kgservice.saveNodeImage(submitItemList);
					// 更新到图数据库,表明该节点有附件,加个标识,0=没有,1=有
					KGGraphService.updateNodeFileStatus(domainName, Long.parseLong(nodeid), 1);
					result.code = 200;
					result.setMsg("操作成功");
				} else {
					KGGraphService.updateNodeFileStatus(domainName, Long.parseLong(nodeid), 0);
					result.code = 200;
					result.setMsg("操作成功");
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			result.code = 500;
			result.setMsg("服务器错误");
		}
		return result;
	}

	@ResponseBody
	@RequestMapping(value = "/savenodecontent")
	public R<String> savenodecontent(@RequestBody Map<String, Object> params) {
		R<String> result = new R<String>();
		try {
			String username = "tc";
			int domainid = (int) params.get("domainid");
			String nodeid = params.get("nodeid").toString();
			String content = params.get("content").toString();
			List<Map<String, Object>> domainList = kgservice.getDomainById(domainid);
			if (domainList != null && domainList.size() > 0) {
				String domainName = domainList.get(0).get("name").toString();
				// 检查是否存在
				List<Map<String, Object>> items = kgservice.getNodeContent(domainid, Integer.parseInt(nodeid));
				if (items != null && items.size() > 0) {
					Map<String, Object> olditem = items.get(0);
					Map<String, Object> item = new HashMap<String, Object>();
					item.put("domainid", olditem.get("DomainId"));
					item.put("nodeid", olditem.get("NodeId"));
					item.put("content", content);
					item.put("modifyuser", username);
					item.put("modifytime", DateUtil.getDateNow());
					kgservice.updateNodeContent(item);
					result.code = 200;
					result.setMsg("更新成功");
				} else {
					Map<String, Object> sb = new HashMap<String, Object>();
					sb.put("content", content);
					sb.put("domainid", domainid);
					sb.put("nodeid", nodeid);
					sb.put("status", 1);
					sb.put("createuser", username);
					sb.put("createtime", DateUtil.getDateNow());
					if (sb != null && sb.size() > 0) {
						kgservice.saveNodeContent(sb);
						result.code = 200;
						result.setMsg("保存成功");
					}
				}
				// 更新到图数据库,表明该节点有附件,加个标识,0=没有,1=有
				KGGraphService.updateNodeFileStatus(domainName, Long.parseLong(nodeid), 1);
			}

		} catch (Exception e) {
			e.printStackTrace();
			result.code = 500;
			result.setMsg("服务器错误");
		}
		return result;
	}
	
}
