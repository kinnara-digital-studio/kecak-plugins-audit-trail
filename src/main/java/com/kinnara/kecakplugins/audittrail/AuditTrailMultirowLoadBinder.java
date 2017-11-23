package com.kinnara.kecakplugins.audittrail;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.*;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;
import org.springframework.context.ApplicationContext;

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
		return "Audit Trail Multirow Form Load Binder";
	}

	public String getVersion() {
		return getClass().getPackage().getImplementationVersion();
	}

	public String getDescription() {
		return "Artifact ID : " + getClass().getPackage().getImplementationTitle();
	}

	public FormRowSet load(Element element, String primaryKey, FormData formData) {
		ApplicationContext appContext = AppUtil.getApplicationContext();
		WorkflowManager wfManager = (WorkflowManager)appContext.getBean("workflowManager");
		WorkflowAssignment wfAssignment = wfManager.getAssignment(formData.getActivityId());
		FormDataDao formDataDao = (FormDataDao) appContext.getBean("formDataDao");
        Form form = AuditTrailUtil.generateForm(getPropertyString("formDefId"));

        if(form != null) {
        	String sqlFilterCondition = getPropertyString("sqlFilterCondition");

        	FormRowSet rowSet =
					formDataDao.find(form, "WHERE e.customProperties." + getPropertyString("fieldProcessId") + "=?" +
							(sqlFilterCondition != null && !sqlFilterCondition.isEmpty() ?
									(" AND ( " + AppUtil.processHashVariable(sqlFilterCondition, wfAssignment, null, null) + " )") : ""),
        			new Object[] { primaryKey }, null, null, null, null);
        	return rowSet;
        }
		return null;
	}
}
