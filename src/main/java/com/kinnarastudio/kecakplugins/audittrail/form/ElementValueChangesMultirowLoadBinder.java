package com.kinnarastudio.kecakplugins.audittrail.form;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONStream;
import com.kinnarastudio.kecakplugins.audittrail.auditplugins.FormDataAuditTrailPlugin;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.form.dao.FormDataAuditTrailDao;
import org.joget.apps.form.model.*;
import org.joget.apps.form.service.FormUtil;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Load data saved by {@link FormDataAuditTrailPlugin}
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
        final FormDataAuditTrailDao formDataAuditTrailDao = (FormDataAuditTrailDao) AppUtil.getApplicationContext().getBean("formDataAuditTrailDao");
        final AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();

        final Form rootForm = FormUtil.findRootForm(element);
        final String formId = rootForm.getPropertyString(FormUtil.PROPERTY_ID);
        final String tableName = rootForm.getPropertyString(FormUtil.PROPERTY_TABLE_NAME);

        final String condition = "where appId = ? and appVersion = ? and tableName = ? and action = ?";
        final String[] args = new String[]{
                appDefinition.getAppId(),
                appDefinition.getVersion().toString(),
                tableName,
                "saveOrUpdate"
        };

        final FormRowSet rowSet = Optional.ofNullable(formDataAuditTrailDao.getAuditTrails(condition, args, "datetime", true, null, null))
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(FormDataAuditTrail::getData)
                .map(Try.onFunction(JSONObject::new, (JSONException ignored) -> null))
                .filter(Objects::nonNull)
                .filter(Try.onPredicate(json -> {
                    final String id = json.getString(FormUtil.PROPERTY_ID);
                    return id.equals(primaryKey);
                }, (JSONException ignored) -> false))
                .map(json -> JSONStream.of(json, Try.onBiFunction(JSONObject::getString))
                        .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue(), (accept, ignore) -> accept, FormRow::new)))
                .collect(Collectors.toCollection(FormRowSet::new));

        rowSet.setMultiRow(true);

        return rowSet;
    }
}
