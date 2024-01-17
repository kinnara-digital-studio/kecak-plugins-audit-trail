package com.kinnarastudio.kecakplugins.audittrail.auditplugins;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.kecakplugins.audittrail.AuditTrailUtil;
import org.joget.apps.app.dao.AuditTrailDao;
import org.joget.apps.app.model.AuditTrail;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDaoImpl;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultAuditTrailPlugin;
import org.joget.plugin.base.PluginManager;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Store data row into audit trail table as json
 */
public class ElementValueChangesAuditTrail extends DefaultAuditTrailPlugin {
    final public static String LABEL = "Element Value Changes On saveOrUpdate";
    final public static String KEY_FORM_ID = "formDefId";
    final public static String KEY_TABLE_FORM = "tableForm";

    @Override
    public String getName() {
        return LABEL;
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
    public Object execute(Map map) {
        final AuditTrail callerAuditTrail = (AuditTrail) map.get("auditTrail");
        if (!isEnabled() || !callerAuditTrail.getClazz().equals(FormDataDaoImpl.class.getName()) || !callerAuditTrail.getMethod().equals("saveOrUpdate")) {
            return null;
        }

        final AuditTrailDao auditTrailDao = AuditTrailUtil.getAuditTrailDao();

        final String formId;
        final String formTable;
        final FormRowSet rowSet;

        final Object[] args = callerAuditTrail.getArgs();
        if (args.length == 2) {
            final Form form = (Form) callerAuditTrail.getArgs()[0];
            formId = form.getPropertyString(FormUtil.PROPERTY_ID);
            formTable = form.getPropertyString(FormUtil.PROPERTY_TABLE_NAME);
            rowSet = (FormRowSet) callerAuditTrail.getArgs()[1];
        } else if (args.length == 3) {
            formId = (String) callerAuditTrail.getArgs()[0];
            formTable = (String) callerAuditTrail.getArgs()[1];
            rowSet = (FormRowSet) callerAuditTrail.getArgs()[2];
        } else {
            LogUtil.warn(getClassName(), "Error storing audittrail data args [" + Optional.of(args).map(Arrays::stream).orElseGet(Stream::empty).map(String::valueOf).collect(Collectors.joining(" | ")) + "]");
            return null;
        }

        rowSet.forEach(Try.onConsumer(row -> {
            final JSONObject json = new JSONObject(row);

            json.put(KEY_FORM_ID, formId);
            json.put(KEY_TABLE_FORM, formTable);

            final AuditTrail auditTrail = new AuditTrail();
            auditTrail.setUsername(callerAuditTrail.getUsername());
            auditTrail.setClazz(callerAuditTrail.getClazz());
            auditTrail.setMethod(callerAuditTrail.getMethod());
            auditTrail.setParamTypes(callerAuditTrail.getParamTypes());
            auditTrail.setArgs(callerAuditTrail.getArgs());
            auditTrail.setReturnObject(callerAuditTrail.getReturnObject());
            auditTrail.setTimestamp(callerAuditTrail.getTimestamp());
            auditTrail.setAppDef(callerAuditTrail.getAppDef());
            auditTrail.setAppId(callerAuditTrail.getAppId());
            auditTrail.setAppVersion(callerAuditTrail.getAppVersion());
            auditTrail.setMessage(json.toString());

            auditTrailDao.addAuditTrail(auditTrail);
        }));

        return null;
    }

    @Override
    public String getLabel() {
        return LABEL;
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/auditplugins/ElementFormDataDaoOnSaveOrUpdateAuditTrail.json", null, true, "messages/auditplugins/FormDataDaoOnSaveOrUpdateAuditTrail");
    }

    protected boolean isEnabled() {
        return "true".equalsIgnoreCase(getPropertyString("enable"));
    }
}
