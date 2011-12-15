/**
 *  Copyright 2011 Terracotta, Inc.
 *  Copyright 2011 Oracle America Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package javax.cache.annotation.impl.guice.module;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import org.aopalliance.intercept.MethodInvocation;

import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheRemoveEntry;
import javax.cache.annotation.CacheResolverFactory;
import javax.cache.annotation.CacheResult;
import javax.cache.annotation.impl.CacheContextSource;
import javax.cache.annotation.impl.DefaultCacheKeyGenerator;
import javax.cache.annotation.impl.DefaultCacheResolverFactory;
import javax.cache.annotation.impl.guice.CacheLookupUtil;
import javax.cache.annotation.impl.guice.CachePutInterceptor;
import javax.cache.annotation.impl.guice.CacheRemoveAllInterceptor;
import javax.cache.annotation.impl.guice.CacheRemoveEntryInterceptor;
import javax.cache.annotation.impl.guice.CacheResultInterceptor;

/**
 * Standard cache module for binding all cache interceptors to their respective annotations. This module needs to be part of the
 * Guice injector instantiation to activate intercepting of the cache annotations.
 * Every interceptor is bound twice due to the fact that the annotations defining the joinpoints have retention type
 * Method and Type.
 *
 * @author Michael Stachel
 * @version $Revision$
 */
public class CacheAnnotationsModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(CacheKeyGenerator.class).to(DefaultCacheKeyGenerator.class);
        bind(CacheResolverFactory.class).to(DefaultCacheResolverFactory.class);
        bind(new TypeLiteral<CacheContextSource<MethodInvocation>>() { }).to(CacheLookupUtil.class);

        CachePutInterceptor cachePutInterceptor = new CachePutInterceptor();
        requestInjection(cachePutInterceptor);
        bindInterceptor(Matchers.annotatedWith(CachePut.class), Matchers.any(), cachePutInterceptor);
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(CachePut.class), cachePutInterceptor);

        CacheResultInterceptor cacheResultInterceptor = new CacheResultInterceptor();
        requestInjection(cacheResultInterceptor);
        bindInterceptor(Matchers.annotatedWith(CacheResult.class), Matchers.any(), cacheResultInterceptor);
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(CacheResult.class), cacheResultInterceptor);

        CacheRemoveEntryInterceptor cacheRemoveEntryInterceptor = new CacheRemoveEntryInterceptor();
        requestInjection(cacheRemoveEntryInterceptor);
        bindInterceptor(Matchers.annotatedWith(CacheRemoveEntry.class), Matchers.any(), cacheRemoveEntryInterceptor);
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(CacheRemoveEntry.class), cacheRemoveEntryInterceptor);

        CacheRemoveAllInterceptor cacheRemoveAllInterceptor = new CacheRemoveAllInterceptor();
        requestInjection(cacheRemoveAllInterceptor);
        bindInterceptor(Matchers.annotatedWith(CacheRemoveAll.class), Matchers.any(), cacheRemoveAllInterceptor);
        bindInterceptor(Matchers.any(), Matchers.annotatedWith(CacheRemoveAll.class), cacheRemoveAllInterceptor);
    }

}
