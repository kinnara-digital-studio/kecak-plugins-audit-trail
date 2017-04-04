package com.kinnara.kecakplugins.audittrail;

import java.util.ArrayList;
import java.util.Collection;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

public class Activator implements BundleActivator {

    protected Collection<ServiceRegistration> registrationList;

    public void start(BundleContext context) {
        registrationList = new ArrayList<ServiceRegistration>();

        //Register plugin here
        registrationList.add(context.registerService(AuditTrailFormBinder.class.getName(), new AuditTrailFormBinder(), null));
        registrationList.add(context.registerService(AuditTrailFormElement.class.getName(), new AuditTrailFormElement(), null));
        registrationList.add(context.registerService(AuditTrailMultirowLoadBinder.class.getName(), new AuditTrailMultirowLoadBinder(), null));
    }

    public void stop(BundleContext context) {
        for (ServiceRegistration registration : registrationList) {
            registration.unregister();
        }
    }
}