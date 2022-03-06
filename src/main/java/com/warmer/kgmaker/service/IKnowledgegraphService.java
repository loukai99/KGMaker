package com.warmer.kgmaker.service;


import java.util.List;
import java.util.Map;

public interface IKnowledgegraphService {
	List<Map<String,Object>> getDomains();
	List<Map<String,Object>> getDomainList(String domainname,String createuser);
	void saveDomain(Map<String, Object> map);
	void updateDomain(Map<String, Object> map);
	void deleteDomain(Integer id);
	List<Map<String,Object>> getDomainByName(String domainname);
	List<Map<String,Object>> getDomainById(Integer domainid);
}