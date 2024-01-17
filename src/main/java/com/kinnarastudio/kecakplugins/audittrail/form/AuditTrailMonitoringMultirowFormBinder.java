package com.kinnarastudio.kecakplugins.audittrail.form;

import com.kinnarastudio.kecakplugins.audittrail.AuditTrailUtil;
import com.kinnarastudio.kecakplugins.audittrail.generators.ActivityOptionsGenerator;
import com.kinnarastudio.kecakplugins.audittrail.generators.OptionsGenerator;
import com.kinnarastudio.commons.Try;
import org.enhydra.shark.api.common.SharkConstants;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.directory.model.User;
import org.joget.directory.model.service.ExtDirectoryManager;
import org.joget.plugin.base.PluginManager;
import org.joget.plugin.base.PluginWebSupport;
import org.joget.workflow.model.WorkflowActivity;
import org.joget.workflow.model.WorkflowProcess;
import org.joget.workflow.model.dao.WorkflowProcessLinkDao;
import org.joget.workflow.model.service.WorkflowManager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AuditTrailMonitoringMultirowFormBinder extends FormBinder
        implements FormLoadBinder, FormLoadMultiRowElementBinder, PluginWebSupport {

    public enum Fields {

        ID("_id", "ID"),
        PROCESS_ID("_processId", "Process ID"),
        PROCESS_NAME("_processName", "Process Name"),
        ACTIVITY_ID("_activityId", "Activity ID"),
        ACTIVITY_NAME("_activityName", "Activity Name"),
        CREATED_TIME("_createdTime", "Created Time"),
        FINISH_TIME("_finishTime", "Finish Time"),
        USERNAME("_username", "Username"),
        USER_FULLNAME("_userFullname", "User Full Name"),

        USER_FIRST_NAME("_userFirstName", "User First Name"),
        PARTICIPANT("_participantId", "Participant");

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


    public final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    private Date keepCreatedDate = null;

    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {

        // do not load data for empty primary key
        if (primaryKey == null || primaryKey.isEmpty()) {
            return null;
        }

        final ApplicationContext appContext = AppUtil.getApplicationContext();
        final WorkflowProcessLinkDao workflowProcessLinkDao = (WorkflowProcessLinkDao) appContext.getBean("workflowProcessLinkDao");
        final WorkflowManager workflowManager = (WorkflowManager) appContext.getBean("workflowManager");

        final FormRowSet rowSet = Optional.of(primaryKey)
                .map(workflowProcessLinkDao::getLinks)
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .filter(Objects::nonNull)
                .map(l -> workflowManager.getActivityList(l.getProcessId(), null, 1000, null, null))
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)

                // only handle activity
                .filter(activity -> {
                    final WorkflowActivity definition = workflowManager.getProcessActivityDefinition(activity.getProcessDefId(), activity.getActivityDefId());
                    if (definition == null)
                        return false;

                    return (isActivity(definition) && !isInExcludedActivity(definition))
                            || (isTool(definition) && (isToolAsStartProcess() && isStartProcessTool(definition) || isInAlsoDisplayTools(definition)));
                })

                // only show unaborted activity
//                .filter(activity -> !SharkConstants.STATE_CLOSED_ABORTED.equals(activity.getState()))

                .sorted(Comparator.comparing(WorkflowActivity::getCreatedTime))

                // if property showPendingValue is checked, then display open assignment
                .filter(activity -> "true".equalsIgnoreCase(getPropertyString("showPendingValue"))
                        || Optional.of(activity).map(WorkflowActivity::getState).map(s -> !s.startsWith(SharkConstants.STATEPREFIX_OPEN)).orElse(false))

                .collect(() -> {
                    // handle first record, get from process
                    final FormRowSet formRowSet = new FormRowSet();

                    if (!isToolAsStartProcess()) {
                        final WorkflowProcess process = workflowManager.getRunningProcessById(primaryKey);
                        final WorkflowProcess info = Optional.ofNullable(process)
                                .map(WorkflowProcess::getInstanceId)
                                .map(workflowManager::getRunningProcessInfo)
                                .orElse(null);

                        final FormRow row = new FormRow();
                        row.setId(process == null || process.getId() == null ? "" : process.getId());
                        row.setProperty(Fields.ID.toString(), row.getId());
                        row.setProperty(Fields.PROCESS_ID.toString(), process == null || process.getId() == null ? "" : process.getId());
                        row.setProperty(Fields.PROCESS_NAME.toString(), process == null || process.getName() == null ? "" : process.getName());
                        row.setProperty(Fields.ACTIVITY_ID.toString(), "startProcess");
                        row.setProperty(Fields.ACTIVITY_NAME.toString(), "Start Process");
                        row.setProperty(Fields.CREATED_TIME.toString(), info == null || info.getStartedTime() == null ? "" : dateFormat.format(info.getStartedTime()));
                        row.setProperty(Fields.FINISH_TIME.toString(), info == null || info.getStartedTime() == null ? "" : dateFormat.format(info.getStartedTime())); // for start process this should be the same
                        row.setProperty(Fields.USERNAME.toString(), process == null || process.getRequesterId() == null ? "" : process.getRequesterId());
                        row.setProperty(Fields.USER_FULLNAME.toString(), Optional.ofNullable(process).map(WorkflowProcess::getRequesterId).map(this::mapUsernameToFullUsername).orElse(""));
                        row.setProperty(Fields.USER_FIRST_NAME.toString(), Optional.ofNullable(process).map(WorkflowProcess::getRequesterId).map(this::mapUsernameToFirstName).orElse(""));

                        final Map<String, String> startProcessValues = getStartProcessValues();
                        startProcessValues.forEach(row::setProperty);

                        // put first process
                        formRowSet.add(row);
                    }
                    return formRowSet;
                }, (rows, activity) -> {
                    final WorkflowActivity info = workflowManager.getRunningActivityInfo(activity.getId());
                    final WorkflowActivity definition = workflowManager.getProcessActivityDefinition(activity.getProcessDefId(), activity.getActivityDefId());
                    final FormRow row = new FormRow();

                    if (SharkConstants.STATE_CLOSED_ABORTED.equals(activity.getState())) {
                        // keep aborted activity first data
                        if (keepCreatedDate == null)
                            keepCreatedDate = info.getCreatedTime();
                        return;
                    }

                    row.setId(activity.getId());
                    row.setProperty(Fields.ID.toString(), row.getId());
                    row.setProperty(Fields.PROCESS_ID.toString(), activity.getProcessDefId());
                    row.setProperty(Fields.PROCESS_NAME.toString(), activity.getProcessName());
                    row.setProperty(Fields.ACTIVITY_ID.toString(), activity.getActivityDefId());
                    row.setProperty(Fields.ACTIVITY_NAME.toString(), activity.getName());

                    row.setProperty(Fields.CREATED_TIME.toString(), dateFormat.format(keepCreatedDate != null ? keepCreatedDate : info.getCreatedTime()));
                    keepCreatedDate = null;

                    if (info.getFinishTime() != null)
                        row.setProperty(Fields.FINISH_TIME.toString(), dateFormat.format(info.getFinishTime()));

                    if (isActivity(definition)) {
                        row.setProperty(Fields.PARTICIPANT.toString(), info.getPerformer());

                        if ("true".equalsIgnoreCase(getPropertyString("toolAsStartProcess")) && WorkflowActivity.TYPE_TOOL.equalsIgnoreCase(definition.getType())) {
                            WorkflowProcess process = workflowManager.getRunningProcessById(primaryKey);
                            row.setProperty(Fields.USERNAME.toString(), process.getRequesterId());
                            row.setProperty(Fields.USER_FULLNAME.toString(), mapUsernameToFullUsername(process.getRequesterId()));
                            row.setProperty(Fields.USER_FIRST_NAME.toString(), mapUsernameToFirstName(process.getRequesterId()));
                        } else {
                            row.setProperty(Fields.USERNAME.toString(), info.getNameOfAcceptedUser() != null ? info.getNameOfAcceptedUser() : String.join(",", info.getAssignmentUsers()));
                            row.setProperty(Fields.USER_FULLNAME.toString(), Arrays.stream(info.getNameOfAcceptedUser() != null ? new String[]{info.getNameOfAcceptedUser()} : info.getAssignmentUsers())
                                    .filter(u -> !u.isEmpty())
                                    .map(this::mapUsernameToFullUsername)
                                    .filter(u -> !u.isEmpty())
                                    .collect(Collectors.joining(","))
                            );
                            row.setProperty(Fields.USER_FIRST_NAME.toString(), Arrays.stream(info.getNameOfAcceptedUser() != null ? new String[]{info.getNameOfAcceptedUser()} : info.getAssignmentUsers())
                                    .filter(u -> !u.isEmpty())
                                    .map(this::mapUsernameToFirstName)
                                    .filter(u -> !u.isEmpty())
                                    .collect(Collectors.joining(","))
                            );
                        }
                    }

                    final Map<String, String> mapPendingValues = new HashMap<>();
                    final Map<String, String> pendingValues = getPendingValues();

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
                    } else {
                        mapPendingValues.putAll(pendingValues);
                        row.putAll(workflowManager.getActivityVariableList(activity.getId()).stream()
                                .collect(
                                        FormRow::new,
                                        (formRow, workflowVariable) -> {
                                            String workflowVariableName = workflowVariable.getName();
                                            formRow.setProperty(workflowVariableName, mapPendingValues.containsKey(workflowVariableName) ? mapPendingValues.get(workflowVariableName) : "");
                                        },
                                        FormRow::putAll));
                    }
                    rows.add(0, row);
                }, FormRowSet::addAll);

        rowSet.setMultiRow(true);

        return rowSet;
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
        return "Audit Tail Monitoring Multirow Load Binder";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        final List<Map<String, String>> workflowVariableOptions = AuditTrailUtil.getWorkflowVariableOptions();

        final JSONObject startProcessToolProperty = new JSONObject();
        try {
            startProcessToolProperty.put("name", "startProcessTool");
            startProcessToolProperty.put("label", "@@monitoringMultirowFormBinder.startProcessTool@@");
            startProcessToolProperty.put("control_field", "toolAsStartProcess");
            startProcessToolProperty.put("control_value", "true");
            startProcessToolProperty.put("type", "selectbox");
            startProcessToolProperty.put("options_ajax", "[CONTEXT_PATH]/web/json/app[APP_PATH]/plugin/" + getClassName() + "/service?type=" + WorkflowActivity.TYPE_TOOL);
        } catch (JSONException ignored) {
        }

        final JSONObject alsoDislpayToolProperty = new JSONObject();
        try {
            alsoDislpayToolProperty.put("name", "alsoDisplayTools");
            alsoDislpayToolProperty.put("label", "@@monitoringMultirowFormBinder.alsoDisplayTools@@");
            alsoDislpayToolProperty.put("type", "multiselect");
            alsoDislpayToolProperty.put("options_ajax", "[CONTEXT_PATH]/web/json/app[APP_PATH]/plugin/" + getClassName() + "/servicetype=" + WorkflowActivity.TYPE_TOOL);
        } catch (JSONException ignored) {
        }

        final JSONObject excludeActivityProperty = new JSONObject();
        try {
            excludeActivityProperty.put("name", "excludeActivities");
            excludeActivityProperty.put("label", "@@monitoringMultirowFormBinder.excludeActivities@@");
            excludeActivityProperty.put("type", "multiselect");
            excludeActivityProperty.put("options_ajax", "[CONTEXT_PATH]/web/json/app[APP_PATH]/plugin/" + getClassName() + "/service?type=" + WorkflowActivity.TYPE_NORMAL);
        } catch (JSONException ignored) {
        }

        String[] args = {
                new JSONArray(workflowVariableOptions).toString().replaceAll("\"", "'"),
                startProcessToolProperty.toString().replaceAll("\"", "'"),
                new JSONArray(workflowVariableOptions).toString().replaceAll("\"", "'"),
                alsoDislpayToolProperty.toString().replaceAll("\"", "'"),
                excludeActivityProperty.toString().replaceAll("\"", "'")
        };
        return AppUtil.readPluginResource(getClassName(), "/properties/AuditTrailMonitoringMultirowLoadBinder.json", args, false, "/messages/AuditTrailMonitoringMultirowFormBinder");
    }

    /**
     * Web service
     */
    @Override
    public void webService(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        final String type = request.getParameter("type");
        final AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();

        final JSONArray result = new JSONArray();

        try {
            // add empty record
            JSONObject empty = new JSONObject();
            empty.put(FormUtil.PROPERTY_VALUE, "");
            empty.put(FormUtil.PROPERTY_LABEL, "");
            result.put(empty);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        final OptionsGenerator generator = new ActivityOptionsGenerator(type == null ? "" : type);
        generator.generate(Try.onConsumer(row -> {
            JSONObject item = new JSONObject();
            item.put(FormUtil.PROPERTY_VALUE, row.getProperty(FormUtil.PROPERTY_VALUE));
            item.put(FormUtil.PROPERTY_LABEL, row.getProperty(FormUtil.PROPERTY_LABEL));
            result.put(item);
        }));

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.getWriter().write(result.toString());
    }

    protected String mapUsernameToFullUsername(String username) {
        ExtDirectoryManager directoryManager = (ExtDirectoryManager) AppUtil.getApplicationContext().getBean("directoryManager");
        User user = directoryManager.getUserByUsername(username);
        if (user == null) {
            return username;
        } else {
            return user.getFirstName() + (user.getLastName() != null ? " " + user.getLastName() : "");
        }
    }
    
    protected String mapUsernameToFirstName(String username) {
        final ExtDirectoryManager directoryManager = (ExtDirectoryManager) AppUtil.getApplicationContext().getBean("directoryManager");
        User user = directoryManager.getUserByUsername(username);
        if (user == null) {
            return username;
        } else {
            return user.getFirstName();
        }
    }

    protected boolean isActivity(WorkflowActivity activityDefinition) {
        return WorkflowActivity.TYPE_NORMAL.equals(activityDefinition.getType());
    }

    protected boolean isTool(WorkflowActivity activityDefinition) {
        return WorkflowActivity.TYPE_TOOL.equals(activityDefinition.getType());
    }

    protected Collection<String> getExcludeActivities() {
        return getMultivalueProperty("excludeActivities");
    }

    protected Collection<String> getStartProcessTool() {
        return getMultivalueProperty("startProcessTool");
    }

    protected Collection<String> getAlsoDisplayTools() {
        return getMultivalueProperty("alsoDisplayTools");
    }

    protected boolean isInExcludedActivity(WorkflowActivity activityDefinition) {
        Collection<String> excludedActivities = getExcludeActivities();
        return excludedActivities.stream().anyMatch(s -> s.equals(activityDefinition.getId()));
    }

    protected boolean isToolAsStartProcess() {
        return "true".equalsIgnoreCase(getPropertyString("toolAsStartProcess"));
    }

    protected boolean isStartProcessTool(WorkflowActivity activityDefinition) {
        return getStartProcessTool().stream().anyMatch(s -> s.equals(activityDefinition.getId()));
    }

    protected boolean isInAlsoDisplayTools(WorkflowActivity activityDefinition) {
        return getAlsoDisplayTools().stream().anyMatch(id -> id.equals(activityDefinition.getId()));
    }

    protected Collection<String> getMultivalueProperty(String propertyName) {
        return Optional.of(propertyName)
                .map(this::getPropertyString)
                .map(s -> s.split(";"))
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * @return
     */
    @Nonnull
    protected Map<String, String> getStartProcessValues() {
        return Optional.ofNullable((Object[])getProperty("startProcessValues"))
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .map(o -> (Map<String, Object>) o)
                .collect(Collectors.toMap(m -> String.valueOf(m.getOrDefault("columnId", "")), m -> AppUtil.processHashVariable(m.getOrDefault("columnValue", "").toString(), null, null, null)));
    }

    protected Map<String, String> getPendingValues() {
        return Optional.ofNullable((Object[]) getProperty("pendingValues"))
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .map(o -> (Map<String, Object>) o)
                .collect(Collectors.toMap(m -> String.valueOf(m.getOrDefault("columnId", "")), m -> AppUtil.processHashVariable(m.getOrDefault("columnValue", "").toString(), null, null, null)));
    }
}
