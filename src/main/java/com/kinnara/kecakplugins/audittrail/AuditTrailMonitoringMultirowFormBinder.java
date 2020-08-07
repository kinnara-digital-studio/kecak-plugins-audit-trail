package com.kinnara.kecakplugins.audittrail;

import org.enhydra.shark.api.common.SharkConstants;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.directory.model.User;
import org.joget.directory.model.service.ExtDirectoryManager;
import org.joget.plugin.base.Plugin;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.WorkflowActivity;
import org.joget.workflow.model.WorkflowProcess;
import org.joget.workflow.model.WorkflowVariable;
import org.joget.workflow.model.dao.WorkflowProcessLinkDao;
import org.joget.workflow.model.service.WorkflowManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class AuditTrailMonitoringMultirowFormBinder extends FormBinder
        implements FormLoadBinder, FormLoadMultiRowElementBinder {

    public enum Fields {

        ID("id", "ID"),
        PROCESS_ID("processId", "Process ID"),
        PROCESS_NAME("processName", "Process Name"),
        ACTIVITY_ID("activityId", "Activity ID"),
        ACTIVITY_NAME("activityName", "Activity Name"),
        CREATED_TIME("createdTime", "Created Time"),
        FINISH_TIME("finishTime", "Finish Time"),
        USERNAME("username", "Username"),
        USER_FULLNAME("userFullname", "User Full Name"),
        PARTICIPANT("participantId", "Participant");

        private String name;
        private String label;

        Fields(String name, String label) {
            this.name = name;
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public String toString() {
            return name;
        }
    }


    public final static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private Date keepCreatedDate = null;

    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        final Map<String, Date> startTime = new HashMap<>();

        // do not load data for empty primary key
        if(primaryKey == null || primaryKey.isEmpty()) {
            return null;
        }

        ApplicationContext appContext = AppUtil.getApplicationContext();
        WorkflowProcessLinkDao workflowProcessLinkDao = (WorkflowProcessLinkDao) appContext.getBean("workflowProcessLinkDao");
        WorkflowManager workflowManager = (WorkflowManager)appContext.getBean("workflowManager");

        FormRowSet rowSet = workflowProcessLinkDao.getLinks(primaryKey).stream()
                .filter(Objects::nonNull)
                .flatMap(l -> workflowManager.getActivityList(l.getProcessId(), null, 1000, null, null).stream())

                // only handle activity
                .filter(activity -> {
                    final WorkflowActivity definition = workflowManager.getProcessActivityDefinition(activity.getProcessDefId(), activity.getActivityDefId());
                    return
                            (
                                    WorkflowActivity.TYPE_NORMAL.equals(definition.getType()) && (
                                            getPropertyString("excludeActivities") == null
                                                    || Arrays.stream(getPropertyString("excludeActivities").split(";"))
                                                    .filter(s -> !s.isEmpty())
                                                    .noneMatch(id -> id.equals(definition.getId()))
                                    )
                            ) || (
                                    WorkflowActivity.TYPE_TOOL.equals(definition.getType()) && (
                                            ("true".equalsIgnoreCase(getPropertyString("toolAsStartProcess")) && definition.getId().equals(getPropertyString("startProcessTool"))) ||
                                                    (getPropertyString("alsoDisplayTools") != null && Arrays.stream(getPropertyString("alsoDisplayTools").split(";"))
                                                            .filter(s -> !s.isEmpty())
                                                            .anyMatch(id -> id.equals(definition.getId())))
                                    )
                            );
                })

                // only show unaborted activity
//                .filter(activity -> !SharkConstants.STATE_CLOSED_ABORTED.equals(activity.getState()))

                .sorted(Comparator.comparing(WorkflowActivity::getCreatedTime))

                // if property showPendingValue is checked, then display open assignment
                .filter(activity -> "true".equalsIgnoreCase(getPropertyString("showPendingValue"))
                        || !activity.getState().startsWith(SharkConstants.STATEPREFIX_OPEN))

                .collect(() -> {
                    // handle first record, get from process'
                    FormRowSet formRowSet = new FormRowSet();

                    if (!"true".equalsIgnoreCase(getPropertyString("toolAsStartProcess"))) {
                        WorkflowProcess process = workflowManager.getRunningProcessById(primaryKey);
                        WorkflowProcess info = workflowManager.getRunningProcessInfo(primaryKey);
                        FormRow row = new FormRow();
                        row.setProperty(Fields.ID.toString(), process == null || process.getId() == null ? "" : process.getId());
                        row.setProperty(Fields.PROCESS_ID.toString(), process == null || process.getId() == null ? "" : process.getId());
                        row.setProperty(Fields.PROCESS_NAME.toString(), process == null || process.getName() == null ? "" : process.getName());
                        row.setProperty(Fields.ACTIVITY_ID.toString(), "startProcess");
                        row.setProperty(Fields.ACTIVITY_NAME.toString(), "Start Process");
                        row.setProperty(Fields.CREATED_TIME.toString(), info == null || info.getStartedTime() == null ? "" : dateFormat.format(info.getStartedTime()));
                        row.setProperty(Fields.FINISH_TIME.toString(), info == null || info.getStartedTime() == null ? "" : dateFormat.format(info.getStartedTime())); // for start process this should be the same
                        row.setProperty(Fields.USERNAME.toString(), process == null || process.getRequesterId() == null ? "" : process.getRequesterId());
                        row.setProperty(Fields.USER_FULLNAME.toString(), process == null || process.getRequesterId() == null ? "" : mapUsernameToFullUsername(process.getRequesterId()));

                        // get first process

                        formRowSet.add(row);
                    }
                    return formRowSet;
                }, (formRows, activity) -> {
                    WorkflowActivity info = workflowManager.getRunningActivityInfo(activity.getId());
                    WorkflowActivity definition = workflowManager.getProcessActivityDefinition(activity.getProcessDefId(), activity.getActivityDefId());
                    FormRow row = new FormRow();

                    if (SharkConstants.STATE_CLOSED_ABORTED.equals(activity.getState())) {
                        // keep aborted activity first data
                        if (keepCreatedDate == null)
                            keepCreatedDate = info.getCreatedTime();
                        return;
                    }

                    row.setProperty(Fields.ID.toString(), activity.getId());
                    row.setProperty(Fields.PROCESS_ID.toString(), activity.getProcessDefId());
                    row.setProperty(Fields.PROCESS_NAME.toString(), activity.getProcessName());
                    row.setProperty(Fields.ACTIVITY_ID.toString(), activity.getActivityDefId());
                    row.setProperty(Fields.ACTIVITY_NAME.toString(), activity.getName());

                    row.setProperty(Fields.CREATED_TIME.toString(), dateFormat.format(keepCreatedDate != null ? keepCreatedDate : info.getCreatedTime()));
                    keepCreatedDate = null;

                    if (info.getFinishTime() != null)
                        row.setProperty(Fields.FINISH_TIME.toString(), dateFormat.format(info.getFinishTime()));

                    row.setProperty(Fields.PARTICIPANT.toString(), info.getPerformer());

                    if ("true".equalsIgnoreCase(getPropertyString("toolAsStartProcess")) && WorkflowActivity.TYPE_TOOL.equalsIgnoreCase(definition.getType())) {
                        WorkflowProcess process = workflowManager.getRunningProcessById(primaryKey);
                        row.setProperty(Fields.USERNAME.toString(), process.getRequesterId());
                        row.setProperty(Fields.USER_FULLNAME.toString(), mapUsernameToFullUsername(process.getRequesterId()));
                    } else {
                        row.setProperty(Fields.USERNAME.toString(), info.getNameOfAcceptedUser() != null ? info.getNameOfAcceptedUser() : String.join(",", info.getAssignmentUsers()));
                        row.setProperty(Fields.USER_FULLNAME.toString(), Arrays.stream(info.getNameOfAcceptedUser() != null ? new String[]{info.getNameOfAcceptedUser()} : info.getAssignmentUsers())
                                .filter(u -> !u.isEmpty())
                                .map(this::mapUsernameToFullUsername)
                                .filter(u -> !u.isEmpty())
                                .collect(Collectors.joining(","))
                        );
                    }

                    Map<String, String> mapPendingValues = new HashMap<>();
                    Object[] pendingValues = ((Object[]) getProperty("pendingValues"));

                    // no need to show variables value of current assignment
                    boolean isCurrentAssignment = activity.getState().startsWith(SharkConstants.STATEPREFIX_OPEN);
                    if (!isCurrentAssignment) {
                        row.putAll(workflowManager.getActivityVariableList(activity.getId()).stream()
                                .collect(
                                        FormRow::new,
                                        (formRow, workflowVariable) -> {
                                            String workflowVariableName = workflowVariable.getName();
                                            formRow.setProperty(workflowVariableName, mapPendingValues.containsKey(workflowVariableName) ? mapPendingValues.get(workflowVariableName) : String.valueOf(workflowVariable.getVal()));
                                        },
                                        FormRow::putAll));
                    } else if (pendingValues != null) {
                        mapPendingValues.putAll(Arrays.stream(pendingValues)
                                .map(rows -> (Map<String, Object>) rows)
                                .collect(
                                        HashMap::new,
                                        (hashMap, rows) -> hashMap.put(String.valueOf(rows.get("columnId")),
                                                String.valueOf(AppUtil.processHashVariable(String.valueOf(rows.get("columnValue")), null, null, null))),
                                        Map::putAll));

                        row.putAll(workflowManager.getActivityVariableList(activity.getId()).stream()
                                .collect(
                                        FormRow::new,
                                        (formRow, workflowVariable) -> {
                                            String workflowVariableName = workflowVariable.getName();
                                            formRow.setProperty(workflowVariableName, mapPendingValues.containsKey(workflowVariableName) ? mapPendingValues.get(workflowVariableName) : "");
                                        },
                                        FormRow::putAll));
                    }
                    formRows.add(0, row);
                }, FormRowSet::addAll);

        rowSet.setMultiRow(true);

        return rowSet;
    }

    @Override
    public String getName() {
        return AppPluginUtil.getMessage("monitoringMultirowFormBinder.title", getClassName(), "/messages/AuditTrailMonitoringMultirowFormBinder");
    }

    @Override
    public String getVersion() {
        return getClass().getPackage().getImplementationVersion();
    }

    @Override
    public String getDescription() {
        return getClass().getPackage().getImplementationTitle();
    }

    @Override
    public String getLabel() {
        return getName();
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        List<Map<String, String>> monitoringOptions = new ArrayList<>();

        WorkflowManager workflowManager = (WorkflowManager) AppUtil.getApplicationContext().getBean("workflowManager");
        AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();
        if(workflowManager != null && appDefinition != null && appDefinition.getPackageDefinition() != null) {
            String packageId = appDefinition.getPackageDefinition().getId();
            monitoringOptions.addAll(workflowManager.getProcessList(packageId)
                    .stream()
                    .map(WorkflowProcess::getId)
                    .map(workflowManager::getProcessVariableDefinitionList)
                    .flatMap(Collection::stream)
                    .map(WorkflowVariable::getId)
                    .distinct()
                    .sorted()
                    .map(v -> {
                        Map<String, String> map = new HashMap<>();
                        map.put("value", v);
                        map.put("label", v);
                        return map;
                    })
                    .collect(Collectors.toCollection(ArrayList::new)));
        }

        JSONObject startProcessToolProperty = new JSONObject();
        try {
            startProcessToolProperty.put("name", "startProcessTool");
            startProcessToolProperty.put("label","@@monitoringMultirowFormBinder.startProcessTool@@");
            startProcessToolProperty.put("control_field", "toolAsStartProcess");
            startProcessToolProperty.put("control_value", "true");
            startProcessToolProperty.put("required", "true");

            if(appDefinition != null && isClassInstalled("com.kinnara.kecakplugins.workflowcomponentoptionsbinder.ActivityOptionsBinder")) {
                String appId = appDefinition.getAppId();
                String appVersion = appDefinition.getVersion().toString();

                startProcessToolProperty.put("type","selectbox");
                startProcessToolProperty.put("options_ajax","[CONTEXT_PATH]/web/json/app[APP_PATH]/plugin/com.kinnara.kecakplugins.workflowcomponentoptionsbinder.ActivityOptionsBinder/service?appId="+appId + "&appVersion=" + appVersion + "&type=" + WorkflowActivity.TYPE_TOOL);
            } else {
                startProcessToolProperty.put("type","textfield");
            }
        } catch (JSONException ignored) { }

        JSONObject alsoDislpayToolProperty = new JSONObject();
        try {
            alsoDislpayToolProperty.put("name", "alsoDisplayTools");
            alsoDislpayToolProperty.put("label", "@@monitoringMultirowFormBinder.alsoDisplayTools@@");
            if(appDefinition != null && isClassInstalled("com.kinnara.kecakplugins.workflowcomponentoptionsbinder.ActivityOptionsBinder")) {
                String appId = appDefinition.getAppId();
                String appVersion = appDefinition.getVersion().toString();
                alsoDislpayToolProperty.put("type", "multiselect");
                alsoDislpayToolProperty.put("options_ajax","[CONTEXT_PATH]/web/json/app[APP_PATH]/plugin/com.kinnara.kecakplugins.workflowcomponentoptionsbinder.ActivityOptionsBinder/service?appId="+appId + "&appVersion=" + appVersion + "&type=" + WorkflowActivity.TYPE_TOOL);
            } else {
                alsoDislpayToolProperty.put("type", "textfield");
                alsoDislpayToolProperty.put("description", "@@monitoringMultirowFormBinder.alsoDisplayTools.desc@@");
            }

        } catch (JSONException ignored) { }

        JSONObject excludeActivityProperty = new JSONObject();
        try {
            excludeActivityProperty.put("name", "excludeActivities");
            excludeActivityProperty.put("label", "@@monitoringMultirowFormBinder.excludeActivities@@");
            if(appDefinition != null && isClassInstalled("com.kinnara.kecakplugins.workflowcomponentoptionsbinder.ActivityOptionsBinder")) {
                String appId = appDefinition.getAppId();
                String appVersion = appDefinition.getVersion().toString();
                excludeActivityProperty.put("type", "multiselect");
                excludeActivityProperty.put("options_ajax","[CONTEXT_PATH]/web/json/app[APP_PATH]/plugin/com.kinnara.kecakplugins.workflowcomponentoptionsbinder.ActivityOptionsBinder/service?appId="+appId + "&appVersion=" + appVersion + "&type=" + WorkflowActivity.TYPE_NORMAL);
            } else {
                excludeActivityProperty.put("type", "textfield");
                excludeActivityProperty.put("description", "@@monitoringMultirowFormBinder.excludeActivities.desc@@");
            }
        } catch (JSONException ignored) { }

        String[] args = {
                startProcessToolProperty.toString().replaceAll("\"", "'"),
                new JSONArray(monitoringOptions).toString().replaceAll("\"", "'"),
                alsoDislpayToolProperty.toString().replaceAll("\"", "'"),
                excludeActivityProperty.toString().replaceAll("\"", "'")
        };
        return AppUtil.readPluginResource(getClassName(), "/properties/AuditTrailMonitoringMultirowLoadBinder.json", args, false, "/messages/AuditTrailMonitoringMultirowFormBinder");
    }

    private boolean isClassInstalled(String className) {
        PluginManager pluginManager = (PluginManager) AppUtil.getApplicationContext().getBean("pluginManager");
        Plugin plugin = pluginManager.getPlugin(className);
        return plugin != null;
    }

    private String mapUsernameToFullUsername(String u)  {
        ExtDirectoryManager directoryManager = (ExtDirectoryManager)AppUtil.getApplicationContext().getBean("directoryManager");
        User user = directoryManager.getUserByUsername(u);
        if(user == null){
            return u;
        } else {
            return user.getFirstName() + (user.getLastName() != null ? " " + user.getLastName() : "");
        }
    }
}
