package com.cis.gw21c.client;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import javax.xml.ws.BindingProvider;
import javax.xml.namespace.QName;

import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.service.DispatchContext;
import org.ofbiz.service.ServiceUtil;


public class TaskClientService {

	public static Map<String, Object> gw21cAddTask(DispatchContext dctx, Map context) {
		GenericDelegator delegator = (GenericDelegator) dctx.getDelegator();
		String name = (String) context.get("workEffortName");
		String description = (String) context.get("description");
		if (name == null) {
			name = "";
		}
		if (description == null) {
			description = "";
		}
		try {
			Map result = ServiceUtil.returnSuccess();
			URL url = new URL("file:TaskService.xml");
			com.cis.gw21c.client.TaskService service = new com.cis.gw21c.client.TaskService(
					url, new QName("http://gw21c.cis.com", "TaskService"));
			com.cis.gw21c.client.TaskServicePortType port = service.getTaskServiceSoap();
			BindingProvider bp = (BindingProvider) port;
			bp.getRequestContext().put(BindingProvider.USERNAME_PROPERTY, "admin");
			bp.getRequestContext().put(BindingProvider.PASSWORD_PROPERTY, "111");
			result.put("result", port.addTask(name, description));
			return result;
		} catch (Exception ex) {
			return ServiceUtil.returnError("OFBIZ Error: " + ex.getMessage());
		}

	}

}