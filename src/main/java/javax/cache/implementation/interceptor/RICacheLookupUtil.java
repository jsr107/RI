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
import java.lang.annotation.AnnotationFormatError;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.cache.Cache;
import javax.cache.interceptor.CacheDefaults;
import javax.cache.interceptor.CacheKeyGenerator;
import javax.cache.interceptor.CacheKeyParam;
import javax.cache.interceptor.CachePut;
import javax.cache.interceptor.CacheRemoveAll;
import javax.cache.interceptor.CacheRemoveEntry;
import javax.cache.interceptor.CacheResolver;
import javax.cache.interceptor.CacheResult;
import javax.cache.interceptor.CacheValue;
import javax.inject.Inject;
import javax.interceptor.InvocationContext;

/**
 * Utility used by all annotations to lookup the {@link CacheResolver} and {@link CacheKeyGenerator} for a given method.
 * 
 * @author Rick Hightower
 * 
 */
public class RICacheLookupUtil {
    @Inject
    private RIBeanManagerUtil beanManagerUtil;
    
    private CacheKeyGenerator defaultCacheKeyGenerator = new RIDefaultCacheKeyGenerator();
    private CacheResolver defaultCacheResolver = new RIDefaultCacheResolver();
    
    private final ConcurrentMap<Method, MethodDetails> methodDetailsCache = new ConcurrentHashMap<Method, MethodDetails>();

    /**
     * Get the {@link javax.cache.interceptor.CacheInvocationContext} for the CDI {@link InvocationContext}
     * 
     * @param invocationContext The CDI invocation context
     * @return The cache invocation context
     * @throws UnsupportedOperationException if the invocation context is not for a method that has an annotation for which CacheInvocationContext exists.
     */
    public CacheInvocationContextImpl getCacheInvocationContext(InvocationContext invocationContext) {
        final MethodDetails methodDetails = this.getMethodDetails(invocationContext);

        switch (methodDetails.getInterceptorType()) {
            case CACHE_RESULT:
            case CACHE_PUT:
            case CACHE_REMOVE_ENTRY: {
                return new CacheInvocationContextImpl((KeyedMethodDetails)methodDetails, invocationContext);
            }
            default: {
                throw new UnsupportedOperationException(
                        "Cannot get CacheInvocationContextImpl for interceptor type: " + methodDetails.getInterceptorType());
            }
        }
    }
    
