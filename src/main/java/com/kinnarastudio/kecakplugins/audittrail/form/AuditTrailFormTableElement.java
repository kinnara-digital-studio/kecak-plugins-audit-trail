package com.kinnarastudio.kecakplugins.audittrail.form;

import com.kinnarastudio.commons.jsonstream.JSONCollectors;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.PackageDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.WorkflowVariable;
import org.joget.workflow.model.service.WorkflowManager;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.function.IntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Yonathan
 */
public class AuditTrailFormTableElement extends Element implements FormBuilderPaletteElement {
    @Override
    public String renderTemplate(FormData formData, Map dataModel) {
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

        String template = "AuditTrailFormElement.ftl";

        dataModel.put("className", getClassName());

        // Data tables datas container
        final List<List<String>> data = new ArrayList<>();
        final List<String> headers = new ArrayList<>();

        // Column id container
        final List<String> columnList = new ArrayList<>();
        final Map<String, String>[] columnProperties = getColumnProperties();

        if (columnProperties != null && columnProperties.length > 0) {
            for (Map<String, String> headerProp : columnProperties) {
                columnList.add(getField(headerProp));
                headers.add(String.valueOf(headerProp.get("columnLabel")));
            }
            dataModel.put("headers", headers);
        }

        final FormRowSet rowSet = formData.getLoadBinderData(this);
        if (rowSet != null) {
            for (FormRow row : rowSet) {
                List<String> contentList = new ArrayList<>();
                for (final String columnName : columnList) {
                    final String value = row.getProperty(columnName);
                    contentList.add(formatColumn(columnName, null, row.getId(), value, appDefinition.getAppId(), appDefinition.getVersion(), ""));
                }
                data.add(contentList);
            }
        }

        final Object[] sortBy = (Object[]) getProperty("sortBy");
        if (sortBy != null && sortBy.length > 0) {
            dataModel.put("sort", translateSoryBy(sortBy));
        }

        dataModel.put("datas", data);
        dataModel.put("error", false);

        String html = FormUtil.generateElementHtml(this, formData, template, dataModel);
        return html;
    }

    private List<Map<String, String>> translateSoryBy(Object[] sortBy) {
        List<Map<String, String>> result = new ArrayList<Map<String, String>>();
        for (Object o : sortBy) {
            result.add((Map<String, String>) o);
        }
        return result;
    }

    @Override
    public String getName() {
        return getLabel() + getVersion();
    }

    @Override
    public String getVersion() {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        ResourceBundle resourceBundle = pluginManager.getPluginMessageBundle(getClassName(), "/messages/BuildNumber");
        return resourceBundle.getString("buildNumber");
    }

    @Override
    public String getDescription() {
        return getClass().getPackage().getImplementationTitle();
    }

    @Override
    public String getLabel() {
        return "Audit Trail Table";
    }

    @Override
    public String getClassName() {
        return this.getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        List<Map<String, String>> monitoringOptions = Arrays.stream(WorkflowProcessMonitoringMultirowFormBinder.Fields.values())
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
                WorkflowProcessMonitoringMultirowFormBinder.class.getName(),
                WorkflowProcessMonitoringMultirowFormBinder.class.getName(),
                WorkflowProcessMonitoringMultirowFormBinder.class.getName(),
                new JSONArray(monitoringOptions).toString().replaceAll("\"", "'")
        };
        return AppUtil.readPluginResource(getClass().getName(), "/properties/AuditTrailFormElement.json", args, true, "/messages/AuditTrailFormElement");
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

    public String formatColumn(String columnName, Map<String, String> properties, String recordId, String value, String appId, Long appVersion, String contextPath) {
        return value;
    }

    public Map<String, String>[] getColumnProperties() {
        Map<String, Object> loadBinder = (Map<String, Object>) getProperty("loadBinder");

        return Optional.of(WorkflowProcessMonitoringMultirowFormBinder.class.getName().equals(loadBinder.get("className")) ? "monitoringColumns" : "columns")
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

    public String getField(Map<String, String> map) {
        return map.get("columnId");
    }

    @Override
    public Object handleElementValueResponse(Element element, FormData formData) {
        final FormRowSet rowSet = formData.getLoadBinderData(element);
        return Optional.ofNullable(rowSet)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(this::collectGridElement)
                .collect(JSONCollectors.toJSONArray());
    }

    protected JSONObject collectGridElement(@Nonnull FormRow row) {
        final AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();
        final Map<String, String>[] columnProperties = getColumnProperties();

        final JSONObject jsonObject = Optional.ofNullable(columnProperties)
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .collect(JSONCollectors.toJSONObject(this::getField, props -> {
                    final String primaryKey = Optional.of(row).map(FormRow::getId).orElse("");
                    final String columnName = Optional.of(props)
                            .map(this::getField)
                            .orElse("");

                    return Optional.of(columnName)
                            .filter(s -> !s.isEmpty())
                            .map(row::getProperty)
                            .map(s -> formatColumn(columnName, props, primaryKey, s, appDefinition.getAppId(), appDefinition.getVersion(), ""))
                            .orElse(null);
                }));

        return jsonObject;
    }
}