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
package javax.cache.annotation.impl;

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

import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheKeyParam;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CachePut;
import javax.cache.annotation.CacheRemoveAll;
import javax.cache.annotation.CacheRemoveEntry;
import javax.cache.annotation.CacheResolver;
import javax.cache.annotation.CacheResolverFactory;
import javax.cache.annotation.CacheResult;
import javax.cache.annotation.CacheValue;

/**
 * Utility used by all annotations to lookup the {@link CacheResolver} and {@link CacheKeyGenerator} for a given method.
 * 
 * @author Rick Hightower
 * @author Eric Dalquist
 * @param <I> The intercepted method invocation
 * @since 1.0
 */
public abstract class AbstractCacheLookupUtil<I> implements CacheContextSource<I> {
    private final ConcurrentMap<MethodKey, StaticCacheInvocationContext<? extends Annotation>> methodDetailsCache = 
            new ConcurrentHashMap<MethodKey, StaticCacheInvocationContext<? extends Annotation>>();
    
    /**
     * Create lookup utility
     */
    public AbstractCacheLookupUtil() {
        //Mark that annotations have been initialized
        AnnotationProviderImpl.setAnnotationsInitialized();
    }

    /**
     * Get the {@link AbstractInternalCacheKeyInvocationContext} for the CDI invocation
     * 
     * @param invocation The CDI invocation context
     * @return The keyed cache invocation context
     * @throws UnsupportedOperationException if the invocation context is not for a method that has an annotation for which CacheInvocationContext exists.
     */
    @Override
    public InternalCacheKeyInvocationContext<? extends Annotation> getCacheKeyInvocationContext(I invocation) {
        final Method method = this.getMethod(invocation);
        final Class<?> targetClass = this.getTargetClass(invocation);
        final StaticCacheInvocationContext<? extends Annotation> staticCacheInvocationContext = this.getMethodDetails(method, targetClass);
        if (staticCacheInvocationContext == null) {
            throw new AnnotationFormatError("At least one cache related annotation must be specified on " + method + 
                    " for intercepted invocation to be valid: " + invocation);
        }

        switch (staticCacheInvocationContext.getInterceptorType()) {
            case CACHE_RESULT:
            case CACHE_PUT:
            case CACHE_REMOVE_ENTRY: {
                return createCacheKeyInvocationContextImpl(
                        (StaticCacheKeyInvocationContext<? extends Annotation>)staticCacheInvocationContext, invocation);
            }
            default: {
                throw new UnsupportedOperationException(
                        "Cannot get AbstractInternalCacheKeyInvocationContext for interceptor type: " + 
                        staticCacheInvocationContext.getInterceptorType());
            }
        }
    }

    /**
     * Create the cache key invocation context for the provided static context and intercepted method invocation
     * 
     * @param staticCacheKeyInvocationContext The static  key context information about the method
     * @param invocation The intercepted method invocation
     * @return The cache invocation context
     */
    protected abstract InternalCacheKeyInvocationContext<? extends Annotation> createCacheKeyInvocationContextImpl(
            StaticCacheKeyInvocationContext<? extends Annotation> staticCacheKeyInvocationContext, I invocation);

    /**
     * Get the {@link AbstractInternalCacheInvocationContext} for the invocation
     * 
     * @param invocation The CDI invocation context
     * @return The cache invocation context
     */
    @Override
    public InternalCacheInvocationContext<? extends Annotation> getCacheInvocationContext(I invocation) {
        final Method method = this.getMethod(invocation);
        final Class<?> targetClass = this.getTargetClass(invocation);
        final StaticCacheInvocationContext<? extends Annotation> staticCacheInvocationContext = this.getMethodDetails(method, targetClass);
        if (staticCacheInvocationContext == null) {
            throw new AnnotationFormatError("At least one cache related annotation must be specified on " + method + 
                    " for intercepted invocation to be valid: " + invocation);
        }
        return createCacheInvocationContextImpl(staticCacheInvocationContext, invocation);
    }
    
    /**
     * Create the cache invocation context for the provided static context and intercepted method invocation
     * 
     * @param staticCacheInvocationContext The static context information about the method
     * @param invocation The intercepted method invocation
     * @return The cache invocation context
     */
    protected abstract InternalCacheInvocationContext<? extends Annotation> createCacheInvocationContextImpl(
            StaticCacheInvocationContext<? extends Annotation> staticCacheInvocationContext, I invocation);
    
