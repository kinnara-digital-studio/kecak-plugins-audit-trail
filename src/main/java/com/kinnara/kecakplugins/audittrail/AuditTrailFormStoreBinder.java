package com.kinnara.kecakplugins.audittrail;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.joget.apps.app.dao.FormDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.FormDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.lib.WorkflowFormBinder;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormService;
import org.joget.apps.form.service.FormUtil;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;

/**
 * 
 * @author aristo
 *
 */
public class AuditTrailFormStoreBinder extends WorkflowFormBinder{
	private Map<String, Form> formCache = new HashMap<String, Form>();
	
	@Override
	public String getClassName() {
		return getClass().getName();
	}
	
	@Override
	public String getName() {
		return "Kecak - Audit Trail Form Store Binder";
	}
	
	@Override
	public String getLabel() {
		return getName();
	}
	
	@Override
	public String getPropertyOptions() {
		return AppUtil.readPluginResource(getClassName(), "/properties/AuditTrailFormStoreBinder.json", null, false, "/messages/AuditTrailFormStoreBinder");
	}
	
	@Override
	public FormRowSet store(Element element, FormRowSet rows, FormData formData) {		
		String processId = formData.getProcessId();
		 
		WorkflowManager wfManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");
		WorkflowAssignment wfAssignment = (WorkflowAssignment) wfManager.getAssignment(formData.getActivityId());
		Form auditForm = generateForm(getPropertyString("formDefId"));
		if(auditForm != null && rows != null && rows.size() > 0 && processId != null) {
			FormService formService = (FormService) AppUtil.getApplicationContext().getBean("formService");
			
			String primaryKeyValue = wfAssignment != null ? wfAssignment.getActivityId() : processId;
			
			final FormRow formRow = rows.get(0);
			final FormData auditFormData = new FormData();
			
			auditFormData.setPrimaryKeyValue(primaryKeyValue);
			formService.executeFormLoadBinders(auditForm, auditFormData);
			
			auditFormData.addRequestParameterValues(getPropertyString("fieldProcessId"), new String[] {processId});
			
			getLeavesChildren(auditForm, new OnLeafChild() {	
				public void onLeafChild(Element leaf) {
					String leafId = leaf.getPropertyString(FormUtil.PROPERTY_ID);
					String value = formRow.getProperty(leafId);
					if(value != null && !value.isEmpty())
						auditFormData.addRequestParameterValues(leafId, new String[] {value});
				}
			});
			
			formService.executeFormStoreBinders(auditForm, auditFormData);
		}
		
		return super.store(element, rows, formData);
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
	
	protected Form generateForm(String formDefId) {
		return generateForm(formDefId, formCache);
	}
	
	protected Form generateForm(String formDefId, Map<String, Form> formCache) {
    	// check in cache
    	if(formCache != null && formCache.containsKey(formDefId))
    		return formCache.get(formDefId);
    	
    	// proceed without cache    	
    	FormService formService = (FormService)AppUtil.getApplicationContext().getBean("formService");
        Form form = null;
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        if (appDef != null && formDefId != null && !formDefId.isEmpty()) {
            FormDefinitionDao formDefinitionDao = (FormDefinitionDao)AppUtil.getApplicationContext().getBean("formDefinitionDao");
            FormDefinition formDef = formDefinitionDao.loadById(formDefId, appDef);
            if (formDef != null) {
                String json = formDef.getJson();
                form = (Form)formService.createElementFromJson(json);
                
                // put in cache if possible
                if(formCache != null)
                	formCache.put(formDefId, form);
                
                return form;
            }
        }
        return null;
	}
}
