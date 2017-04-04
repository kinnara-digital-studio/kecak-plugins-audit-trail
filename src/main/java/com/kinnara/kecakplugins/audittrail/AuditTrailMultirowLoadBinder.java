package com.kinnara.kecakplugins.audittrail;

import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDao;
import org.joget.apps.form.model.Element;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormBinder;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormLoadBinder;
import org.joget.apps.form.model.FormLoadMultiRowElementBinder;
import org.joget.apps.form.model.FormRowSet;
import org.springframework.context.ApplicationContext;

public class AuditTrailMultirowLoadBinder extends FormBinder
implements FormLoadBinder,
		FormLoadMultiRowElementBinder{

	public String getLabel() {
		return getName();
	}

	public String getClassName() {
		return getClass().getName();
	}

	public String getPropertyOptions() {
		return AppUtil.readPluginResource(getClassName(), "/properties/AuditTrailMultirowLoadBinder.json", null, false, "/messages/AuditTrailMultirowLoadBinder");
	}

	public String getName() {
		return "Kecak Audit Trail Form Load Binder";
	}

	public String getVersion() {
		return getClass().getPackage().getImplementationVersion();
	}

	public String getDescription() {
		return "Artifact ID : kecak-plugins-audit-trail";
	}

	public FormRowSet load(Element element, String primaryKey, FormData formData) {
		ApplicationContext appContext = AppUtil.getApplicationContext();
		FormDataDao formDataDao = (FormDataDao) appContext.getBean("formDataDao");
        Form form = AuditTrailUtil.generateForm(getPropertyString("formDefId"));
        if(form != null) {
        	FormRowSet rowSet = formDataDao.find(form, "WHERE e.customProperties." + getPropertyString("fieldProcessId") + "=?",
        			new Object[] { primaryKey }, null, null, null, null);
        	return rowSet;
        }
		return null;
	}
}
