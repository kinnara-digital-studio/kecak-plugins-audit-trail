package com.kinnara.kecakplugins.audittrail.util;

import net.sf.ehcache.Cache;
import org.joget.apps.app.service.AppUtil;
import org.joget.commons.util.LogUtil;

import java.util.Objects;
import java.util.function.Supplier;

public final class Utilities {
    private Utilities() {}

    public static <T> T getFromCache(String cacheKey, Supplier<T> ifNoCache) {

        final Cache cache = (Cache) AppUtil.getApplicationContext().getBean("fluCache");

        net.sf.ehcache.Element cached = cache.get(cacheKey);
        if (cached != null) {
            T value = (T) cached.getObjectValue();
            assert Objects.nonNull(value);

            LogUtil.debug(Utilities.class.getName(), "Cache hit for key [" + cacheKey + "] value [" + value + "]");

            return value;
        }

        assert Objects.nonNull(ifNoCache);

        T value = ifNoCache.get();

        if(value != null) {
            cache.put(new net.sf.ehcache.Element(cacheKey, value));
        }

        return value;
    }
}
