package com.kinnara.kecakplugins.audittrail;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.WorkflowFormBinder;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormService;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;
import org.springframework.context.ApplicationContext;

import java.util.Collection;

/**
 * 
 * @author aristo
 * This Form Binder normally be implemented on Sections which contents need to be
 * tracked
 */
public class AuditTrailFormBinder extends WorkflowFormBinder{	
	@Override
	public String getClassName() {
		return getClass().getName();
	}
	
	@Override
	public String getVersion() {
		return getClass().getPackage().getImplementationVersion();
	}
	
	@Override
	public String getName() {
		return "Audit Trail Form Binder";
	}
	
	@Override
	public String getLabel() {
		return getName();
	}
	
	@Override
	public String getPropertyOptions() {
		return AppUtil.readPluginResource(getClassName(), "/properties/AuditTrailFormBinder.json", null, false, "/messages/AuditTrailFormBinder");
	}
	
	/**
	 * Override load method so during loading the field will be empty
	 */
	@Override
	public FormRowSet load(Element element, String primaryKey, FormData formData) {
		FormRowSet originalRowSet = super.load(element, primaryKey, formData);		
		if(originalRowSet != null && !originalRowSet.isEmpty()) {
			final FormRow row = originalRowSet.get(0);
			final String foreignKeyField = getPropertyString("foreignKeyField");
			
			Form auditForm = AuditTrailUtil.generateForm(getPropertyString("formDefId"));
			getLeavesChildren(auditForm, element1 -> {
                String id = element1.getPropertyString(FormUtil.PROPERTY_ID);

                if(row != null && row.containsKey(id) && !id.equals(foreignKeyField))
                    row.remove(id);
            });
		}
		
		return originalRowSet;
	}
	
	
	/**
	 * Override store method so during storing, the audited data will be kept
	 * in the configured formDefId
	 */
	@Override
	public FormRowSet store(Element element, FormRowSet rows, FormData formData) {
		ApplicationContext appContext = AppUtil.getApplicationContext();
		WorkflowManager wfManager = (WorkflowManager)appContext.getBean("workflowManager");
		WorkflowAssignment wfAssignment = wfManager.getAssignment(formData.getActivityId());
		Form auditForm = AuditTrailUtil.generateForm(getPropertyString("formDefId"));

		if(auditForm != null && rows != null && rows.size() > 0) {
			FormService formService = (FormService) AppUtil.getApplicationContext().getBean("formService");			
			final FormRow formRow = rows.get(0);
			final FormData auditFormData = new FormData();

			LogUtil.info(getClassName(), "formData activityId [" +formData.getActivityId()+"]");
//			LogUtil.info(getClassName(), "wfAssignment activityId [" + wfAssignment == null ? null : wfAssignment.getActivityId()+"]");
			auditFormData.setPrimaryKeyValue(wfAssignment != null && wfAssignment.getActivityId() != null ? wfAssignment.getActivityId() : formData.getPrimaryKeyValue());
			
			auditFormData.addRequestParameterValues(getPropertyString("foreignKeyField"), new String[] {formData.getPrimaryKeyValue()});
			
			getLeavesChildren(auditForm, leaf -> {
                String leafId = leaf.getPropertyString(FormUtil.PROPERTY_ID);
                String value = formRow.getProperty(leafId);
                if(value != null && !value.isEmpty())
                    auditFormData.addRequestParameterValues(leafId, new String[] {value});
            });
			
			formService.executeFormStoreBinders(auditForm, auditFormData);
		} else if(auditForm == null){
			LogUtil.warn(getClassName(), "Form [" + getPropertyString("formDefId") + "] cannot be generated");
		}
		
		if(element.getProperties().containsKey(FormBinder.FORM_STORE_BINDER))
			element.getProperties().remove(FormBinder.FORM_STORE_BINDER);
		
		return super.store(element, rows, formData);
	}
	
	@Override
	public String getDescription() {
		return "Kecak Plugins; Artifact ID : " + getClass().getPackage().getImplementationTitle() + "; Load or Store form data to audit trail table";
	}
	
	private void getLeavesChildren(Element element, OnLeafChild listener) {
		Collection<Element> children = element.getChildren(); 
		if(children == null  || children.isEmpty()) {
			listener.onLeafChild(element);
		} else {
			for(Element child : children) {
				getLeavesChildren(child, listener);
			}
		}
	}
	
	private interface OnLeafChild {
		void onLeafChild(Element element);
	}
}
