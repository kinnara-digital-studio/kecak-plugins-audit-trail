package com.kinnara.kecakplugins.audittrail;

import java.util.HashMap;
import java.util.Map;

import org.joget.apps.app.dao.FormDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.FormDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.service.FormService;

public class AuditTrailUtil {
	private static Map<String, Form> formCache = new HashMap<String, Form>();
	
	public static Form generateForm(String formDefId) {
		return generateForm(formDefId, formCache);
	}
	
	public static Form generateForm(String formDefId, Map<String, Form> formCache) {
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
