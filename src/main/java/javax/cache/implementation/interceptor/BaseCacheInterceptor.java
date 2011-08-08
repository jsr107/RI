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




/**
 * Base class for all interceptor implementations, contains utility methods
 *
 * @author Eric Dalquist
 */
public abstract class BaseCacheInterceptor {

    /**
     * Get, check the {@link InterceptorType} and cast the {@link MethodDetails} for the invocation.
     * 
     * @param cacheInvocationContext The invocation context to get the {@link MethodDetails} from.
     * @param interceptorType The current interceptor type, used for validation.
     * @return The casted {@link MethodDetails} object.
     */
    @SuppressWarnings("unchecked")
    protected <T extends KeyedMethodDetails> T getMethodDetails(
            final CacheInvocationContextImpl cacheInvocationContext, final InterceptorType interceptorType) {
        
        final KeyedMethodDetails keyedMethodDetails = cacheInvocationContext.getKeyedMethodDetails();
        
        if (keyedMethodDetails.getInterceptorType() != interceptorType) {
            throw new IllegalStateException("AroundInvoke method for " + interceptorType + " called but MethodDetails.InterceptorType is " + 
                    keyedMethodDetails.getInterceptorType());
        }
        
        return (T)keyedMethodDetails;
    }

}