    /**
     * Get detailed data about an annotated method for a specific {@link InvocationContext}
     * 
     * @param invocationContext The invocation to get the method details for
     * @return The detailed method data
     * @throws AnnotationFormatError if an invalid combination of annotations exist on the method
     */
    public MethodDetails getMethodDetails(InvocationContext invocationContext) {
        final Method method = invocationContext.getMethod();
        MethodDetails methodDetails = this.methodDetailsCache.get(method);
        if (methodDetails != null) {
            return methodDetails;
        }
        
        final Set<Annotation> methodAnotations = new LinkedHashSet<Annotation>(Arrays.asList(method.getAnnotations()));
        
        //Get the default cache configuration for the class.
        final Object target = invocationContext.getTarget();
        final Class<? extends Object> targetClass = target.getClass();
        final CacheDefaults cacheDefaultsAnnotation = targetClass.getAnnotation(CacheDefaults.class);
        
        //Grab all possible annotations from the method, needed to enforce valid use of the annotations
        final CacheResult cacheResultAnnotation = method.getAnnotation(CacheResult.class);
        final CachePut cachePutAnnotation = method.getAnnotation(CachePut.class);
        final CacheRemoveEntry cacheRemoveEntryAnnotation = method.getAnnotation(CacheRemoveEntry.class);
        final CacheRemoveAll cacheRemoveAllAnnotation = method.getAnnotation(CacheRemoveAll.class);
        
        //Check for no annotations (should never actually happen, but lets be safe and return a good error message if it does)
        if (cacheResultAnnotation == null && cachePutAnnotation == null && cacheRemoveEntryAnnotation == null && cacheRemoveAllAnnotation == null) {
            //TODO is this the right type of exception to throw here?
            throw new AnnotationFormatError("At least one cache related annotation must be specified on " + method);
        } else if (!(cacheResultAnnotation != null ^ cachePutAnnotation != null ^ 
                cacheRemoveEntryAnnotation != null ^ cacheRemoveAllAnnotation != null)) {
            //TODO is this the right type of exception to throw here?
            throw new AnnotationFormatError(
                    "Multiple cache annotations were found on " + method + " only one cache annotation per method is allowed");
        } else if (cacheResultAnnotation != null) {
            //Find the cache
            final Class<? extends CacheResolver> cacheResolverType = cacheResultAnnotation.cacheResolver();
            final String cacheName = cacheResultAnnotation.cacheName();
            final Cache<Object, Object> cache = this.resolveCache(cacheResolverType, cacheDefaultsAnnotation, method, cacheName, target);
            
            //Find the key generator
            final Class<? extends CacheKeyGenerator> cacheKeyGeneratorType = cacheResultAnnotation.cacheKeyGenerator();
            final CacheKeyGenerator cacheKeyGenerator = this.getCacheKeyGenerator(cacheKeyGeneratorType, cacheDefaultsAnnotation);
            
            //Load parameter data, CacheValue is not allowed for CacheResult
            final ParameterDetails parameterDetails = getParameterDetails(method, false);
            
            methodDetails = new CacheResultMethodDetails(
                    cache, methodAnotations, cacheKeyGenerator, 
                    parameterDetails.allParameters, parameterDetails.keyParameters, 
                    cacheResultAnnotation);
        } else if (cachePutAnnotation != null) {
            //Find the cache
            final Class<? extends CacheResolver> cacheResolverType = cachePutAnnotation.cacheResolver();
            final String cacheName = cachePutAnnotation.cacheName();
            final Cache<Object, Object> cache = this.resolveCache(cacheResolverType, cacheDefaultsAnnotation, method, cacheName, target);
            
            //Find the key generator
            final Class<? extends CacheKeyGenerator> cacheKeyGeneratorType = cachePutAnnotation.cacheKeyGenerator();
            final CacheKeyGenerator cacheKeyGenerator = this.getCacheKeyGenerator(cacheKeyGeneratorType, cacheDefaultsAnnotation);
            
            //Load parameter data, CacheValue is not allowed for CacheResult
            final ParameterDetails parameterDetails = getParameterDetails(method, true);
            
            methodDetails = new CachePutMethodDetails(
                    cache, methodAnotations, cacheKeyGenerator, 
                    parameterDetails.allParameters, parameterDetails.keyParameters, 
                    parameterDetails.cacheValueParameter, cachePutAnnotation);
        } else if (cacheRemoveEntryAnnotation != null) {
            //Find the cache
            final Class<? extends CacheResolver> cacheResolverType = cacheRemoveEntryAnnotation.cacheResolver();
            final String cacheName = cacheRemoveEntryAnnotation.cacheName();
            final Cache<Object, Object> cache = this.resolveCache(cacheResolverType, cacheDefaultsAnnotation, method, cacheName, null);
            
            //Find the key generator
            final Class<? extends CacheKeyGenerator> cacheKeyGeneratorType = cacheRemoveEntryAnnotation.cacheKeyGenerator();
            final CacheKeyGenerator cacheKeyGenerator = this.getCacheKeyGenerator(cacheKeyGeneratorType, cacheDefaultsAnnotation);
            
            //Load parameter data, CacheValue is not allowed for CacheResult
            final ParameterDetails parameterDetails = getParameterDetails(method, false);
            
            methodDetails = new CacheRemoveEntryMethodDetails(
                    cache, methodAnotations, cacheKeyGenerator, 
                    parameterDetails.allParameters, parameterDetails.keyParameters, 
                    cacheRemoveEntryAnnotation);
        } else if (cacheRemoveAllAnnotation != null) {
            //Find the cache
            final Class<? extends CacheResolver> cacheResolverType = cacheRemoveAllAnnotation.cacheResolver();
            final String cacheName = cacheRemoveAllAnnotation.cacheName();
            final Cache<Object, Object> cache = this.resolveCache(cacheResolverType, cacheDefaultsAnnotation, method, cacheName, null);
            
            methodDetails = new CacheRemoveAllMethodDetails(
                    cache, methodAnotations, 
                    cacheRemoveAllAnnotation);
        }
        
        final MethodDetails existingMethodDetails = this.methodDetailsCache.putIfAbsent(method, methodDetails);

        //Handle concurrent creation of MethodDetails for this method and only return "the one true object"
        if (existingMethodDetails != null) {
            return existingMethodDetails;
        }
        
        return methodDetails;
    }
    
    
    /**
     * Used to generated parameter details data out of {@link RICacheLookupUtil#getParameterDetails(Method, boolean)}
     */
    private static final class ParameterDetails {
        private final List<CacheParameterDetails> allParameters;
        private final List<CacheParameterDetails> keyParameters;
        private final CacheParameterDetails cacheValueParameter;

