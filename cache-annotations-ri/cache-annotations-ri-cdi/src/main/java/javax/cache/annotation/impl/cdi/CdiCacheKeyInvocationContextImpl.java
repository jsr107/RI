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

package javax.cache.annotation.impl.cdi;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.cache.annotation.impl.AbstractInternalCacheKeyInvocationContext;
import javax.cache.annotation.impl.StaticCacheKeyInvocationContext;
import javax.interceptor.InvocationContext;

/**
 * @author Eric Dalquist
 * @version $Revision$
 */
public class CdiCacheKeyInvocationContextImpl extends AbstractInternalCacheKeyInvocationContext<InvocationContext> {
    
    public CdiCacheKeyInvocationContextImpl(
            StaticCacheKeyInvocationContext<? extends Annotation> staticCacheKeyInvocationContext,
            InvocationContext invocation) {

        super(staticCacheKeyInvocationContext, invocation);
    }

    /* (non-Javadoc)
     * @see javax.cache.annotation.impl.AbstractInternalCacheInvocationContext#getParameters(java.lang.Object)
     */
    @Override
    protected Object[] getParameters(InvocationContext invocation) {
        return invocation.getParameters();
    }

    /* (non-Javadoc)
     * @see javax.cache.annotation.impl.AbstractInternalCacheInvocationContext#getMethod(java.lang.Object)
     */
    @Override
    protected Method getMethod(InvocationContext invocation) {
        return invocation.getMethod();
    }

    /* (non-Javadoc)
     * @see javax.cache.annotation.impl.AbstractInternalCacheInvocationContext#getTarget(java.lang.Object)
     */
    @Override
    protected Object getTarget(InvocationContext invocation) {
        return invocation.getTarget();
    }

}