    /**
     * Get the concrete annotation object for the method which will be invoked on the target class.
     * <p/>
     * Subclasses may override this to provide their own annotation resolution logic, the default implementation
     * uses {@link Method#getAnnotation(Class)}
     * 
     * @param annotationClass The annotation class
     * @param method Target method
     * @param targetClass Target Class
     * @return The concrete annotation from the method
     */
    protected <T extends Annotation> T getAnnotation(Class<T> annotationClass, Method method, Class<? extends Object> targetClass) {
        return method.getAnnotation(annotationClass);
    }
    
    /**
     * Get detailed data about an annotated method for a specific targeted class
     * 
     * @param method The method that to get details for
     * @param targetClass The class that is being targeted with the invocation
     * @return The detailed method data
     * @throws AnnotationFormatError if an invalid combination of annotations exist on the method
     */
    @Override
    public StaticCacheInvocationContext<? extends Annotation> getMethodDetails(Method method, Class<? extends Object> targetClass) {
        final MethodKey methodKey = new MethodKey(method, targetClass);
        
        StaticCacheInvocationContext<? extends Annotation> staticCacheInvocationContext = this.methodDetailsCache.get(methodKey);
        if (staticCacheInvocationContext != null) {
            return staticCacheInvocationContext;
        }
        
        final CacheDefaults cacheDefaultsAnnotation = targetClass.getAnnotation(CacheDefaults.class);
        
        //Grab all possible annotations from the method, needed to enforce valid use of the annotations
        final CacheResult cacheResultAnnotation = getAnnotation(CacheResult.class, method, targetClass);
        final CachePut cachePutAnnotation = getAnnotation(CachePut.class, method, targetClass);
        final CacheRemoveEntry cacheRemoveEntryAnnotation = getAnnotation(CacheRemoveEntry.class, method, targetClass);
        final CacheRemoveAll cacheRemoveAllAnnotation = getAnnotation(CacheRemoveAll.class, method, targetClass);
        
        if (cacheResultAnnotation == null && cachePutAnnotation == null && cacheRemoveEntryAnnotation == null && cacheRemoveAllAnnotation == null) {
            //Check for no annotations, just ignore the method
            return null;
        } else if (!(cacheResultAnnotation != null ^ cachePutAnnotation != null ^ 
                cacheRemoveEntryAnnotation != null ^ cacheRemoveAllAnnotation != null)) {
            //Check for more than one caching annotation
            throw new AnnotationFormatError(
                    "Multiple cache annotations were found on " + method + " only one cache annotation per method is allowed");
        } else if (cacheResultAnnotation != null) {
            staticCacheInvocationContext = 
                    this.createCacheResultMethodDetails(cacheResultAnnotation, cacheDefaultsAnnotation, method, targetClass);
        } else if (cachePutAnnotation != null) {
            staticCacheInvocationContext = 
                    this.createCachePutMethodDetails(cachePutAnnotation, cacheDefaultsAnnotation, method, targetClass);
        } else if (cacheRemoveEntryAnnotation != null) {
            staticCacheInvocationContext = 
                    this.createCacheRemoveEntryMethodDetails(cacheRemoveEntryAnnotation, cacheDefaultsAnnotation, method, targetClass);
        } else if (cacheRemoveAllAnnotation != null) {
            staticCacheInvocationContext = 
                    this.createCacheRemoveAllMethodDetails(cacheRemoveAllAnnotation, cacheDefaultsAnnotation, method, targetClass);
        } else {
            //This should not be possible
            return null;
        }

        //Cache the resolved information
        final StaticCacheInvocationContext<? extends Annotation> existingMethodDetails = 
                this.methodDetailsCache.putIfAbsent(methodKey, staticCacheInvocationContext);

        //Handle concurrent creation of MethodDetails for this method and only return "the one true object"
        if (existingMethodDetails != null) {
            return existingMethodDetails;
        }
        
        return staticCacheInvocationContext;
    }

    /**
     * @param invocation
     */
    protected abstract Class<?> getTargetClass(I invocation);

    /**
     * @param invocation
     */
    protected abstract Method getMethod(I invocation);

    /**
     * Create CacheMethodDetails to describe the annotated method
     * 
     * @param cacheAnnotation The annotation
     * @param cacheDefaultsAnnotation The class level defaults annotation
     * @param method The annotated method
     * @param targetClass The intercepted class
     * @return The cache method details
     */
    protected <A extends Annotation> CacheMethodDetails<A> createCacheMethodDetails(
            A cacheAnnotation, CacheDefaults cacheDefaultsAnnotation, 
            String methodCacheName, Method method, Class<? extends Object> targetClass) {
        final String cacheName = this.resolveCacheName(methodCacheName, cacheDefaultsAnnotation, method, targetClass);

        //Create immutable Set of all annotations on the method
        final Set<Annotation> methodAnotations = this.getMethodAnnotations(method);
        
        //Create the method details instance
        return new CacheMethodDetailsImpl<A>(method, methodAnotations, cacheAnnotation, cacheName);
    }
    