        /**
         * Creates a new parameter details object
         * 
         * @param allParameters All method parameters
         * @param keyParameters Method parameters to use for key generation
         * @param cacheValueParameter The value parameter
         */
        private ParameterDetails(
                List<CacheParameterDetails> allParameters, 
                List<CacheParameterDetails> keyParameters,
                CacheParameterDetails cacheValueParameter) {
            this.allParameters = allParameters;
            this.keyParameters = keyParameters;
            this.cacheValueParameter = cacheValueParameter;
        }
    }

    /**
     * Parse the parameters for a Method and create immutable {@link CacheParameterDetails} lists
     * 
     * @param method The method to get parameter detail information for
     * @param cacheValueAllowed If the {@link CacheValue} annotation is legal for this method
     * @return A {@link ParameterDetails} object, the lists it contains must be immutable.
     */
    protected ParameterDetails getParameterDetails(final Method method, final boolean cacheValueAllowed) {
        //Get parameter type and annotation details
        final Class<?>[] parameterTypes = method.getParameterTypes();
        final Annotation[][] parameterAnnotations = method.getParameterAnnotations();
        
        //all and key parameter lists
        final ArrayList<CacheParameterDetails> allParameters = new ArrayList<CacheParameterDetails>(parameterTypes.length);
        final ArrayList<CacheParameterDetails> keyParameters = new ArrayList<CacheParameterDetails>(parameterTypes.length);
        CacheParameterDetails cacheValueParameter = null;
        
        //Step through each parameter
        for (int pIdx = 0; pIdx < parameterTypes.length; pIdx++) {
            final Class<?> rawType = parameterTypes[pIdx];
            
            //Create Set of annotations on Method and check for @CacheKeyParam
            boolean isKey = false;
            boolean isValue = false;
            final Set<Annotation> annotations = new LinkedHashSet<Annotation>();
            for (final Annotation parameterAnnotation : parameterAnnotations[pIdx]) {
                annotations.add(parameterAnnotation);
                if (!isKey && CacheKeyParam.class.isAssignableFrom(parameterAnnotation.annotationType())) {
                    isKey = true;
                } else if (CacheValue.class.isAssignableFrom(parameterAnnotation.annotationType())) {
                    if (!cacheValueAllowed) {
                        throw new AnnotationFormatError("CacheValue parameter annotation is not allowed on " + method);
                    } else if (cacheValueParameter != null || isValue) {
                        throw new AnnotationFormatError("Multiple CacheValue parameter annotations are not allowed: " + method);
                    } else {
                        isValue = true;
                    }
                }
            }
            
            //Create parameter details object
            final CacheParameterDetails cacheParameterDetails = new CacheParameterDetails(
                    rawType.getGenericSuperclass(), rawType, Collections.unmodifiableSet(annotations), pIdx);
            
            //Add parameter details to List and to key list if it is marked as a cache key parameter
            allParameters.add(cacheParameterDetails);
            if (isKey) {
                keyParameters.add(cacheParameterDetails);
            }
            if (isValue) {
                cacheValueParameter = cacheParameterDetails;
            }
        }
        
        //If no parameters were marked as key parameters then they all must be
        if (keyParameters.isEmpty()) {
            keyParameters.addAll(allParameters);
        }

        //Remove the value parameter from the key parameter list 
        if (cacheValueParameter != null) {
            keyParameters.remove(cacheValueParameter);
        }
        
        //reduce memory usage of the key parameter list since it may be smaller than the all parameter list
        keyParameters.trimToSize(); 

        //Make lists unmodifiable and return them
        return new ParameterDetails(Collections.unmodifiableList(allParameters), Collections.unmodifiableList(keyParameters), cacheValueParameter);
    }
    
