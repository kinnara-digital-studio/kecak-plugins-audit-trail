package com.kinnarastudio.kecakplugins.audittrail.auditplugins;

import com.kinnarastudio.commons.Try;
import org.joget.apps.app.model.AuditTrail;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataAuditTrailDao;
import org.joget.apps.form.dao.FormDataDaoImpl;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultAuditTrailPlugin;
import org.joget.plugin.base.PluginManager;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Store data row into audit trail table as json
 */
public class FormDataAuditTrail extends DefaultAuditTrailPlugin {
    final public static String LABEL = "Form Data Audit Trail";
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
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final FormDataAuditTrailDao formDataAuditTrailDao = (FormDataAuditTrailDao) applicationContext.getBean("formDataAuditTrailDao");

        final AuditTrail callerAuditTrail = (AuditTrail) map.get("auditTrail");
        if (!callerAuditTrail.getClazz().equals(FormDataDaoImpl.class.getName()) || !callerAuditTrail.getMethod().equals("saveOrUpdate")) {
            return null;
        }

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

        if(shouldHandleThisForm(formId)) {
            rowSet.forEach(Try.onConsumer(row -> {
                final JSONObject json = new JSONObject(row);

                final org.joget.apps.form.model.FormDataAuditTrail formDataAuditTrail = new org.joget.apps.form.model.FormDataAuditTrail();
                formDataAuditTrail.setId(UUID.randomUUID().toString());
                formDataAuditTrail.setAppId(callerAuditTrail.getAppId());
                formDataAuditTrail.setAppVersion(callerAuditTrail.getAppVersion());
                formDataAuditTrail.setFormId(formId);
                formDataAuditTrail.setAction(callerAuditTrail.getMethod());
                formDataAuditTrail.setTableName(formTable);
                formDataAuditTrail.setDatetime(callerAuditTrail.getTimestamp());
                formDataAuditTrail.setUsername(callerAuditTrail.getUsername());
                formDataAuditTrail.setData(json.toString());

                formDataAuditTrailDao.addAuditTrail(formDataAuditTrail);
            }));
        }

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
        return AppUtil.readPluginResource(getClassName(), "/properties/auditplugins/FormDataAuditTrail.json", null, true, "messages/auditplugins/FormDataDaoOnSaveOrUpdateAuditTrail");
    }

    protected Set<String> getFormDefIds() {
        return Optional.of(getPropertyString("formDefId"))
                .map(s -> s.split(";"))
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }


    /**
     * Should handle [formDefId] form?
     *
     * @param formDefId
     * @return
     */
    protected boolean shouldHandleThisForm(String formDefId) {
        final Collection<String> forms = getFormDefIds();
        return forms.isEmpty() || forms.contains(formDefId);
    }
}
