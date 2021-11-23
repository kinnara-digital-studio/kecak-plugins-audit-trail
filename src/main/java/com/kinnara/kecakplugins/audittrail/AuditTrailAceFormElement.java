package com.kinnara.kecakplugins.audittrail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.PackageDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.workflow.model.WorkflowVariable;
import org.joget.workflow.model.service.WorkflowManager;
import org.json.JSONArray;

import com.kinnara.kecakplugins.audittrail.model.AuditTrailModel;

public class AuditTrailAceFormElement extends Element implements FormBuilderPaletteElement{

	@Override
	public String getFormBuilderTemplate() {
		return "<label class='label'>Auditrail ACE Treeview</label>";
	}

	@Override
	public String getLabel() {
		return this.getName();
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
        if (workflowManager != null && appDefinition != null && appDefinition.getPackageDefinition() != null) {
            PackageDefinition packageDefinition = appDefinition.getPackageDefinition();
            String packageId = packageDefinition.getId();
            long packageVersion = packageDefinition.getVersion();
            monitoringOptions.addAll(workflowManager.getProcessList(packageId, String.valueOf(packageVersion))
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
        return AppUtil.readPluginResource(getClass().getName(), "/properties/AuditTrailAceFormElement.json", args, true, "/messages/AuditTrailAceFormElement");
	}

	@Override
	public String getName() {
		return "Auditrail ACE";
	}

	@Override
	public String getVersion() {
		return getClass().getPackage().getImplementationVersion();
	}

	@Override
	public String getDescription() {
		return "Kecak Plugins; Artifact ID : " + getClass().getPackage().getImplementationTitle() + "; Treeview Auditrail in ACE Theme";
	}

	@Override
	public String getFormBuilderCategory() {
		return "Kecak Enterprise";
	}

	@Override
	public int getFormBuilderPosition() {
		return 100;
	}

	@Override
	public String getFormBuilderIcon() {
		return "/plugin/org.joget.apps.form.lib.TextField/images/textField_icon.gif";
	}

	private String renderTemplate(String template,FormData formData, Map dataModel) {
		FormUtil.setReadOnlyProperty(this);
		
		boolean isHidden = ("true".equals(getPropertyString("hiddenWhenReadonly"))
                && FormUtil.isReadonly(this.getParent(), formData)) ||
                ("true".equals(getPropertyString("hiddenWhenAsSubform"))
                        && FormUtil.findRootForm(this) != null
                        && FormUtil.findRootForm(this).getParent() != null);
        dataModel.put("hidden", isHidden);

        if(isHidden) {
            return "";
        }

        AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();

        dataModel.put("className", getClassName());

        // Data tables datas container
        List<AuditTrailModel> datas = new ArrayList<>();
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
        if (rowSet != null) {
            for (FormRow row : rowSet) {
            	AuditTrailModel audit = new AuditTrailModel();
                List<String> contentList = new ArrayList<>();
                for (int i = 0, size = columnList.size(); i < size; i++) {
                    String columnName = columnList.get(i);
                    String value = row.getProperty(columnName);
                    
                    if(columnName.equals("_finishTime")) {
                    	audit.setDate(formatColumn(columnName, null, row.getId(), value, appDefinition.getAppId(), appDefinition.getVersion(), ""));
                    }else if(columnName.equals("status")) {
                    	audit.setStatus(formatColumn(columnName, null, row.getId(), value, appDefinition.getAppId(), appDefinition.getVersion(), ""));
                    }else if(columnName.equals("_userFullname")) {
                    	audit.setPerformer(formatColumn(columnName, null, row.getId(), value, appDefinition.getAppId(), appDefinition.getVersion(), ""));
                    }
                    
                }
                datas.add(audit);
            }
        }

        Object[] sortBy = (Object[]) getProperty("sortBy");
        if (sortBy != null && sortBy.length > 0) {
            dataModel.put("sort", translateSoryBy(sortBy));
        }

        dataModel.put("datas", datas);
        dataModel.put("error", false);

        String html = FormUtil.generateElementHtml(this, formData, template, dataModel);
        return html;
	}
	
	public String formatColumn(String columnName, Map<String, String> properties, String recordId, String value, String appId, Long appVersion, String contextPath) {
        return value;
    }
	
	public String getField(Map<String, String> map) {
        return map.get("columnId");
    }
	
	public Map<String, String>[] getColumnProperties() {
        Map<String, Object> loadBinder = (Map<String, Object>) getProperty("loadBinder");

        return Optional.of(AuditTrailMonitoringMultirowFormBinder.class.getName().equals(loadBinder.get("className")) ? "monitoringColumns" : "columns")
                .map(this::getProperty)
                .map(o -> (Object[]) o)
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .map(o -> (Map<String, String>) o)
                .map(r -> {
                    Map<String, Object> m = new HashMap<>();
                    r.forEach((k, v) -> m.put(String.valueOf(k), v));
                    return m;
                })
                .toArray((IntFunction<Map<String, String>[]>) Map[]::new);
    }
	
	private List<Map<String, String>> translateSoryBy(Object[] sortBy) {
        List<Map<String, String>> result = new ArrayList<Map<String, String>>();
        for (Object o : sortBy) {
            result.add((Map<String, String>) o);
        }
        return result;
    }
	
	@Override
    public String renderAceTemplate(FormData formData, Map dataModel) {
        String template = "AuditTrailAceFormElement.ftl";
        return renderTemplate(template,formData,dataModel);
    }

    @Override
    public String renderAdminLteTemplate(FormData formData, Map dataModel){
        String template = "AuditTrailAceFormElement.ftl";
        return renderTemplate(template,formData,dataModel);
    }

	@Override
	public String renderAdminKitTemplate(FormData formData, Map dataModel) {
		String template = "AuditTrailAceFormElement.ftl";
        return renderTemplate(template,formData,dataModel);
	}

	@Override
	public String renderTemplate(FormData formData, Map dataModel) {
		String template = "AuditTrailAceFormElement.ftl";
        return renderTemplate(template,formData,dataModel);
	}

}