    /**
     * Create a StaticCacheInvocationContext implementation specific to the {@link CacheResult} annotated method
     * 
     * @param cacheResultAnnotation The annotation on the method
     * @param cacheDefaultsAnnotation The defaults annotation for the class, if it exists
     * @param method The annotated method
     * @param targetClass The intercepted class
     * @return Details on the annotated method
     */
    protected CacheResultMethodDetails createCacheResultMethodDetails(
            CacheResult cacheResultAnnotation, CacheDefaults cacheDefaultsAnnotation,
            Method method, Class<? extends Object> targetClass) {
        
        final String methodCacheName = cacheResultAnnotation.cacheName();
        
        //Determine the name of the cache
        final CacheMethodDetails<CacheResult> cacheMethodDetails = 
                createCacheMethodDetails(cacheResultAnnotation, cacheDefaultsAnnotation, methodCacheName, method, targetClass);
        
        //Find the cache resolver factory
        final Class<? extends CacheResolverFactory> cacheResolverFactoryType = cacheResultAnnotation.cacheResolverFactory();
        final CacheResolverFactory cacheResolverFactory = 
                this.getCacheResolverFactory(cacheResolverFactoryType, cacheDefaultsAnnotation);
        
        //Find the key generator
        final Class<? extends CacheKeyGenerator> cacheKeyGeneratorType = cacheResultAnnotation.cacheKeyGenerator();
        final CacheKeyGenerator cacheKeyGenerator = this.getCacheKeyGenerator(cacheKeyGeneratorType, cacheDefaultsAnnotation);
        
        //Load parameter data, CacheValue is not allowed for CacheResult
        final ParameterDetails parameterDetails = this.getParameterDetails(method, false);

        //Get the cache resolver to use for the method
        final CacheResolver cacheResolver = cacheResolverFactory.getCacheResolver(cacheMethodDetails);

        //Get the exception cache resolver to use for the method, if an exceptionCacheName is set
        final CacheResolver exceptionCacheResolver;
        final String exceptionCacheName = cacheResultAnnotation.exceptionCacheName();
        if (exceptionCacheName != null && exceptionCacheName.trim().length() != 0) {
            exceptionCacheResolver = cacheResolverFactory.getExceptionCacheResolver(cacheMethodDetails);
        } else {
            exceptionCacheResolver = null;
        }
        
        return new CacheResultMethodDetails(cacheMethodDetails, 
                cacheResolver, exceptionCacheResolver, 
                cacheKeyGenerator, 
                parameterDetails.allParameters, parameterDetails.keyParameters);
    }
    
    /**
     * Create a StaticCacheInvocationContext implementation specific to the {@link CachePut} annotated method
     * 
     * @param cachePutAnnotation The annotation on the method
     * @param cacheDefaultsAnnotation The defaults annotation for the class, if it exists
     * @param method The annotated method
     * @param targetClass The intercepted class
     * @return Details on the annotated method
     */
    protected CachePutMethodDetails createCachePutMethodDetails(
            CachePut cachePutAnnotation, CacheDefaults cacheDefaultsAnnotation,
            Method method, Class<? extends Object> targetClass) {
        
        //Determine the name of the cache
        final String methodCacheName = cachePutAnnotation.cacheName();
        
        //Create the method details instance
        final CacheMethodDetails<CachePut> cacheMethodDetails = 
                createCacheMethodDetails(cachePutAnnotation, cacheDefaultsAnnotation, methodCacheName, method, targetClass);
        
        //Find the cache resolver factory
        final Class<? extends CacheResolverFactory> cacheResolverFactoryType = cachePutAnnotation.cacheResolverFactory();
        final CacheResolverFactory cacheResolverFactory = this.getCacheResolverFactory(cacheResolverFactoryType, cacheDefaultsAnnotation);
        
        //Find the key generator
        final Class<? extends CacheKeyGenerator> cacheKeyGeneratorType = cachePutAnnotation.cacheKeyGenerator();
        final CacheKeyGenerator cacheKeyGenerator = this.getCacheKeyGenerator(cacheKeyGeneratorType, cacheDefaultsAnnotation);
        
        //Load parameter data, CacheValue is not allowed for CacheResult
        final ParameterDetails parameterDetails = getParameterDetails(method, false);

        //Get the cache resolver to use for the method
        final CacheResolver cacheResolver = cacheResolverFactory.getCacheResolver(cacheMethodDetails);
        
        return new CachePutMethodDetails(cacheMethodDetails, 
                cacheResolver, cacheKeyGenerator, 
                parameterDetails.allParameters, parameterDetails.keyParameters,
                parameterDetails.cacheValueParameter);
    }
    
