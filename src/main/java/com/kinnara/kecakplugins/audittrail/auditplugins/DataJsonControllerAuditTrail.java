package com.kinnara.kecakplugins.audittrail.auditplugins;

import com.kinnara.kecakplugins.audittrail.AuditTrailUtil;
import org.joget.apps.app.dao.AuditTrailDao;
import org.joget.apps.app.model.AuditTrail;
import org.joget.plugin.base.DefaultAuditTrailPlugin;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class DataJsonControllerAuditTrail extends DefaultAuditTrailPlugin {
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
        final AuditTrail auditTrail = (AuditTrail) map.get("auditTrail");

        if (!auditTrail.getClazz().endsWith("DataJsonController")) {
            return null;
        }

        final HttpServletRequest request = AuditTrailUtil.getArgumentByClassType(auditTrail, HttpServletRequest.class);
        if(request == null) {
            return null;
        }

        if (!getPropertyRequestMethod().isEmpty() && !getPropertyRequestMethod().contains(request.getMethod())) {
            return null;
        }

        final AuditTrailDao auditTrailDao = AuditTrailUtil.getAuditTrailDao();
        auditTrailDao.addAuditTrail(auditTrail);

        return null;
    }

    @Override
    public String getLabel() {
        return "DataJsonController Audit Trail";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return "";
    }

    protected Set<String> getPropertyRequestMethod() {
        return Collections.emptySet();
    }
}
