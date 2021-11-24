package com.kinnara.kecakplugins.audittrail;

import java.util.ArrayList;
import java.util.Map;

import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.springframework.context.ApplicationContext;

import com.kinnara.kecakplugins.auditrail.model.AuditTrailModel;

public class AuditTrailAceFormElement extends Element implements FormBuilderPaletteElement{

	@Override
	public String getFormBuilderTemplate() {
		return "<label class='label'>Auditrail ACE Timeline</label>";
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
		return AppUtil.readPluginResource(getClassName(), "/properties/AuditTrailAceFormElement.json", null, false, "/messages/AuditTrailAceFormElement");
	}

	@Override
	public String getName() {
		return "Auditrail ACE Element";
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
		ApplicationContext appContext = AppUtil.getApplicationContext();
		FormDataDao formDataDao = (FormDataDao) appContext.getBean("formDataDao");
		AppDefinition appDef = AppUtil.getCurrentAppDefinition() ;
		AppService appService = (AppService) appContext.getBean("appService");
		
		String primaryKey = formData.getPrimaryKeyValue();
		String foreignKeyField = getPropertyString("foreignKeyField");
		if(primaryKey!=null && !primaryKey.equals("")) {
			String formDefId = (String) getProperty("formDefId");
			String tableName = appService.getFormTableName(appDef, formDefId);
			
			FormRowSet auditTrailData = formDataDao.find(formDefId, tableName, "where "+foreignKeyField+" = ?", new Object[] {primaryKey}, "dateCreated", false, null, null);
			ArrayList<AuditTrailModel> auditL = new ArrayList<AuditTrailModel>();
			for (FormRow formRow : auditTrailData) {
				AuditTrailModel audit = new AuditTrailModel();
				audit.setId(formRow.getId());
				auditL.add(audit);
			}
			dataModel.put("auditrailData", auditL);
		}
		dataModel.put("className", getClassName());
		return FormUtil.generateElementHtml(this, formData, template, dataModel);
	}
	
	@Override
    public String renderAceTemplate(FormData formData, Map dataModel) {
        String template = "AudiTrailAceFormElement.ftl";
        return renderTemplate(template,formData,dataModel);
    }

    @Override
    public String renderAdminLteTemplate(FormData formData, Map dataModel){
        String template = "AudiTrailAceFormElement.ftl";
        return renderTemplate(template,formData,dataModel);
    }

	@Override
	public String renderAdminKitTemplate(FormData formData, Map dataModel) {
		String template = "AudiTrailAceFormElement.ftl";
        return renderTemplate(template,formData,dataModel);
	}

	@Override
	public String renderTemplate(FormData formData, Map dataModel) {
		String template = "AudiTrailAceFormElement.ftl";
        return renderTemplate(template,formData,dataModel);
	}

}
