/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package com.cis.gw21c;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;


import org.ofbiz.entity.DelegatorFactory;
import org.ofbiz.entity.GenericDelegator;
import org.ofbiz.entity.GenericValue;

import org.ofbiz.service.LocalDispatcher;
import org.ofbiz.service.GenericDispatcher;

import org.ofbiz.base.util.UtilDateTime;
/**
 *
 */
public class TaskService {

	private GenericDelegator delegator;
	private LocalDispatcher dispatcher;

	public TaskService() {
		delegator = (GenericDelegator) DelegatorFactory.getDelegator("default");
		dispatcher = GenericDispatcher.getLocalDispatcher("default", delegator);
	}

	public TaskListType TaskList() {


		try {
			List<GenericValue> lst = delegator.findList("WorkEffortAndPartyAssign", null, null, null, null, false);

			TaskListType taskList = new TaskListType();

			List<TaskType> lstTask = new ArrayList<TaskType>();

			for(GenericValue item: lst) {
				TaskType task = new TaskType();
				task.setName(item.getString("workEffortName"));
				lstTask.add(task);
			}
			taskList.setTasks(lstTask);

			return taskList;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	private String getId(String id) {
		return "GW21C-" + id;
	}

	
	public String changeTask(String id, String name, String description) {
		try {
			String sysId = getId(id);
			GenericValue we = delegator.makeValue("WorkEffort");
			we.set("workEffortId", sysId);
			we.set("workEffortTypeId", "TASK");
			we.set("currentStatusId", "CAL_ACCEPTED");
			we.set("workEffortName", name);
			we.set("description", description);
			we.set("estimatedStartDate", UtilDateTime.toDate("01/01/2011 00:00:00"));
			we.set("estimatedCompletionDate", UtilDateTime.toDate("01/01/2014 00:00:00"));
			delegator.createOrStore(we);

			//WorkEffortPartyAssignment
			we = delegator.makeValue("WorkEffortPartyAssignment");
			we.set("workEffortId", sysId);
			we.set("roleTypeId", "CAL_OWNER");
			we.set("partyId", "admin");
			we.set("statusId", "PRTYASGN_ASSIGNED");
			we.set("statusDateTime", new Timestamp(System.currentTimeMillis()));
			we.set("fromDate", UtilDateTime.toDate("01/01/2011 00:00:00"));
			delegator.createOrStore(we);
			return sysId;
		} catch (Exception ex) {
			return "OFBiz Error: " + ex.getMessage();
		}
	}

	public String removeTask(String id) {
		String sysId = getId(id);
		try {

			GenericValue we = delegator.makeValue("WorkEffort");
			we.set("workEffortId", sysId);
			we.removeRelated("WorkEffortKeyword");
			we.removeRelated("WorkEffortPartyAssignment");
			we.removeRelated("WorkEffortStatus");
			we.remove();
			return sysId;
		} catch (Exception ex) {
			return "OFBiz Error: " + ex.getMessage();
		}
	}

}