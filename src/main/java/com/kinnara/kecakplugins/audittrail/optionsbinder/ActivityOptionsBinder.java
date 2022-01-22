package com.kinnara.kecakplugins.audittrail.optionsbinder;

import com.kinnara.kecakplugins.audittrail.optionsbinder.generator.ActivityOptionsGenerator;
import com.kinnara.kecakplugins.audittrail.optionsbinder.generator.OptionsGenerator;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.PackageDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.workflow.model.WorkflowActivity;
import org.joget.workflow.model.WorkflowProcess;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.model.service.WorkflowUserManager;
import org.joget.workflow.util.WorkflowUtil;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.*;

public class ActivityOptionsBinder extends FormBinder implements FormLoadOptionsBinder, PluginWebSupport{
	/**
	 * Option Binder
	 */
	public FormRowSet load(Element element, String primaryKey, FormData formData) {
		AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();
		final FormRowSet result = new FormRowSet();
		ActivityOptionsGenerator generator = new ActivityOptionsGenerator(appDefinition, "", getPropertyString("type"));
		generator.generate(result::add);
			
		return result;
	}

	public String getLabel() {
		return "Activity Options Binder";
	}

	public String getClassName() {
		return getClass().getName();
	}

	public String getPropertyOptions() {
		return AppUtil.readPluginResource(getClassName(), "/properties/ActivityOptionBinder.json", null, false, "/messages/ActivityOptionsBinder");
	}

	public String getName() {
		return getClass().getName();
	}

	public String getVersion() {
		return getClass().getPackage().getImplementationVersion();
	}

	public String getDescription() {
		return getClass().getPackage().getImplementationTitle();
	}

	/**
	 * Web service
	 */
	public void webService(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		boolean isAdmin = WorkflowUtil.isCurrentUserInRole(WorkflowUserManager.ROLE_ADMIN);
		if (!isAdmin) {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			return;
		}

		String type = request.getParameter("type");
		String processId = Optional.ofNullable(request.getParameter("processId")).orElse("");
		
		final JSONArray result = new JSONArray();
		
		try {
			// add empty record
			JSONObject empty = new JSONObject();
			empty.put(FormUtil.PROPERTY_VALUE, "");
			empty.put(FormUtil.PROPERTY_LABEL, "");
			result.put(empty);
		} catch (JSONException e) {
			e.printStackTrace();
		}

		AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();
		ActivityOptionsGenerator generator = new ActivityOptionsGenerator(appDefinition, processId, type == null ? "" : type);
		generator.generate(row -> {
                JSONObject item = new JSONObject();
                try {
                    item.put(FormUtil.PROPERTY_VALUE, row.getProperty(FormUtil.PROPERTY_VALUE));
                    item.put(FormUtil.PROPERTY_LABEL, row.getProperty(FormUtil.PROPERTY_LABEL));
					result.put(item);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
        });
		
		response.setStatus(HttpServletResponse.SC_OK);
		response.setContentType("application/json");
		response.getWriter().write(result.toString());
	}

	public final FormRowSet generate(String processId) {
		ApplicationContext appContext = AppUtil.getApplicationContext();
		WorkflowManager wfManager = (WorkflowManager)appContext.getBean("workflowManager");
		AppService appService = (AppService) appContext.getBean("appService");

		AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();
		PackageDefinition packageDefinition = appDefinition.getPackageDefinition();
		Long packageVersion = (packageDefinition != null) ? packageDefinition.getVersion() : 1L;
		Collection<WorkflowProcess> processes = processId.isEmpty() ? wfManager.getProcessList(appDefinition.getAppId(), packageVersion.toString()) : Collections.singleton(appService.getWorkflowProcessForApp(appDefinition.getId(), appDefinition.getVersion().toString(), processId));

		Set<String> uniqueName = new HashSet<>();

		final FormRowSet rowSet = new FormRowSet();
		for(WorkflowProcess process : processes) {
			Collection<WorkflowActivity> activities = wfManager.getProcessActivityDefinitionList(process.getId());
			for(WorkflowActivity activity : activities) {
				if(uniqueName.add(activity.getId())) {
					FormRow row = new FormRow();
					row.put(FormUtil.PROPERTY_VALUE, activity.getId());
					row.put(FormUtil.PROPERTY_LABEL, activity.getName() + " ("+activity.getId()+")");
					rowSet.add(row);
				}
			}
		}

		return rowSet;
	}
}