    /**
     * Create a StaticCacheInvocationContext implementation specific to the {@link CacheRemoveEntry} annotated method
     * 
     * @param cacheRemoveEntryAnnotation The annotation on the method
     * @param cacheDefaultsAnnotation The defaults annotation for the class, if it exists
     * @param method The annotated method
     * @param targetClass The intercepted class
     * @return Details on the annotated method
     */
    protected CacheRemoveEntryMethodDetails createCacheRemoveEntryMethodDetails(
            CacheRemoveEntry cacheRemoveEntryAnnotation, CacheDefaults cacheDefaultsAnnotation,
            Method method, Class<? extends Object> targetClass) {
        
        //Determine the name of the cache
        final String methodCacheName = cacheRemoveEntryAnnotation.cacheName();
        
        //Create the method details instance
        final CacheMethodDetails<CacheRemoveEntry> cacheMethodDetails = 
                createCacheMethodDetails(cacheRemoveEntryAnnotation, cacheDefaultsAnnotation, methodCacheName, method, targetClass);
        
        //Find the cache resolver factory
        final Class<? extends CacheResolverFactory> cacheResolverFactoryType = cacheRemoveEntryAnnotation.cacheResolverFactory();
        final CacheResolverFactory cacheResolverFactory = this.getCacheResolverFactory(cacheResolverFactoryType, cacheDefaultsAnnotation);
        
        //Find the key generator
        final Class<? extends CacheKeyGenerator> cacheKeyGeneratorType = cacheRemoveEntryAnnotation.cacheKeyGenerator();
        final CacheKeyGenerator cacheKeyGenerator = this.getCacheKeyGenerator(cacheKeyGeneratorType, cacheDefaultsAnnotation);
        
        //Load parameter data, CacheValue is not allowed for CacheResult
        final ParameterDetails parameterDetails = getParameterDetails(method, false);

        //Get the cache resolver to use for the method
        final CacheResolver cacheResolver = cacheResolverFactory.getCacheResolver(cacheMethodDetails);
        
        return new CacheRemoveEntryMethodDetails(cacheMethodDetails, 
                cacheResolver, cacheKeyGenerator, 
                parameterDetails.allParameters, parameterDetails.keyParameters);
    }
    
    /**
     * Create a StaticCacheInvocationContext implementation specific to the {@link CacheRemoveAll} annotated method
     * 
     * @param cacheRemoveAllAnnotation The annotation on the method
     * @param cacheDefaultsAnnotation The defaults annotation for the class, if it exists
     * @param method The annotated method
     * @param targetClass The intercepted class
     * @return Details on the annotated method
     */
    protected CacheRemoveAllMethodDetails createCacheRemoveAllMethodDetails(
            CacheRemoveAll cacheRemoveAllAnnotation, CacheDefaults cacheDefaultsAnnotation,
            Method method, Class<? extends Object> targetClass) {
        
        //Determine the name of the cache
        final String methodCacheName = cacheRemoveAllAnnotation.cacheName();
        
        //Create the method details instance
        final CacheMethodDetails<CacheRemoveAll> cacheMethodDetails = 
                createCacheMethodDetails(cacheRemoveAllAnnotation, cacheDefaultsAnnotation, methodCacheName, method, targetClass);

        //Find the cache resolver factory
        final Class<? extends CacheResolverFactory> cacheResolverFactoryType = cacheRemoveAllAnnotation.cacheResolverFactory();
        final CacheResolverFactory cacheResolverFactory = this.getCacheResolverFactory(cacheResolverFactoryType, cacheDefaultsAnnotation);
        
        final ParameterDetails parameterDetails = getParameterDetails(method, false);

        //Get the cache resolver to use for the method
        final CacheResolver cacheResolver = cacheResolverFactory.getCacheResolver(cacheMethodDetails);
        
        return new CacheRemoveAllMethodDetails(cacheMethodDetails, 
                cacheResolver, parameterDetails.allParameters);
    }

    /**
     * Get an immutable set of all annotations on the method
     */
    protected Set<Annotation> getMethodAnnotations(Method method) {
        return Collections.unmodifiableSet(new LinkedHashSet<Annotation>(Arrays.asList(method.getAnnotations())));
    }
    
