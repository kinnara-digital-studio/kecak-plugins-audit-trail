package com.kinnara.kecakplugins.audittrail;

import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FileUtil;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.FileManager;
import org.joget.commons.util.LogUtil;
import org.joget.directory.dao.UserDao;
import org.joget.directory.model.User;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.model.service.WorkflowUserManager;
import org.springframework.context.ApplicationContext;
import sun.rmi.runtime.Log;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class AuditTrailProgress extends Element implements FormBuilderPaletteElement{


    @Override
    public String getFormBuilderTemplate() {
        return "<ul> " +
                "<li>" +
                "			<span>Status</span><br>" +
                "			<span>dd MMM yyyy</span><br>" +
                "			<span>m:hh sa</span>" +
                "		</li>" +
                "</ul>";
    }

    @Override
    public String getFormBuilderCategory() {
        return "Kecak";
    }

    @Override
    public int getFormBuilderPosition() {
        return 1;
    }

    @Override
    public String getFormBuilderIcon() {
        return "/plugin/org.joget.apps.form.lib.Grid/images/grid_icon.gif";
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
        return AppUtil.readPluginResource(getClass().getName(), "/properties/AuditTrailProgress.json", null, true, "/messages/MofizAuditList");
    }

    @Override
    public String getName() {
        return "Audit Progress List";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public String getDescription() {
        return this.getName();
    }

    @Override
    public String renderTemplate(FormData formData, Map dataModel) {
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        FormDataDao formDataDao = (FormDataDao) AppUtil.getApplicationContext().getBean("formDataDao");
        List<Map<String,String>> auditList = new ArrayList<Map<String,String>>();
        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
        String tableName = appService.getFormTableName(appDef, getPropertyString("formDefId"));
        String primaryKey = formData.getPrimaryKeyValue();
        String fPK = getPropertyString("fieldProcessId");
        if(!fPK.equals("id")){
            fPK = "c_" + fPK;
        }
        FormRowSet rs = formDataDao.find(getPropertyString("formDefId"), tableName, " WHERE "+fPK+"=?",
                new Object[] { primaryKey }, null, null, null, null);
        String filePath = "/web/client/app/" + appDef.getAppId() + "/" + appDef.getVersion() + "/form/download/";
        if(rs != null) {
            for(FormRow row : rs) {
                Map<String, String> auditRow = new HashMap<String, String>();
                auditRow.put("image", "");
                FormRow rUser = null;
                if(!getPropertyString("formUserDef").isEmpty()){
                    String userTable = appService.getFormTableName(appDef, getPropertyString("formUserDef"));
                    String fieldPK = getPropertyString("fieldUserPrimaryKey");
                    if(!fieldPK.equals("id")){
                        fieldPK = "c_" + fieldPK;
                    }
                    FormRowSet rsUser = formDataDao.find(getPropertyString("formUserDef"), userTable, " WHERE "+fieldPK+"=?",
                            new Object[] { row.getProperty(getPropertyString("fieldUserForeignKey")) }, null, null, null, null);
                    if(!rsUser.isEmpty()) {
                        rUser = rsUser.get(0);
                        if(!getPropertyString("fieldImageSource").isEmpty()) {
                            String img = filePath + getPropertyString("formUserDef") + "/" + rUser.getId() + "/" + rUser.getProperty(getPropertyString("fieldImageSource")) + ".";
                            auditRow.put("image", img);
                        }
                    }
                }
                String status = getAuditStatus(row.getProperty(getPropertyString("fieldStatus")), row,rUser, formData.getActivityId());
                auditRow.put("status", status);
                SimpleDateFormat dateString = new SimpleDateFormat("yyyy-MM-dd");
                auditRow.put("date", dateString.format(row.getDateCreated()));
                SimpleDateFormat timeString = new SimpleDateFormat("HH:mm");
                auditRow.put("time", timeString.format(row.getDateCreated()));
                auditList.add(auditRow);
            }
        }
        dataModel.put("auditList", auditList);
        dataModel.put("className", getClassName());

        return FormUtil.generateElementHtml(this, formData, "AuditTrailProgress.ftl", dataModel);
    }

    private String getAuditStatus(String status, FormRow r, FormRow rsUser, String activityId) {
        ApplicationContext appContext = AppUtil.getApplicationContext();
        WorkflowManager wfManager = (WorkflowManager)appContext.getBean("workflowManager");
        WorkflowAssignment wfAssignment = wfManager.getAssignment(activityId);
        UserDao userdao = (UserDao) AppUtil.getApplicationContext().getBean("userDao");
        AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        String auditStatus = "";
        Object[] params = (Object[]) getProperty("statusReplacement");
        String currentText = "";
        for (Object o : params) {
            Map<String, String> row = (Map<String, String>) o;
            if(status.equals(row.get("status"))){
                if(!row.get("text").equals(currentText)) {
                    auditStatus = AppUtil.processHashVariable(row.get("text"), wfAssignment, null, null, appDef);
                    currentText = row.get("text");
                }
                LogUtil.info(getClassName(),row.get("replaceKey") + " - " + rsUser.getProperty(row.get("replaceValue")));
                if(rsUser != null && rsUser.getProperty(row.get("replaceValue")) != null) {
                    auditStatus = auditStatus.replace(row.get("replaceKey"), rsUser.getProperty(row.get("replaceValue")));
                } else if(r.getProperty(row.get("replaceValue")) != null){
                    User user = userdao.getUser(r.getProperty(row.get("replaceValue")));
                    if (user != null && !user.getFirstName().isEmpty()) {
                        auditStatus = auditStatus.replace(row.get("replaceKey"), user.getFirstName() + ((!user.getLastName().isEmpty()) ? " " + user.getLastName() : ""));
                    } else {
                        auditStatus = auditStatus.replace(row.get("replaceKey"), r.getProperty(row.get("replaceValue")));
                    }
                }
            }
        }
        return auditStatus;
    }

}
