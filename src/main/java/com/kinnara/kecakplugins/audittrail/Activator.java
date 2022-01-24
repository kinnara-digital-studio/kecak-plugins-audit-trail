package com.kinnara.kecakplugins.audittrail;

import java.util.ArrayList;
import java.util.Collection;

import com.kinnara.kecakplugins.audittrail.auditplugins.FormDataDaoOnSaveOrUpdateAuditTrail;
import com.kinnara.kecakplugins.audittrail.datalist.AuditTrailConsoleDataListBinder;
import com.kinnara.kecakplugins.audittrail.datalist.FormDataAuditTrailDataListBinder;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    protected Collection<ServiceRegistration> registrationList;

    public void start(BundleContext context) {
        registrationList = new ArrayList<ServiceRegistration>();

        //Register plugin here
        registrationList.add(context.registerService(AuditTrailFormBinder.class.getName(), new AuditTrailFormBinder(), null));
        registrationList.add(context.registerService(AuditTrailFormTableElement.class.getName(), new AuditTrailFormTableElement(), null));
        registrationList.add(context.registerService(AuditTrailMultirowLoadBinder.class.getName(), new AuditTrailMultirowLoadBinder(), null));
        registrationList.add(context.registerService(AuditTrailFormCollector.class.getName(), new AuditTrailFormCollector(), null));
        registrationList.add(context.registerService(AuditTrailMonitoringMultirowFormBinder.class.getName(), new AuditTrailMonitoringMultirowFormBinder(), null));
        registrationList.add(context.registerService(AuditTrailProgress.class.getName(), new AuditTrailProgress(), null));
        registrationList.add(context.registerService(AuditTrailAceFormElement.class.getName(), new AuditTrailAceFormElement(), null));
        registrationList.add(context.registerService(FormDataDaoOnSaveOrUpdateAuditTrail.class.getName(), new FormDataDaoOnSaveOrUpdateAuditTrail(), null));
        registrationList.add(context.registerService(FormDataAuditTrailDataListBinder.class.getName(), new FormDataAuditTrailDataListBinder(), null));
        registrationList.add(context.registerService(AuditTrailConsoleDataListBinder.class.getName(), new AuditTrailConsoleDataListBinder(), null));
    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}