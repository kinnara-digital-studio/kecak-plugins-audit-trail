package com.kinnara.kecakplugins.audittrail;

import org.enhydra.shark.api.common.SharkConstants;
import org.joget.apps.app.service.AppPluginUtil;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.directory.model.User;
import org.joget.directory.model.service.ExtDirectoryManager;
import org.joget.workflow.model.WorkflowActivity;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.WorkflowProcess;
import org.joget.workflow.model.dao.WorkflowProcessLinkDao;
import org.joget.workflow.model.service.WorkflowManager;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MonitoringMultirowFormBinder extends FormBinder implements FormLoadBinder, FormLoadMultiRowElementBinder {

    public enum Fields {

        ID("id", "ID"),
        PROCESS_ID("processId", "Process ID"),
        PROCESS_NAME("processName", "Process Name"),
        ACTIVITY_ID("activityId", "Activity ID"),
        ACTIVITY_NAME("activityName", "Activity Name"),
        CREATED_TIME("createdTime", "Created Time"),
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

    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        // do not load data for empty primary key
        if(primaryKey == null || primaryKey.isEmpty()) {
            return null;
        }

        ApplicationContext appContext = AppUtil.getApplicationContext();
        WorkflowProcessLinkDao workflowProcessLinkDao = (WorkflowProcessLinkDao) appContext.getBean("workflowProcessLinkDao");
        WorkflowManager workflowManager = (WorkflowManager)appContext.getBean("workflowManager");
        WorkflowAssignment wfAssignment = workflowManager.getAssignment(formData.getActivityId());

        return workflowProcessLinkDao.getLinks(primaryKey).stream()
                .filter(Objects::nonNull)
                .flatMap(l -> workflowManager.getActivityList(l.getProcessId(), null, null, null, null).stream())

                // only handle activity
                .filter(activity -> WorkflowActivity.TYPE_NORMAL.equals(workflowManager.getProcessActivityDefinition(activity.getProcessDefId(), activity.getActivityDefId()).getType()))

                // only show unaborted activity
                .filter(activity -> !SharkConstants.STATE_CLOSED_ABORTED.equals(activity.getState()))

                .sorted(Comparator.comparing(WorkflowActivity::getCreatedTime))
                .collect(() -> {
                    // handle first record, get from process'
                    FormRowSet formRowSet = new FormRowSet();
                    WorkflowProcess process = workflowManager.getRunningProcessById(primaryKey);
                    WorkflowProcess info = workflowManager.getRunningProcessInfo(primaryKey);
                    FormRow row = new FormRow();
                    row.put(Fields.ID.toString(), process.getId());
                    row.put(Fields.PROCESS_ID.toString(), process.getId());
                    row.put(Fields.PROCESS_NAME.toString(), process.getName());
                    row.put(Fields.ACTIVITY_ID.toString(), "startProcess");
                    row.put(Fields.ACTIVITY_NAME.toString(), "Start Process");
                    row.put(Fields.CREATED_TIME.toString(), info.getStartedTime());
                    row.put(Fields.USERNAME.toString(), process.getRequesterId());
                    row.put(Fields.USER_FULLNAME.toString(), ((Function<String, String>) u -> {
                        ExtDirectoryManager directoryManager = (ExtDirectoryManager) AppUtil.getApplicationContext().getBean("directoryManager");
                        User user = directoryManager.getUserByUsername(u);
                        if (user == null) {
                            return u;
                        } else {
                            return user.getFirstName() + (user.getLastName() != null ? " " + user.getLastName() : "");
                        }
                    }).apply(process.getRequesterId()));

                    // get first process

                    workflowManager.getProcessVariableList(primaryKey).forEach(v -> LogUtil.info(getClassName(), "Process Variables id ["+v.getId()+"] name ["+v.getName()+"] val ["+v.getVal()+"]"));

                    formRowSet.add(row);
                    return formRowSet;
                }, (formRows, activity) -> {
                    FormRow row = new FormRow();
                    WorkflowActivity info = workflowManager.getRunningActivityInfo(activity.getId());

                    row.put(Fields.ID.toString(), activity.getId());
                    row.put(Fields.PROCESS_ID.toString(), activity.getProcessDefId());
                    row.put(Fields.PROCESS_NAME.toString(), activity.getProcessName());
                    row.put(Fields.ACTIVITY_ID.toString(), activity.getActivityDefId());
                    row.put(Fields.ACTIVITY_NAME.toString(), activity.getName());
                    row.put(Fields.CREATED_TIME.toString(), info.getCreatedTime());
                    row.put(Fields.PARTICIPANT.toString(), info.getPerformer());
                    row.put(Fields.USERNAME.toString(), info.getNameOfAcceptedUser() != null ? info.getNameOfAcceptedUser() : String.join(",", info.getAssignmentUsers()));
                    row.put(Fields.USER_FULLNAME.toString(), Arrays.stream(info.getNameOfAcceptedUser() != null ? new String[] {info.getNameOfAcceptedUser()} : info.getAssignmentUsers())
                            .filter(u -> !u.isEmpty())
                            .map(u -> {
                                ExtDirectoryManager directoryManager = (ExtDirectoryManager)AppUtil.getApplicationContext().getBean("directoryManager");
                                User user = directoryManager.getUserByUsername(u);
                                if(user == null){
                                    return u;
                                } else {
                                    return user.getFirstName() + (user.getLastName() != null ? " " + user.getLastName() : "");
                                }
                            })
                            .filter(u -> !u.isEmpty())
                            .collect(Collectors.joining(","))
                    );

                    // no need to show variables value of current assignment
                    boolean isCurrentAssignment = activity.getState().startsWith(SharkConstants.STATEPREFIX_OPEN);
                    if(!isCurrentAssignment) {
                        row.putAll(workflowManager.getActivityVariableList(activity.getId()).stream()
                                .collect(
                                        FormRow::new,
                                        (formRow, workflowVariable) -> formRow.put(workflowVariable.getName(), workflowVariable.getVal()),
                                        FormRow::putAll));
                    }
                    formRows.add(0, row);
                }, FormRowSet::addAll);
    }

    @Override
    public String getName() {
        return AppPluginUtil.getMessage("monitoringMultirowFormBinder.title", getClassName(), "/messages/MonitoringMultirowFormBinder");
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
        return "";
    }
}
