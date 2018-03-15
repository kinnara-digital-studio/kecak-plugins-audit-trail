package com.kinnara.kecakplugins.audittrail;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

import org.joget.apps.app.model.AuditTrail;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.service.FormService;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultAuditTrailPlugin;
import org.joget.plugin.base.PluginManager;

public class AuditTrailFormCollector extends DefaultAuditTrailPlugin{

	public String getLabel() {
		return getName();
	}

	public String getClassName() {
		return getClass().getName();
	}

	public String getPropertyOptions() {
		return AppUtil.readPluginResource(getClassName(), "/properties/AuditTrailFormCollector.json", null, false, "/messages/AuditTrailFormCollector");
	}

	public String getName() {
		return AppPluginUtil.getMessage("formFieldAuditTrail.title", getClassName(), "/messages/AuditTrailFormCollector");
	}

	public String getVersion() {
		return getClass().getPackage().getImplementationVersion();
	}

	public String getDescription() {
		return "Kecak Plugins; Artifact ID : " + getClass().getPackage().getImplementationTitle();
	}

	@Override
	public Object execute(Map props) {
		final AuditTrail auditTrail = (AuditTrail)props.get("auditTrail");
		PluginManager pluginManager = (PluginManager) props.get("pluginManager");
		final Map<String, String[]> requestParams = pluginManager.getHttpServletRequest().getParameterMap();
		
		Form auditForm = AuditTrailUtil.generateForm(getPropertyString("formDefId"));
		
		if(validation(auditTrail) && auditForm != null) {
			FormService formService = (FormService) AppUtil.getApplicationContext().getBean("formService");
			final FormData auditFormData = new FormData();
			
			auditFormData.setPrimaryKeyValue(null); // use UUID
			formService.executeFormLoadBinders(auditForm, auditFormData);
			
			auditFormData.addRequestParameterValues(getPropertyString("foreignKeyField"), requestParams.get("_FORM_META_ORIGINAL_ID"));
			
			getLeavesChildren(auditForm, leaf -> {
					String leafId = leaf.getPropertyString(FormUtil.PROPERTY_ID);
					if(requestParams.containsKey(leafId))
						auditFormData.addRequestParameterValues(leafId, requestParams.get(leafId));
			});
			
			formService.executeFormStoreBinders(auditForm, auditFormData);
			
		} else if(auditForm == null){
			LogUtil.warn(getClassName(), "Form [" + getPropertyString("formDefId") + "] cannot be generated");
		}
		
		return null;
	}
	
	public boolean validation(AuditTrail auditTrail) {
        return auditTrail.getMethod().equals("assignmentComplete")
                || auditTrail.getMethod().equals("assignmentForceComplete")
                || auditTrail.getMethod().equals("assignmentReassign");
    }

	private void getLeavesChildren(Element element, Consumer<Element> listener) {
		Collection<Element> children = element.getChildren(); 
		if(children == null  || children.isEmpty()) {
			listener.accept(element);
		} else {
			for(Element child : children) {
				getLeavesChildren(child, listener);
			}
		}
	}
}
