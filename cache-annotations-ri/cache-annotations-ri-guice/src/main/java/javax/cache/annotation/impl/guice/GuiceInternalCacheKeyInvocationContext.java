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

package javax.cache.annotation.impl.guice;

import org.aopalliance.intercept.MethodInvocation;

import javax.cache.annotation.impl.AbstractInternalCacheKeyInvocationContext;
import javax.cache.annotation.impl.StaticCacheKeyInvocationContext;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Guice specific cache key invocation context using {@link MethodInvocation}
 *
 * @author Michael Stachel
 * @version $Revision$
 * @param <A> The type of annotation this context information is for. One of {@link javax.cache.annotation.CacheResult},
 * {@link javax.cache.annotation.CachePut}, {@link javax.cache.annotation.CacheRemoveEntry}, or
 * {@link javax.cache.annotation.CacheRemoveAll}.
 */
public class GuiceInternalCacheKeyInvocationContext<A extends Annotation> extends
        AbstractInternalCacheKeyInvocationContext<MethodInvocation, A> {

    /**
     * Create new cache key invocation context for the static context and invocation
     *
     * @param staticCacheKeyInvocationContext Static information about the invoked method
     * @param invocation The AOP Alliance invocation context
     */
    public GuiceInternalCacheKeyInvocationContext(StaticCacheKeyInvocationContext<A> staticCacheKeyInvocationContext,
            MethodInvocation invocation) {
        super(staticCacheKeyInvocationContext, invocation);
    }

    @Override
    protected Object[] getParameters(MethodInvocation invocation) {
        return invocation.getArguments();
    }

    @Override
    protected Method getMethod(MethodInvocation invocation) {
        return invocation.getMethod();
    }

    @Override
    protected Object getTarget(MethodInvocation invocation) {
        return invocation.getThis();
    }

}
