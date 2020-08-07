package com.kinnara.kecakplugins.audittrail;

import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.workflow.model.WorkflowVariable;
import org.joget.workflow.model.service.WorkflowManager;
import org.json.JSONArray;
import org.kecak.apps.form.model.GridElement;

import java.util.*;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 * @author Yonathan
 */
public class AuditTrailFormTableElement extends Element implements FormBuilderPaletteElement, GridElement {
    @Override
    public String renderTemplate(FormData formData, Map dataModel) {
        AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();

    	FormUtil.setReadOnlyProperty(this);
    	
        String template = "AuditTrailFormElement.ftl";
        
        dataModel.put("className", getClassName());
        
        // Data tables datas container
        List<List<String>> datas = new ArrayList<>();
        List<String> headers = new ArrayList<>();
        
        // Column id container
        List<String> columnList = new ArrayList<>();
        Map<String, String>[] columnProperties = getColumnProperties();
        if (columnProperties != null && columnProperties.length > 0) {
            for (Map<String, String> headerProp : columnProperties) {
                columnList.add(getField(headerProp));
                headers.add(String.valueOf(headerProp.get("columnLabel")));
            }
            dataModel.put("headers", headers);
        }
        
        FormRowSet rowSet = formData.getLoadBinderData(this);
        if(rowSet != null) {
	    	for(FormRow row : rowSet) {                		
	    		List<String> contentList = new ArrayList<>();
	            for(int i = 0, size = columnList.size(); i < size;i++){
	                String columnName = columnList.get(i);
	                String value = row.getProperty(columnName);
	                contentList.add(formatColumn(columnName, null, row.getId(), value, appDefinition.getAppId(), appDefinition.getVersion(), ""));
	            }
	            datas.add(contentList);
	    	}
        }
        
        Object[] sortBy = (Object[])getProperty("sortBy");  	
        if(sortBy != null && sortBy.length > 0) {
            dataModel.put("sort", translateSoryBy(sortBy));
        }
        
        dataModel.put("datas", datas);
        dataModel.put("error", false);
        dataModel.put("hidden",
                ("true".equals(getPropertyString("hiddenWhenReadonly"))
                        && FormUtil.isReadonly(this, formData)) ||
                ("true".equals(getPropertyString("hiddenWhenAsSubform"))
                        && FormUtil.findRootForm(this) != null
                        && FormUtil.findRootForm(this).getParent() != null) );
        
        String html = FormUtil.generateElementHtml(this, formData, template, dataModel);
        return html;
    }
    
    private List<Map<String, String>> translateSoryBy(Object[] sortBy) {
    	List<Map<String, String>> result = new ArrayList<Map<String, String>>();
    	for(Object o : sortBy) {
    		result.add((Map<String, String>) o);
    	}
    	return result;
    }

    @Override
    public String getName() {
        return AppPluginUtil.getMessage("auditTrailFormElement.title", getClassName(), "/messages/AuditTrailFormElement");
    }

    @Override
    public String getVersion() {
    	return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public String getDescription() {
        return "Kecak Plugins; Form table element, to get list of approval history for each process; Artifact ID : " + getClass().getPackage().getImplementationTitle();
    }

    @Override
    public String getLabel() {
        return getName();
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        List<Map<String, String>> monitoringOptions = Arrays.stream(AuditTrailMonitoringMultirowFormBinder.Fields.values())
                .collect(ArrayList::new, (list, field) -> {
                    Map<String, String> map = new HashMap<>();
                    map.put("value", field.toString());
                    map.put("label", field.getLabel());
                    list.add(map);
                }, Collection::addAll);

        WorkflowManager workflowManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");
        AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();
        if(workflowManager != null && appDefinition != null && appDefinition.getPackageDefinition() != null) {
            String packageId = appDefinition.getPackageDefinition().getId();
            monitoringOptions.addAll(workflowManager.getProcessList(packageId)
                    .stream()
                    .flatMap(p -> workflowManager.getProcessVariableDefinitionList(p.getId()).stream())
                    .map(WorkflowVariable::getId)
                    .distinct()
                    .sorted()
                    .map(v -> {
                        Map<String, String> map = new HashMap<>();
                        map.put("value", v);
                        map.put("label", "Variable " + v);
                        return map;
                    })
                    .collect(Collectors.toCollection(ArrayList::new)));
        }

        String[] args = {
                AuditTrailMultirowLoadBinder.class.getName(),
                AuditTrailMonitoringMultirowFormBinder.class.getName(),
                AuditTrailMonitoringMultirowFormBinder.class.getName(),
                new JSONArray(monitoringOptions).toString().replaceAll("\"", "'")
        };
        return AppUtil.readPluginResource(getClass().getName(), "/properties/AuditTrailFormElement.json", args , true, "/messages/AuditTrailFormElement");
    }

    public String getFormBuilderCategory() {
        return "Kecak";
    }

    public int getFormBuilderPosition() {
        return 14;
    }

    public String getFormBuilderIcon() {
        return "/plugin/org.joget.apps.form.lib.Grid/images/grid_icon.gif";
    }

    public String getFormBuilderTemplate() {
        return "<table id='audittrail'> "
                + "    <thead> "
                + "        <tr> "
                + "            <th>Column1</th> "
                + "            <th>Column2</th> "
                + "            <th>Column3</th> "
                + "            <th>Column4</th> "
                + "            <th>Column5</th> "
                + "        </tr> "
                + "    </thead> "
                + "</table>";
    }

    @Override
    public String formatColumn(String columnName, Map<String, String> properties, String recordId, String value, String appId, Long appVersion, String contextPath) {
        return value;
    }

    @Override
    public Map<String, String>[] getColumnProperties() {
        Map<String, Object> loadBinder = (Map<String, Object>) getProperty("loadBinder");

        return Optional.of(AuditTrailMonitoringMultirowFormBinder.class.getName().equals(loadBinder.get("className")) ? "monitoringColumns" : "columns")
                .map(this::getProperty)
                .map(o -> (Object[]) o)
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .map(o -> (Map<String, String>)o)
                .map(r -> {
                    Map<String, Object> m = new HashMap<>();
                    r.forEach((k, v) -> m.put(String.valueOf(k), v));
                    return m;
                })
                .toArray((IntFunction<Map<String, String>[]>) Map[]::new);
    }

    @Override
    public String getField(Map<String, String> map) {
        return map.get("columnId");
    }
}