    /**
     * Get the cache key generator based on the requested type and the defaults
     * 
     * @param cacheKeyGeneratorType Requested key generator type
     * @param cacheDefaultsAnnotation Defaults data
     * @return The key generator to use
     */
    protected CacheKeyGenerator getCacheKeyGenerator(
            Class<? extends CacheKeyGenerator> cacheKeyGeneratorType, CacheDefaults cacheDefaultsAnnotation) {
        
        //If the specified generator type is CacheKeyGenerator then ignore the value as the default 
        if (!CacheKeyGenerator.class.equals(cacheKeyGeneratorType)) {
            return beanManagerUtil.getBeanByType(cacheKeyGeneratorType);                
        }
        
        if (cacheDefaultsAnnotation != null) {
            final Class<? extends CacheKeyGenerator> defaultCacheKeyGeneratorType = cacheDefaultsAnnotation.cacheKeyGenerator();
            //If the specified generator type is CacheKeyGenerator then ignore the value as the default 
            if (!CacheKeyGenerator.class.equals(defaultCacheKeyGeneratorType)) {
                return beanManagerUtil.getBeanByType(defaultCacheKeyGeneratorType);                
            }
        }
        
        return this.defaultCacheKeyGenerator;
    }
    
    /**
     * Get the cache resolver based on the requested type and the defaults.
     * 
     * @param cacheResolverType Requested resolver type
     * @param cacheDefaultsAnnotation Defaults data
     * @return The cache resolver to use
     */
    protected CacheResolver getCacheResolver(Class<? extends CacheResolver> cacheResolverType, CacheDefaults cacheDefaultsAnnotation) {
      //If the specified resolver type is CacheResolver then ignore the value as the default 
        if (!CacheResolver.class.equals(cacheResolverType)) {
            return beanManagerUtil.getBeanByType(cacheResolverType);                
        }
        
        if (cacheDefaultsAnnotation != null) {
            final Class<? extends CacheResolver> defaultCacheResolverType = cacheDefaultsAnnotation.cacheResolver();
            //If the specified resolver type is CacheResolver then ignore the value as the default 
            if (!CacheResolver.class.equals(defaultCacheResolverType)) {
                return beanManagerUtil.getBeanByType(defaultCacheResolverType);                
            }
        }
        
        return this.defaultCacheResolver;
    }
    
    /**
     * Resolve the cache to use
     * 
     * @param cacheDefaultsAnnotation Class level defaults used for finding the {@link CacheResolver} and the default cacheName 
     * @param cacheResolverType Cache resolver to use
     * @param method The annotated method
     * @param cacheName The name of the cache specified in the annotation
     * @param target (Optional) The target of the invocation. If specified and no cacheName or {@link CacheDefaults#cacheName()} are specified a cache name will be generated.
     * @return The {@link Cache} to use
     * @throws AnnotationFormatError If no cacheName or {@link CacheDefaults#cacheName()} are specified and target is null.
     */
    protected Cache<Object, Object> resolveCache(
            Class<? extends CacheResolver> cacheResolverType, CacheDefaults cacheDefaultsAnnotation, 
            Method method, String cacheName, Object target) {
        
        final CacheResolver cacheResolver = this.getCacheResolver(cacheResolverType, cacheDefaultsAnnotation);
        
        if (!"".equals(cacheName)) {
            return cacheResolver.resolveCache(cacheName, method);
        }
        
        final String defaultCacheName = cacheDefaultsAnnotation.cacheName();
        if (!"".equals(cacheName)) {
            return cacheResolver.resolveCache(defaultCacheName, method);
        }
        
        //A target was provided, that implies we should generate the cache name
        if (target != null) {
            final String fqClassName = target.getClass().getName();
            final StringBuilder generatedCacheNameBuilder = new StringBuilder(fqClassName);
            generatedCacheNameBuilder.append(".");
            generatedCacheNameBuilder.append(method.getName());
            generatedCacheNameBuilder.append("(");
            
            final Class<?>[] parameterTypes = method.getParameterTypes();
            for (int pIdx = 0; pIdx < parameterTypes.length; pIdx++) {
                generatedCacheNameBuilder.append(parameterTypes[pIdx].getName());
                if ((pIdx + 1) < parameterTypes.length) {
                    generatedCacheNameBuilder.append(",");
                }
            }
            generatedCacheNameBuilder.append(")");
            
            return cacheResolver.resolveCache(generatedCacheNameBuilder.toString(), method);
        }
        
        throw new AnnotationFormatError("cacheName must be specified in either CacheDefaults or CacheRemoveEntry or CacheRemoveAll"); 
    }
}
