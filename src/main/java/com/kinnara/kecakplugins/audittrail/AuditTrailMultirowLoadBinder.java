package com.kinnara.kecakplugins.audittrail;

import org.enhydra.shark.api.common.SharkConstants;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.*;
import org.joget.commons.util.LogUtil;
import org.joget.directory.model.User;
import org.joget.directory.model.service.ExtDirectoryManager;
import org.joget.workflow.model.WorkflowActivity;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.WorkflowProcess;
import org.joget.workflow.model.service.WorkflowManager;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

public class AuditTrailMultirowLoadBinder extends FormBinder
implements FormLoadBinder,
		FormLoadMultiRowElementBinder{

	public String getLabel() {
		return getName();
	}

	public String getClassName() {
		return getClass().getName();
	}

	public String getPropertyOptions() {
		return AppUtil.readPluginResource(getClassName(), "/properties/AuditTrailMultirowLoadBinder.json", null, false, "/messages/AuditTrailMultirowLoadBinder");
	}

	public String getName() {
		return AppPluginUtil.getMessage("auditTrailMultirowLoadBinder.title", getClassName(), "/messages/AuditTrailMultirowLoadBinder");
	}

	public String getVersion() {
		return getClass().getPackage().getImplementationVersion();
	}

	public String getDescription() {
		return "Artifact ID : " + getClass().getPackage().getImplementationTitle();
	}

	public FormRowSet load(Element element, String primaryKey, FormData formData) {
		ApplicationContext appContext = AppUtil.getApplicationContext();
		AppService appService = (AppService) appContext.getBean("appService");
		WorkflowManager workflowManager = (WorkflowManager)appContext.getBean("workflowManager");
		WorkflowAssignment wfAssignment = workflowManager.getAssignment(formData.getActivityId());
		FormDataDao formDataDao = (FormDataDao) appContext.getBean("formDataDao");
        Form form = AuditTrailUtil.generateForm(getPropertyString("formDefId"));

        if(form == null) {
            LogUtil.warn(getClassName(), "Form ["+getPropertyString("formDefId")+"] cannot be defined");
            return null;
        }

        String sqlFilterCondition = getPropertyString("sqlFilterCondition");

        FormRowSet rowSet =
                formDataDao.find(form, "WHERE e.customProperties." + getPropertyString("fieldProcessId") + "=?" +
                        (sqlFilterCondition != null && !sqlFilterCondition.isEmpty() ?
                                (" AND ( " + AppUtil.processHashVariable(sqlFilterCondition, wfAssignment, null, null) + " )") : ""),
                new Object[] { primaryKey }, "dateCreated", true, null, null);

		String originProcessId = appService.getOriginProcessId(primaryKey);
		WorkflowProcess workflowProcess = workflowManager.getRunningProcessById(originProcessId);

        if("true".equalsIgnoreCase(getPropertyString("pendingActivity")) && workflowProcess.getState().equals(SharkConstants.STATE_OPEN_RUNNING)) {
            final FormRow row = new FormRow();
			Object[] pendingActivityValues = (Object[]) getProperty("pendingActivityValues");
			Arrays.stream(pendingActivityValues)
					.map(cols -> (Map<String, Object>) cols)
					.forEach(cols -> {
						String field = cols.get("field").toString();
						String type = cols.get("type").toString();
						String value = cols.get("value").toString();
						switch (type) {
							case "pendingUser":
								row.setProperty(field, readPendingUser(workflowProcess.getInstanceId()));
								break;
							case "pendingActivity":
								row.setProperty(field, readPendingActivity(workflowProcess.getInstanceId()));
								break;
							default:
								row.setProperty(field, AppUtil.processHashVariable(value, wfAssignment, null, null));
						}
					});

			rowSet.add(0, row);
        }
        return rowSet;
	}

	private String readPendingUser(String processId) {
		WorkflowManager wfManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");
		Collection<WorkflowActivity> activities = wfManager.getActivityList(processId, 0, 1, "dateCreated", true);
		return activities.stream()
				.filter(activity -> SharkConstants.STATE_OPEN_NOT_RUNNING_NOT_STARTED.equals(activity.getState()))
				.map(activity -> {
					WorkflowActivity runningActivity = wfManager.getRunningActivityInfo(activity.getId());
					return Arrays.stream(runningActivity.getAssignmentUsers())
							.filter(u -> !u.isEmpty())
							.map(u -> {
								ExtDirectoryManager directoryManager = (ExtDirectoryManager)AppUtil.getApplicationContext().getBean("directoryManager");
								User user = directoryManager.getUserByUsername(u);
								if(user == null){
									LogUtil.warn(getClassName(), "Username [" + u + "] in process ["+activity.getProcessId()+"] is not listed in User Directory");
									return u;
								} else {
									return user.getFirstName() + " " + user.getLastName();
								}
							})
							.filter(u -> !u.isEmpty())
							.collect(Collectors.joining(","));
				})
				.collect(Collectors.joining(","));
	}

	private String readPendingActivity(String processId) {
		WorkflowManager wfManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");
		Collection<WorkflowActivity> activities = wfManager.getActivityList(processId, 0, 1, "dateCreated", true);
		return activities.stream()
				.filter(activity -> SharkConstants.STATE_OPEN_NOT_RUNNING_NOT_STARTED.equals(activity.getState()))
				.map(WorkflowActivity::getName)
				.collect(Collectors.joining(","));
	}
}
