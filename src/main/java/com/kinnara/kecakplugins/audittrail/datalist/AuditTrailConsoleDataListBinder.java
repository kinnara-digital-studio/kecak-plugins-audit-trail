package com.kinnara.kecakplugins.audittrail.datalist;

import com.kinnara.kecakplugins.audittrail.AuditTrailUtil;
import org.joget.apps.app.dao.AuditTrailDao;
import org.joget.apps.app.service.AppUtil;
import org.joget.apps.datalist.model.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AuditTrailConsoleDataListBinder extends DataListBinderDefault {

    @Override
    public DataListColumn[] getColumns() {
        return new DataListColumn[] {
                new DataListColumn("id", "ID", false),
                new DataListColumn("username", "Usermame", true),
                new DataListColumn("clazz", "Class", true),
                new DataListColumn("method", "Method", true),
                new DataListColumn("message", "Message", true),
                new DataListColumn("timestamp", "Timestamp", true),
                new DataListColumn("appId", "App ID", true),
                new DataListColumn("appVersion", "App Version", true)
        };
    }

    @Override
    public String getPrimaryKeyColumnName() {
        return "id";
    }

    @Override
    public DataListCollection getData(DataList dataList, Map properties, DataListFilterQueryObject[] filterQueryObjects, String sort, Boolean desc, Integer starts, Integer rows) {
        final AuditTrailDao auditTrailDao = AuditTrailUtil.getAuditTrailDao();
        final DataListFilterQueryObject processedFilterQueryObject = processFilterQueryObjects(filterQueryObjects);
        final DateFormat dateFormat = new SimpleDateFormat(getPropertyTimestampDateFormat());

        final String condition = String.join(" ", getPropertyExtraCondition(properties), processedFilterQueryObject.getQuery());
        final String[] arguments = processedFilterQueryObject.getValues();

        return Optional.of(auditTrailDao)
                .map(dao -> dao.getAuditTrails("where " + condition, arguments, sort, desc, starts, rows))
                .map(Collection::stream)
                .orElseGet(Stream::empty)
                .map(auditTrail -> {
                    final Map<String, String> row = new HashMap<>();
                    row.put("id", auditTrail.getId());
                    row.put("username", auditTrail.getUsername());
                    row.put("clazz", auditTrail.getClazz());
                    row.put("method", auditTrail.getMethod());
                    row.put("message", auditTrail.getMessage());
                    row.put("timestamp", dateFormat.format(auditTrail.getTimestamp()));
                    row.put("appId", auditTrail.getAppId());
                    row.put("appVersion", auditTrail.getAppVersion());
                    return row;
                })
                .collect(Collectors.toCollection(DataListCollection::new));

    }

    @Override
    public int getDataTotalRowCount(DataList dataList, Map properties, DataListFilterQueryObject[] filterQueryObjects) {
        final AuditTrailDao auditTrailDao = AuditTrailUtil.getAuditTrailDao();

        final DataListFilterQueryObject processedFilterQueryObject = processFilterQueryObjects(filterQueryObjects);
        final String condition = String.join(" ", getPropertyExtraCondition(properties), processedFilterQueryObject.getQuery()) ;
        final String[] arguments = processedFilterQueryObject.getValues();

        return Optional.of(auditTrailDao)
                .map(dao -> dao.count("where " + condition, arguments))
                .orElse(0L)
                .intValue();
    }

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
    public String getLabel() {
        return "AuditTrail Console DataList Binder";
    }

    @Override
    public String getClassName() {
        return getClass().getName();
    }

    @Override
    public String getPropertyOptions() {
        return AppUtil.readPluginResource(getClassName(), "/properties/datalist/AuditTrailConsoleDataListBinder.json", null, true, "/messages/datalist/AuditTrailConsoleDataListBinder");
    }

    protected String getPropertyTimestampDateFormat() {
        return AppUtil.processHashVariable(getPropertyString("timestampDateFormat"), null, null, null);
    }

    @Override
    public DataListFilterQueryObject processFilterQueryObjects(DataListFilterQueryObject[] filterQueryObjects) {
        final DataListFilterQueryObject filter = new DataListFilterQueryObject();

        final String query = Optional.ofNullable(filterQueryObjects)
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .map(f -> {
                    final String operator = Optional.of(f)
                            .map(DataListFilterQueryObject::getOperator)
                            .filter(s -> !s.isEmpty())
                            .orElse("and");

                    return String.join(" ", operator, f.getQuery());
                })
                .collect(Collectors.joining(" "));
        filter.setQuery(query);

        final String[] values = Optional.ofNullable(filterQueryObjects)
                .map(Arrays::stream)
                .orElseGet(Stream::empty)
                .map(DataListFilterQueryObject::getValues)
                .filter(Objects::nonNull)
                .flatMap(Arrays::stream)
                .toArray(String[]::new);
        filter.setValues(values);

        return filter;
    }

    protected String getPropertyExtraCondition(Map<String, String> properties) {
        final String appId = AppUtil.getCurrentAppDefinition().getAppId();
        final String extraCondition = properties
                .getOrDefault("extraCondition", "")
                .trim()
                .replaceAll("where", "");
        return "appId = '" + appId + "' " + (extraCondition.isEmpty() ? "" : String.join(" ", "and", extraCondition));
    }
}
