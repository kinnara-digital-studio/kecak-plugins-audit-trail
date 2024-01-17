package com.kinnarastudio.kecakplugins.audittrail.form;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONStream;
import com.kinnarastudio.kecakplugins.audittrail.AuditTrailUtil;
import com.kinnarastudio.kecakplugins.audittrail.auditplugins.ElementValueChangesAuditTrail;
import org.joget.apps.app.dao.AuditTrailDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.AuditTrail;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataDaoImpl;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.json.JSONObject;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Load data saved by {@link ElementValueChangesAuditTrail}
 */
public class ElementValueChangesMultirowLoadBinder extends FormBinder implements FormLoadBinder, FormLoadMultiRowElementBinder {
    public final static String LABEL = "Element Value Changes Load Binder";

    @Override
    public String getName() {
        return LABEL;
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
    public String getLabel() {
        return LABEL;
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return null;
    }

    @Override
    public FormRowSet load(Element element, String primaryKey, FormData formData) {
        final AuditTrailDao auditTrailDao = AuditTrailUtil.getAuditTrailDao();
        final AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();

        final String condition = "where appId = ? and appVersion = ? and clazz = ? and method = ?";
        final String[] args = new String[]{
                appDefinition.getAppId(),
                appDefinition.getVersion().toString(),
                FormDataDaoImpl.class.getName(),
                "saveOrUpdate"
        };

        final Form rootForm = FormUtil.findRootForm(element);
        final String formId = rootForm.getPropertyString(FormUtil.PROPERTY_ID);
        final String tableName = rootForm.getPropertyString(FormUtil.PROPERTY_TABLE_NAME);

        final FormRowSet rowSet = Optional.ofNullable(auditTrailDao.getAuditTrails(condition, args, "timestamp", false, null, null))
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(AuditTrail::getMessage)
                .map(Try.onFunction(JSONObject::new))
                .filter(Objects::nonNull)
                .filter(Try.onPredicate(json -> {
                    final String id = json.getString(FormUtil.PROPERTY_ID);
                    final String formDefId = json.getString(ElementValueChangesAuditTrail.KEY_FORM_ID);
                    final String formTable = json.getString(ElementValueChangesAuditTrail.KEY_TABLE_FORM);
                    return id.equals(primaryKey) && tableName.equalsIgnoreCase(formTable);
                }))
                .map(json -> JSONStream.of(json, Try.onBiFunction(JSONObject::getString))
                        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (accept, ignore) -> accept, FormRow::new)))
                .collect(Collectors.toCollection(FormRowSet::new));

        rowSet.setMultiRow(true);

        return rowSet;
    }
}
