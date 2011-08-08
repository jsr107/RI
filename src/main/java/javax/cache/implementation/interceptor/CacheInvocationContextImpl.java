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
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import javax.cache.interceptor.CacheInvocationContext;
import javax.cache.interceptor.CacheInvocationParameter;
import javax.interceptor.InvocationContext;


/**
 * Provides caching specific context about an intercepted method invocation.
 * 
 * NOTE: This class uses arrays instead of immutable collections due to the frequency of these objects
 * being created. In testing creating new arrays and cloning them requires less CPU time and fewer objects
 * than creating new immutable Lists.
 * 
 * @author Eric Dalquist
 * @version $Revision$
 */
class CacheInvocationContextImpl implements CacheInvocationContext {
    private final KeyedMethodDetails keyedMethodDetails;
    private final InvocationContext invocationContext;
    private final CacheInvocationParameter[] allParameters;
    private final CacheInvocationParameter[] keyParameters;
    private final CacheInvocationParameter valueParameter;
    
    /**
     * Create a CacheInvocationContextImpl
     * 
     * @param keyedMethodDetails The pre-processed method details data
     * @param invocationContext The current invocation context
     */
    public CacheInvocationContextImpl(KeyedMethodDetails keyedMethodDetails, InvocationContext invocationContext) {
        this.keyedMethodDetails = keyedMethodDetails;
        this.invocationContext = invocationContext;
        
        final Object[] parameters = invocationContext.getParameters();
        
        //Build array of all CacheInvocationParameter from CacheParameterDetails List
        final List<CacheParameterDetails> allParameterDetails = keyedMethodDetails.getAllParameters();
        this.allParameters = new CacheInvocationParameter[allParameterDetails.size()];
        for (final CacheParameterDetails parameterDetails : allParameterDetails) {
            final int parameterPosition = parameterDetails.getParameterPosition();
            this.allParameters[parameterPosition] = new CacheInvocationParameterImpl(parameterDetails, parameters[parameterPosition]);
        }
        
        //Build array of key CacheParameterDetails from CacheParameterDetails List
        final List<CacheParameterDetails> keyParameterDetails = keyedMethodDetails.getKeyParameters();
        this.keyParameters = new CacheInvocationParameter[keyParameterDetails.size()];
        int pIdx = 0;
        for (final CacheParameterDetails parameterDetails : keyParameterDetails) {
            final int parameterPosition = parameterDetails.getParameterPosition();
            this.keyParameters[pIdx++] = this.allParameters[parameterPosition];
        }
        
        //If this is for a CachePut get the CacheInvocationParameter for the CacheValue
        if (keyedMethodDetails.getInterceptorType() == InterceptorType.CACHE_PUT) {
            final CachePutMethodDetails cachePutMethodDetails = (CachePutMethodDetails)keyedMethodDetails;
            final CacheParameterDetails cacheValueParameter = cachePutMethodDetails.getCacheValueParameter();
            final int parameterPosition = cacheValueParameter.getParameterPosition();
            this.valueParameter = this.allParameters[parameterPosition];
        } else {
            this.valueParameter = null;
        }
    }
    
    /**
     * @return The method details data
     */
    public KeyedMethodDetails getKeyedMethodDetails() {
        return this.keyedMethodDetails;
    }

    /**
     * @return The underlying invocation context
     */
    public InvocationContext getInvocationContext() {
        return this.invocationContext;
    }

    /* (non-Javadoc)
     * @see javax.cache.interceptor.CacheInvocationContext#getTarget()
     */
    @Override
    public Object getTarget() {
        return this.invocationContext.getTarget();
    }

    /* (non-Javadoc)
     * @see javax.cache.interceptor.CacheInvocationContext#getMethod()
     */
    @Override
    public Method getMethod() {
        return this.invocationContext.getMethod();
    }

    /* (non-Javadoc)
     * @see javax.cache.interceptor.CacheInvocationContext#getAnnotations()
     */
    @Override
    public Set<Annotation> getAnnotations() {
        return this.keyedMethodDetails.getMethodAnotations();
    }

    /* (non-Javadoc)
     * @see javax.cache.interceptor.CacheInvocationContext#getKeyParameters()
     */
    @Override
    public CacheInvocationParameter[] getKeyParameters() {
        //Defensive copy to protect from modification
        return Arrays.copyOf(this.keyParameters, this.keyParameters.length);
    }

    /* (non-Javadoc)
     * @see javax.cache.interceptor.CacheInvocationContext#getAllParameters()
     */
    @Override
    public CacheInvocationParameter[] getAllParameters() {
        //Defensive copy to protect from modification
        return Arrays.copyOf(this.allParameters, this.allParameters.length);
    }

    /* (non-Javadoc)
     * @see javax.cache.interceptor.CacheInvocationContext#getValueParameter()
     */
    @Override
    public CacheInvocationParameter getValueParameter() {
        return this.valueParameter;
    }

    /* (non-Javadoc)
     * @see javax.cache.interceptor.CacheInvocationContext#unwrap(java.lang.Class)
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T> T unwrap(Class<T> cls) {
        if (cls.isAssignableFrom(this.invocationContext.getClass())) {
            return (T)this.invocationContext;
        }
        
        throw new IllegalArgumentException("Unwapping to " + cls + " is not a supported by this implementation");
    }
}
