package com.kinnara.kecakplugins.audittrail;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.joget.apps.app.dao.AuditTrailDao;
import org.joget.apps.app.dao.FormDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.AuditTrail;
import org.joget.apps.app.model.FormDefinition;
import org.joget.apps.app.model.PackageDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.service.FormService;
import org.joget.apps.form.service.FormUtil;
import org.joget.workflow.model.WorkflowProcess;
import org.joget.workflow.model.WorkflowVariable;
import org.joget.workflow.model.service.WorkflowManager;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nullable;

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

    public static AuditTrailDao getAuditTrailDao() {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        try {
            return (AuditTrailDao) applicationContext.getBean("auditTrailDao");
        }catch (NoSuchBeanDefinitionException e) {
            return (AuditTrailDao) applicationContext.getBean("AuditTrailDao");
        }
    }

    @Nullable
    public static <T> T getArgumentByClassType(AuditTrail auditTrail, Class<T> clazz) {
        for (int i = 0; i < auditTrail.getParamTypes().length; i++) {
            Class type = auditTrail.getParamTypes()[i];
            if(type.getName().equals(clazz.getName())) {
                return (T) auditTrail.getArgs()[i];
            }
        }

        return null;
    }

    /**
     * Get list of workflow variable in value->label map
     * @return
     */
    public static List<Map<String, String>> getWorkflowVariableOptions() {
        final AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();
        final WorkflowManager workflowManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");
        final List<Map<String, String>> workflowVariableOptions = Optional.ofNullable(appDefinition)
                .map(AppDefinition::getPackageDefinition)
                .map(PackageDefinition::getId)
                .map(workflowManager::getProcessList)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(WorkflowProcess::getId)
                .map(workflowManager::getProcessVariableDefinitionList)
                .flatMap(Collection::stream)
                .map(WorkflowVariable::getId)
                .distinct()
                .sorted()
                .map(v -> {
                    Map<String, String> map = new HashMap<>();
                    map.put(FormUtil.PROPERTY_VALUE, v);
                    map.put(FormUtil.PROPERTY_LABEL, v);
                    return map;
                })
                .collect(Collectors.toCollection(ArrayList::new));
        return workflowVariableOptions;
    }
}
