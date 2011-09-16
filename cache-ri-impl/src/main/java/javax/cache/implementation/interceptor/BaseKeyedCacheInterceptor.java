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
package javax.cache.implementation.interceptor;

import java.lang.annotation.Annotation;




/**
 * Base class for all interceptor implementations, contains utility methods
 *
 * @author Eric Dalquist
 * @param <T> The type of static invocation context data expected
 */
public abstract class BaseKeyedCacheInterceptor<T extends StaticCacheKeyInvocationContext<?>> {

    /**
     * Get, check the {@link InterceptorType} and cast the {@link CacheMethodDetailsImpl} for the invocation.
     * 
     * @param cacheInvocationContext The invocation context to get the {@link CacheMethodDetailsImpl} from.
     * @param interceptorType The current interceptor type, used for validation.
     * @return The casted {@link CacheMethodDetailsImpl} object.
     */
    @SuppressWarnings("unchecked")
    protected T getStaticCacheKeyInvocationContext(
            final CacheKeyInvocationContextImpl cacheInvocationContext, final InterceptorType interceptorType) {
        
        final StaticCacheKeyInvocationContext<? extends Annotation> staticCacheKeyInvocationContext = 
                cacheInvocationContext.getStaticCacheKeyInvocationContext();
        
        if (staticCacheKeyInvocationContext.getInterceptorType() != interceptorType) {
            throw new IllegalStateException("AroundInvoke method for " + interceptorType + " called but MethodDetails.InterceptorType is " + 
                    staticCacheKeyInvocationContext.getInterceptorType());
        }
        
        return (T)staticCacheKeyInvocationContext;
    }

}