    /**
     * Used to generated parameter details data out of {@link AbstractCacheLookupUtil#getParameterDetails(Method, boolean)}
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
     * @return a ParameterDetails object, the lists it contains must be immutable.
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
                    rawType, Collections.unmodifiableSet(annotations), pIdx);
            
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
     * Used to load {@link CacheKeyGenerator} and {@link CacheResolverFactory} instances by type
     */
    protected abstract <T> T getObjectByType(Class<T> type);
    
    /**
     * @return The default CacheKeyGenerator implementation to use 
     */
    protected abstract CacheKeyGenerator getDefaultCacheKeyGenerator();
    
    /**
     * @return The default CacheKeyGenerator implementation to use
     */
    protected abstract CacheResolverFactory getDefaultCacheResolverFactory();

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
            return getObjectByType(cacheKeyGeneratorType);                
        }
        
        if (cacheDefaultsAnnotation != null) {
            final Class<? extends CacheKeyGenerator> defaultCacheKeyGeneratorType = cacheDefaultsAnnotation.cacheKeyGenerator();
            //If the specified generator type is CacheKeyGenerator then ignore the value as the default 
            if (!CacheKeyGenerator.class.equals(defaultCacheKeyGeneratorType)) {
                return getObjectByType(defaultCacheKeyGeneratorType);                
            }
        }
        
        return this.getDefaultCacheKeyGenerator();
    }
    
    /**
     * Get the cache resolver factory based on the requested type and the defaults.
     * 
     * @param cacheResolverFactoryType Requested resolver factory type
     * @param cacheDefaultsAnnotation Defaults data
     * @return The cache resolver to use
     */
    protected CacheResolverFactory getCacheResolverFactory(Class<? extends CacheResolverFactory> cacheResolverFactoryType, 
            CacheDefaults cacheDefaultsAnnotation) {
        
        //If the specified resolver type is CacheResolverFactory then ignore the value as the default 
        if (!CacheResolverFactory.class.equals(cacheResolverFactoryType)) {
            return getObjectByType(cacheResolverFactoryType);                
        }
        
        if (cacheDefaultsAnnotation != null) {
            final Class<? extends CacheResolverFactory> defaultCacheResolverType = cacheDefaultsAnnotation.cacheResolverFactory();
            //If the specified resolver type is CacheResolverFactory then ignore the value as the default 
            if (!CacheResolverFactory.class.equals(defaultCacheResolverType)) {
                return getObjectByType(defaultCacheResolverType);                
            }
        }
        
        return this.getDefaultCacheResolverFactory();
    }
    
    /**
     * Determine the cache name to use based on the method and class level annotations
     * 
     * @param methodCacheName The cache name specified by the method level annotation
     * @param cacheDefaultsAnnotation The class level cache defaults
     * @param method The annotated method
     * @param targetClass The target class, if not null a default cache name will be generated if no name is specified
     * @return The resolved cache name
     * @throws AnnotationFormatError If target is null and no cache name is specified in the method or class level annotations
     */
    protected String resolveCacheName(String methodCacheName, CacheDefaults cacheDefaultsAnnotation, 
            Method method, Class<? extends Object> targetClass) {
        
        if (!"".equals(methodCacheName)) {
            return methodCacheName;
        }
        
        if (cacheDefaultsAnnotation != null) {
            final String defaultCacheName = cacheDefaultsAnnotation.cacheName();
            if (!"".equals(defaultCacheName)) {
                return defaultCacheName;
            }
        }
        
        //A target was provided, that implies we should generate the cache name
        if (targetClass != null) {
            final String fqClassName = method.getDeclaringClass().getName();
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
            
            return generatedCacheNameBuilder.toString();
        }
        
        throw new AnnotationFormatError("cacheName must be specified in either CacheDefaults or CacheRemoveEntry or CacheRemoveAll for: " + method);
    }
    
    /**
     * Defines a cache key based on the combination of a {@link Method} and {@link Class}.
     */
    private static class MethodKey {
        private final Method method;
        private final Class<?> targetClass;
        private final int hashCode;

        public MethodKey(Method method, Class<?> targetClass) {
            this.method = method;
            this.targetClass = targetClass;
            this.hashCode = this.method.hashCode() * 29 + (this.targetClass != null ? this.targetClass.hashCode() : 0);
        }

        /* (non-Javadoc)
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return this.hashCode;
        }

        /* (non-Javadoc)
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            MethodKey other = (MethodKey) obj;
            if (this.method == null) {
                if (other.method != null)
                    return false;
            } else if (!this.method.equals(other.method))
                return false;
            if (this.targetClass == null) {
                if (other.targetClass != null)
                    return false;
            } else if (!this.targetClass.equals(other.targetClass))
                return false;
            return true;
        }
    }
}
