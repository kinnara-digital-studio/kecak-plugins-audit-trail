package com.kinnara.kecakplugins.audittrail;

import org.enhydra.shark.api.common.SharkConstants;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.*;
import org.joget.commons.util.LogUtil;
import org.joget.directory.model.User;
import org.joget.directory.model.service.ExtDirectoryManager;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.WorkflowActivity;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.WorkflowProcess;
import org.joget.workflow.model.dao.WorkflowProcessLinkDao;
import org.joget.workflow.model.service.WorkflowManager;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.stream.Collectors;

@Deprecated
public class AuditTrailMultirowLoadBinder extends FormBinder
implements FormLoadBinder,
		FormLoadMultiRowElementBinder{

	public String getLabel() {
		return "(Deprecated) Audit Trail Multirow Form Load Binder";
	}

	public String getClassName() {
		return getClass().getName();
	}

	public String getPropertyOptions() {
		return AppUtil.readPluginResource(getClassName(), "/properties/AuditTrailMultirowLoadBinder.json", null, false, "/messages/AuditTrailMultirowLoadBinder");
	}

	public String getName() {
		return getLabel() + getVersion();
	}

	public String getVersion() {
		PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
		ResourceBundle resourceBundle = pluginManager.getPluginMessageBundle(getClassName(), "/messages/BuildNumber");
		return resourceBundle.getString("buildNumber");
	}

	public String getDescription() {
		return "Artifact ID : " + getClass().getPackage().getImplementationTitle();
	}

	public FormRowSet load(Element element, String primaryKey, FormData formData) {
		// do not load data for empty primary key
		if(primaryKey == null || primaryKey.isEmpty()) {
			return null;
		}

		ApplicationContext appContext = AppUtil.getApplicationContext();
		WorkflowProcessLinkDao workflowProcessLinkDao = (WorkflowProcessLinkDao) appContext.getBean("workflowProcessLinkDao");
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

        if("true".equalsIgnoreCase(getPropertyString("pendingActivity"))) {
			workflowProcessLinkDao.getLinks(primaryKey).stream()
					.map(l -> workflowManager.getRunningProcessById(l.getProcessId()))
					.filter(Objects::nonNull)
					.filter(p -> Objects.nonNull(p.getState()))
					.filter(p -> p.getState().startsWith(SharkConstants.STATEPREFIX_OPEN))
					.max(Comparator.comparing(WorkflowProcess::getInstanceId))
					.map(p -> {
						Object[] pendingActivityValues = (Object[]) getProperty("pendingActivityValues");
						return Arrays.stream(pendingActivityValues)
								.map(cols -> (Map<String, Object>) cols)
								.collect(FormRow::new, (r, cols) -> {
									String field = cols.get("field").toString();
									String type = cols.get("type").toString();
									String value = cols.get("value").toString();

									r.setId(readPendingActivityId(p.getInstanceId()));
									r.setProperty(getPropertyString("fieldProcessId"), primaryKey);

									switch (type) {
										case "pendingUser":
											r.setProperty(field, readPendingUser(p.getInstanceId()));
											break;
										case "pendingActivity":
											r.setProperty(field, readPendingActivity(p.getInstanceId()));
											break;
										default:
											r.setProperty(field, AppUtil.processHashVariable(value, wfAssignment, null, null));
									}
								}, FormRow::putAll);
					}).ifPresent(row -> rowSet.add(0, row));
		}
        return rowSet;
	}

	private String readPendingUser(String processId) {
		final WorkflowManager wfManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");
		final Collection<WorkflowActivity> activities = wfManager.getActivityList(processId, 0, 1, "dateCreated", true);
		final ExtDirectoryManager directoryManager = (ExtDirectoryManager)AppUtil.getApplicationContext().getBean("directoryManager");
		return activities.stream()
				.filter(activity -> activity.getState().startsWith(SharkConstants.STATEPREFIX_OPEN))
				.flatMap(a -> Arrays.stream(wfManager.getRunningActivityInfo(a.getId()).getAssignmentUsers()))
				.filter(u -> !u.isEmpty())
				.map(u -> {
					User user = directoryManager.getUserByUsername(u);
					if(user == null){
						LogUtil.warn(getClassName(), "Username [" + u + "] is not listed in User Directory");
						return u;
					} else {
						return user.getFirstName() + " " + user.getLastName();
					}
				})
				.filter(u -> !u.isEmpty())
				.collect(Collectors.joining(","));
	}

	private String readPendingActivity(String processId) {
		WorkflowManager wfManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");
		Collection<WorkflowActivity> activities = wfManager.getActivityList(processId, 0, 1, "dateCreated", true);
		return activities.stream()
				.filter(activity -> activity.getState().startsWith(SharkConstants.STATEPREFIX_OPEN))
				.map(WorkflowActivity::getName)
				.collect(Collectors.joining(","));
	}

	private String readPendingActivityId(String processId) {
		WorkflowManager wfManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");
		Collection<WorkflowActivity> activities = wfManager.getActivityList(processId, 0, 1, "dateCreated", true);
		return activities.stream()
				.filter(activity -> activity.getState().startsWith(SharkConstants.STATEPREFIX_OPEN))
				.map(WorkflowActivity::getId)
				.collect(Collectors.joining(","));
	}
}
