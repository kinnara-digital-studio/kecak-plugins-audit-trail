package com.kinnarastudio.kecakplugins.audittrail.datalist;

import com.kinnarastudio.commons.Try;
import com.kinnarastudio.commons.jsonstream.JSONStream;
import org.joget.apps.app.dao.FormDefinitionDao;
import org.joget.apps.app.model.AppDefinition;
import org.joget.apps.app.model.FormDefinition;
import org.joget.apps.app.service.AppService;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.*;
import org.joget.apps.form.dao.FormDataAuditTrailDao;
import org.joget.apps.form.model.Form;
import org.joget.apps.form.model.FormData;
import org.joget.apps.form.model.FormDataAuditTrail;
import org.joget.apps.form.service.FormService;
import org.joget.apps.form.service.FormUtil;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class FormDataAuditTrailDataListBinder extends DataListBinderDefault {
    public final static String FIELD_ID = "id";
    public final static String FIELD_APP_ID = "appId";
    public final static String FIELD_APP_VERSION = "appVersion";
    public final static String FIELD_FORM_ID = "formId";
    public final static String FIELD_TABLE_NAME = "tableName";
    public final static String FIELD_USERNAME = "username";
    public final static String FIELD_ACTION = "action";
    public final static String FIELD_DATA = "data";
    public final static String FIELD_DATETIME = "datetime";

    public final static String PREFIX_FORM_DATA = "fd_";

    public final static DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

    @Override
    public DataListColumn[] getColumns() {
        final Optional<Form> optForm = getSelectedForm(getPropertyString("formDefId"));
        final FormData formData = new FormData();
        return getData(null, "datetime", true, 0, 10).stream()
                .map(Map::keySet)
                .flatMap(Collection::stream)
                .distinct()
                .map(s -> {
                    final String elementId = s.replaceAll("^" + PREFIX_FORM_DATA, "");
                    final String fieldName = optForm
                            .map(f -> FormUtil.findElement(elementId, f, formData))
                            .map(e -> e.getPropertyString(FormUtil.PROPERTY_LABEL))
                            .orElse(s);
                    return new DataListColumn(s, fieldName, true);
                })
                .toArray(DataListColumn[]::new);
    }

    @Override
    public String getPrimaryKeyColumnName() {
        return FIELD_ID;
    }

    @Override
    public DataListCollection getData(DataList dataList, Map properties, DataListFilterQueryObject[] dataListFilter, String sort, Boolean desc, Integer start, Integer rows) {
        final DataListFilterQueryObject filter = getFilter(properties, dataListFilter);
        return getData(filter, sort, desc, start, rows);
    }

    @Override
    public int getDataTotalRowCount(DataList dataList, Map properties, DataListFilterQueryObject[] dataListFilter) {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final FormDataAuditTrailDao formDataAuditTrailDao = (FormDataAuditTrailDao) applicationContext.getBean("formDataAuditTrailDao");

        final DataListFilterQueryObject filter = getFilter(properties, dataListFilter);

        @Nullable
        final String condition = filter == null ? null : filter.getQuery();

        @Nullable
        final String[] arguments = filter == null ? null :filter.getValues();

        return formDataAuditTrailDao.count(condition, arguments).intValue();
    }

    @Override
    public String getName() {
        return getClass().getName();
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
        return "FormData Audit Trail DataList Binder";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/datalist/FormDataAuditTrailDataListBinder.json", null, true, "/messages/datalist/FormDataAuditTrailDataListBinder");
    }

    protected DataListFilterQueryObject getFilter(Map<String, Object> properties, DataListFilterQueryObject[] datalistFilters) {
        final AppDefinition appDefinition = AppUtil.getCurrentAppDefinition();
        final DataListFilterQueryObject filter = new DataListFilterQueryObject();

        final String condition = compressCondition(datalistFilters);
        filter.setQuery("where appId = ? and tableName = ? " + condition);

        final List<String> arguments = new ArrayList<>();
        arguments.add(appDefinition.getAppId());
        arguments.add(getTableName(properties));
        arguments.addAll(compressArguments(datalistFilters));

        filter.setValues(arguments.toArray(new String[0]));
        return filter;
    }

    protected String getTableName(Map<String, Object> properties) {
        final AppService appService = (AppService) AppUtil.getApplicationContext().getBean("appService");
        final String formDefId = properties.get("formDefId").toString();
        return appService.getFormTableName(AppUtil.getCurrentAppDefinition(), formDefId);
    }

    protected String compressCondition(@Nullable DataListFilterQueryObject[] filterQueryObjects) {
        return Optional.ofNullable(filterQueryObjects)
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .map(f -> String.join(" ", f.getOperator(), f.getQuery()))
                .collect(Collectors.joining(" "));
    }

    protected List<String> compressArguments(@Nullable DataListFilterQueryObject[] filterQueryObject) {
        return Optional.ofNullable(filterQueryObject)
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .map(DataListFilterQueryObject::getValues)
                .flatMap(Arrays::stream)
                .collect(Collectors.toList());
    }

    @Nonnull
    protected DataListCollection<Map<String, String>> getData(DataListFilterQueryObject filter, String sort, Boolean desc, Integer start, Integer rows) {
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final FormDataAuditTrailDao formDataAuditTrailDao = (FormDataAuditTrailDao) applicationContext.getBean("formDataAuditTrailDao");

        @Nullable
        final String condition = filter == null ? "" : filter.getQuery();

        @Nullable
        final String[] arguments = filter == null ? null :filter.getValues();

        return Optional.of(formDataAuditTrailDao)
                .map(dao -> dao.getAuditTrails(condition, arguments, sort, desc, start, rows))
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(auditTrail -> {
                    final Map<String, String> row = new HashMap<>();
                    row.put(FIELD_ID, auditTrail.getId());
                    row.put(FIELD_APP_ID, auditTrail.getAppId());
                    row.put(FIELD_APP_VERSION, auditTrail.getAppVersion());
                    row.put(FIELD_FORM_ID, auditTrail.getFormId());
                    row.put(FIELD_TABLE_NAME, auditTrail.getTableName());
                    row.put(FIELD_USERNAME, auditTrail.getUsername());
                    row.put(FIELD_ACTION, auditTrail.getAction());
                    row.put(FIELD_DATETIME, DATE_FORMAT.format(auditTrail.getDatetime()));

                    Optional.of(auditTrail)
                            .map(FormDataAuditTrail::getData)
                            .map(Try.onFunction(JSONArray::new))
                            .flatMap(a -> JSONStream.of(a, Try.onBiFunction(JSONArray::getJSONObject)).findFirst())
                            .map(json -> JSONStream.of(json, Try.onBiFunction(JSONObject::getString)))
                            .orElseGet(Stream::empty)
                            .forEach(e -> row.put(PREFIX_FORM_DATA + e.getKey(), e.getValue()));

                    return row;
                })
                .collect(Collectors.toCollection(DataListCollection::new));
    }

    private Form cachedForm = null;

    protected Optional<Form> getSelectedForm(String formDefId) {
        if(cachedForm != null) {
            return Optional.of(cachedForm);
        }

        final AppDefinition appDef = AppUtil.getCurrentAppDefinition();
        final ApplicationContext applicationContext = AppUtil.getApplicationContext();
        final FormDefinitionDao formDefinitionDao = (FormDefinitionDao) applicationContext.getBean("formDefinitionDao");
        final FormService formService = (FormService) applicationContext.getBean("formService");
        final FormDefinition formDef = formDefinitionDao.loadById(formDefId, appDef);

        return Optional.ofNullable(formDef)
                .map(FormDefinition::getJson)
                .map(formService::createElementFromJson)
                .map(f -> cachedForm = (Form)f);
    }
}
