package com.kinnara.kecakplugins.audittrail.auditplugins;

import com.kinnara.kecakplugins.audittrail.AuditTrailUtil;
import org.joget.apps.app.dao.AuditTrailDao;
import org.joget.apps.app.model.AuditTrail;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDaoImpl;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormRowSet;
import org.joget.apps.form.service.FormUtil;
import org.joget.commons.util.LogUtil;
import org.joget.plugin.base.DefaultAuditTrailPlugin;
import org.joget.workflow.model.WorkflowAssignment;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FormDataDaoOnSaveOrUpdateAuditTrail extends DefaultAuditTrailPlugin {
    final Collection<String> IGNORED_FIELDS = Collections.unmodifiableCollection(Arrays.asList(
            FormUtil.PROPERTY_ID,
            FormUtil.PROPERTY_ORG_ID,
            FormUtil.PROPERTY_DATE_CREATED,
            FormUtil.PROPERTY_CREATED_BY,
            FormUtil.PROPERTY_DATE_MODIFIED,
            FormUtil.PROPERTY_MODIFIED_BY,
            FormUtil.PROPERTY_DELETED
    ));

    @Override
    public String getName() {
        return getLabel();
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
    public Object execute(Map map) {
        final AuditTrail callerAuditTrail = (AuditTrail) map.get("auditTrail");
        if (!callerAuditTrail.getClazz().equals(FormDataDaoImpl.class.getName()) || !callerAuditTrail.getMethod().equals("saveOrUpdate")) {
            return null;
        }

        final AuditTrailDao auditTrailDao = AuditTrailUtil.getAuditTrailDao();

        final String formId;
        final String formTable;
        final FormRowSet rowSet;

        final Object[] args = callerAuditTrail.getArgs();
        if(args.length == 2) {
            final Form form = (Form) callerAuditTrail.getArgs()[0];
            formId = form.getPropertyString(FormUtil.PROPERTY_ID);
            formTable = form.getPropertyString(FormUtil.PROPERTY_TABLE_NAME);
            rowSet = (FormRowSet) callerAuditTrail.getArgs()[1];
        } else if(args.length == 3){
            formId = (String) callerAuditTrail.getArgs()[0];
            formTable = (String) callerAuditTrail.getArgs()[1];
            rowSet = (FormRowSet) callerAuditTrail.getArgs()[2];
        } else {
            LogUtil.warn(getClassName(), "Error storing audittrail data args ["+ Optional.of(args).map(Arrays::stream).orElseGet(Stream::empty).map(String::valueOf).collect(Collectors.joining(" | ")) +"]");
            return null;
        }

        rowSet.forEach(row -> {
            final String id = row.getId();
            row.forEach((k, v) -> {
                final String field = k.toString();
                final String value = v.toString();

                if(IGNORED_FIELDS.contains(field) || value.isEmpty()) {
                    return;
                }

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

                final String message = getPropertyAuditMessage(callerAuditTrail.getUsername(), formId, formTable, id, field, value, null);
                auditTrail.setMessage(message);

                auditTrailDao.addAuditTrail(auditTrail);
            });
        });

        return null;
    }

    @Override
    public String getLabel() {
        return "FormData On saveOrUpdate Audit Trail";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/auditplugins/FormDataDaoOnSaveOrUpdateAuditTrail.json", null, true, "messages/auditplugins/FormDataDaoOnSaveOrUpdateAuditTrail");
    }

    protected String getPropertyAuditMessage(String username, String formId, String tableName, String primaryKey, String fieldId, String fieldValue, WorkflowAssignment assignment) {
        final String message = AppUtil.processHashVariable(getPropertyString("auditMessage"), assignment, null, null);

        final Map<String, String> variables = new HashMap<>();
        variables.put("user", username);
        variables.put("form", formId);
        variables.put("table", tableName);
        variables.put("id", primaryKey);
        variables.put("field", fieldId);
        variables.put("value", fieldValue);

        return interpolate(message, variables);
    }

    protected String interpolate(String source, Map<String, String> variables) {
        final Pattern p = Pattern.compile("\\{[a-zA-Z0-9_]+}");
        final Matcher m = p.matcher(source);
        final StringBuffer sb = new StringBuffer();

        while (m.find()) {
            final String variable = m.group().replaceAll("^\\{|}$", "");
            final String value = variables.getOrDefault(variable, "");
            if(!value.isEmpty()) {
                m.appendReplacement(sb, value);
            }
        }

        return m.appendTail(sb).toString();
    }
}
