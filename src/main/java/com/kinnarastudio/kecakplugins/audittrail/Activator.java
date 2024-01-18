package com.kinnarastudio.kecakplugins.audittrail;

import java.util.ArrayList;
import java.util.Collection;

import com.kinnarastudio.kecakplugins.audittrail.auditplugins.FormDataAuditTrailPlugin;
import com.kinnarastudio.kecakplugins.audittrail.auditplugins.FormDataDaoOnSaveOrUpdateAuditTrail;
import com.kinnarastudio.kecakplugins.audittrail.datalist.AuditTrailConsoleDataListBinder;
import com.kinnarastudio.kecakplugins.audittrail.datalist.FormDataAuditTrailDataListBinder;
import com.kinnarastudio.kecakplugins.audittrail.form.*;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    protected Collection<ServiceRegistration> registrationList;

    public void start(BundleContext context) {
        registrationList = new ArrayList<ServiceRegistration>();

        //Register plugin here
        registrationList.add(context.registerService(AuditTrailFormTableElement.class.getName(), new AuditTrailFormTableElement(), null));
        registrationList.add(context.registerService(WorkflowProcessMonitoringMultirowFormBinder.class.getName(), new WorkflowProcessMonitoringMultirowFormBinder(), null));
        registrationList.add(context.registerService(AuditTrailProgress.class.getName(), new AuditTrailProgress(), null));
        registrationList.add(context.registerService(FormDataDaoOnSaveOrUpdateAuditTrail.class.getName(), new FormDataDaoOnSaveOrUpdateAuditTrail(), null));
        registrationList.add(context.registerService(FormDataAuditTrailPlugin.class.getName(), new FormDataAuditTrailPlugin(), null));
        registrationList.add(context.registerService(ElementValueHistoryMultirowLoadBinder.class.getName(), new ElementValueHistoryMultirowLoadBinder(), null));
        registrationList.add(context.registerService(ElementValueHistoryField.class.getName(), new ElementValueHistoryField(), null));
        registrationList.add(context.registerService(FormDataAuditTrailDataListBinder.class.getName(), new FormDataAuditTrailDataListBinder(), null));
        registrationList.add(context.registerService(AuditTrailConsoleDataListBinder.class.getName(), new AuditTrailConsoleDataListBinder(), null));
    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}