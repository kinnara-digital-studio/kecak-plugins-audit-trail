package com.kinnara.kecakplugins.audittrail;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.FormBuilderPaletteElement;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormRow;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormUtil;
import org.joget.plugin.base.PluginManager;
import org.joget.workflow.model.WorkflowAssignment;
import org.joget.workflow.model.service.WorkflowManager;
import org.joget.workflow.util.WorkflowUtil;
import org.springframework.context.ApplicationContext;

/**
 *
 * @author Yonathan
 */
public class AuditTrailFormElement extends Element implements FormBuilderPaletteElement {
    @Override
    public String renderTemplate(FormData formData, Map dataModel) {
        HttpServletRequest request = WorkflowUtil.getHttpServletRequest();
        String template = "AuditTrailFormElement.ftl";
        // Data tables datas container
        List<List<String>> datas = new ArrayList<List<String>>();
        List<String> headers = new ArrayList<String>();
        
        // Column id container
        List<String> columnList = new ArrayList<String>();
        
        // Set datatables headerProp
        Object[] columns = (Object[]) getProperty("columns");
        if (columns != null && columns.length > 0) {
            Map<String, String> headerProp = null;
            for (Object o : columns) {
                headerProp = (HashMap<String, String>) o;
                columnList.add(headerProp.get("columnId"));
                headers.add(headerProp.get("columnLabel"));
            }
            dataModel.put("headers", headers);
        }
        if (request != null) {
            ApplicationContext ac = AppUtil.getApplicationContext();
            
            // Get Parameter Value
            String primaryKeyValue = null;
            String mode = request.getParameter("_mode");
            if(mode!=null){
                if(mode.equals("assignment")){
                    // parameter adalah activityId, harus di parse jadi processId
//                    String[] params = request.getParameter("activityId").split("_");
                    primaryKeyValue = "";//params[1]+"_"+params[2]+"_"+params[3];
//                    LogUtil.info(ApproverAuditrail.class.getName(), "ProcessId: "+primaryKeyValue);
                    // Generate instance worklow assignment
                    PluginManager pluginManager = (PluginManager) ac.getBean("pluginManager");
                    WorkflowManager workflowManager = (WorkflowManager) pluginManager.getBean("workflowManager");
                    WorkflowAssignment wfa = workflowManager.getAssignment(request.getParameter("activityId"));
                    if(wfa!=null){
                        // cari processID si parent flow dari process id subflow
                        AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
                        primaryKeyValue = appService.getOriginProcessId(wfa.getProcessId());
                    }
                }else{
                    // parameter sudah processId
                    primaryKeyValue = request.getParameter("id");
                }
                
            	FormRowSet rowSet = formData.getLoadBinderData(this);
            	for(FormRow row : rowSet) {                		
            		List<String> contentList = new ArrayList<String>();
                    for(int i = 0, size = columnList.size(); i < size;i++){
                        contentList.add(row.getProperty((String)columnList.get(i)));
                    }
                    datas.add(contentList);
            	}
                
                String enableSort = getPropertyString("enableSort");
                if(enableSort!=null && enableSort.equals("true")){
                    if(getPropertyString("sortBy")!=null && !getPropertyString("sortBy").equals("")){
                        dataModel.put("sort", Integer.parseInt(getPropertyString("sortBy"))-1);
                    }
                }
                dataModel.put("datas", datas);
                dataModel.put("error", "false");
            }else{
                dataModel.put("error", "true");
            }
        } else {
            dataModel.put("error", "true");
        }
        String html = FormUtil.generateElementHtml(this, formData, template, dataModel);
        return html;
    }

    public String getName() {
        return "Kecak Audit Trail Form Element";
    }

    public String getVersion() {
        return "1.0.0";
    }

    public String getDescription() {
        return "Get list of approval history for each process";
    }

    public String getLabel() {
        return "Audit Trail Form Element";
    }

    public String getClassName() {
        return this.getClass().getName();
    }

    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClass().getName(), "/properties/AuditTrailFormElement.json", new Object[] { AuditTrailMultirowLoadBinder.class.getName() } , true, "/messages/AuditTrailFormElement");
    }

    public String getFormBuilderCategory() {
        return "Kecak Enterprise";
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

    public void webService(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}