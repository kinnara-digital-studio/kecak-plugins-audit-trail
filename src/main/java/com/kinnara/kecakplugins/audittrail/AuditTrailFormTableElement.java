package com.kinnara.kecakplugins.audittrail;

import java.util.*;
import java.util.stream.Collectors;

import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormBuilderPaletteElement;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormUtil;
import org.joget.workflow.model.WorkflowVariable;
import org.joget.workflow.model.service.WorkflowManager;
import org.json.JSONArray;

/**
 *
 * @author Yonathan
 */
public class AuditTrailFormTableElement extends Element implements FormBuilderPaletteElement {
    @Override
    public String renderTemplate(FormData formData, Map dataModel) {
    	FormUtil.setReadOnlyProperty(this);
    	
        String template = "AuditTrailFormElement.ftl";
        
        dataModel.put("className", getClassName());
        
        // Data tables datas container
        List<List<String>> datas = new ArrayList<>();
        List<String> headers = new ArrayList<>();
        
        // Column id container
        List<String> columnList;
        columnList = new ArrayList<String>();

        // Set datatables headerProp
        Object[] columns;
        Map<String, Object> loadBinder = (Map<String, Object>) getProperty("loadBinder");
        if(AuditTrailMonitoringMultirowFormBinder.class.getName().equals(loadBinder.get("className"))) {
            columns = (Object[]) getProperty("monitoringColumns");
        } else {
            columns = (Object[]) getProperty("columns");
        }

        if (columns != null && columns.length > 0) {
            Map<String, String> headerProp = null;
            for (Object o : columns) {
                headerProp = (HashMap<String, String>) o;
                columnList.add(headerProp.get("columnId"));
                headers.add(headerProp.get("columnLabel"));
            }
            dataModel.put("headers", headers);
        }
        
        FormRowSet rowSet = formData.getLoadBinderData(this);
        if(rowSet != null) {
	    	for(FormRow row : rowSet) {                		
	    		List<String> contentList = new ArrayList<String>();
	            for(int i = 0, size = columnList.size(); i < size;i++){
	                contentList.add(row.getProperty((String)columnList.get(i)));
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

    public String getName() {
        return AppPluginUtil.getMessage("auditTrailFormElement.title", getClassName(), "/messages/AuditTrailFormElement");
    }

    public String getVersion() {
    	return getClass().getPackage().getImplementationVersion();
    }

    public String getDescription() {
        return "Kecak Plugins; Form table element, to get list of approval history for each process; Artifact ID : " + getClass().getPackage().getImplementationTitle();
    }

    public String getLabel() {
        return getName();
    }

    public String getClassName() {
        return this.getClass().getName();
    }

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
}