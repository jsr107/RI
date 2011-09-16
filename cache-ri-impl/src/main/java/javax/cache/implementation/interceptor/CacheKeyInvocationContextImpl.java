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
import java.util.Arrays;
import java.util.List;

import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKeyInvocationContext;
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
class CacheKeyInvocationContextImpl extends CacheInvocationContextImpl implements CacheKeyInvocationContext<Annotation> {
    private final StaticCacheKeyInvocationContext<? extends Annotation> staticCacheKeyInvocationContext;
    private final CacheInvocationParameter[] keyParameters;
    private final CacheInvocationParameter valueParameter;
    
    /**
     * Create a CacheInvocationContextImpl
     * 
     * @param staticCacheKeyInvocationContext The pre-processed method details data
     * @param invocationContext The current invocation context
     */
    public CacheKeyInvocationContextImpl(StaticCacheKeyInvocationContext<? extends Annotation> staticCacheKeyInvocationContext, 
            InvocationContext invocationContext) {
        
        super(staticCacheKeyInvocationContext, invocationContext);
        
        this.staticCacheKeyInvocationContext = staticCacheKeyInvocationContext;

        final CacheInvocationParameter[] allParameters = this.getAllParameters();
        
        //Build array of key CacheParameterDetails from CacheParameterDetails List
        final List<CacheParameterDetails> keyParameterDetails = staticCacheKeyInvocationContext.getKeyParameters();
        this.keyParameters = new CacheInvocationParameter[keyParameterDetails.size()];
        int pIdx = 0;
        for (final CacheParameterDetails parameterDetails : keyParameterDetails) {
            final int parameterPosition = parameterDetails.getParameterPosition();
            this.keyParameters[pIdx++] = allParameters[parameterPosition];
        }
        
        //If this is for a CachePut get the CacheInvocationParameter for the CacheValue
        if (staticCacheKeyInvocationContext.getInterceptorType() == InterceptorType.CACHE_PUT) {
            final CachePutMethodDetails cachePutMethodDetails = (CachePutMethodDetails)staticCacheKeyInvocationContext;
            final CacheParameterDetails cacheValueParameter = cachePutMethodDetails.getCacheValueParameter();
            final int parameterPosition = cacheValueParameter.getParameterPosition();
            this.valueParameter = allParameters[parameterPosition];
        } else {
            this.valueParameter = null;
        }
    }
    
    /**
     * @return the staticCacheKeyInvocationContext
     */
    public StaticCacheKeyInvocationContext<? extends Annotation> getStaticCacheKeyInvocationContext() {
        return this.staticCacheKeyInvocationContext;
    }

    /* (non-Javadoc)
     * @see javax.cache.annotation.CacheKeyInvocationContext#getKeyParameters()
     */
    @Override
    public CacheInvocationParameter[] getKeyParameters() {
        return Arrays.copyOf(this.keyParameters, this.keyParameters.length);
    }

    /* (non-Javadoc)
     * @see javax.cache.annotation.CacheKeyInvocationContext#getValueParameter()
     */
    @Override
    public CacheInvocationParameter getValueParameter() {
        return this.valueParameter;
    }
}
