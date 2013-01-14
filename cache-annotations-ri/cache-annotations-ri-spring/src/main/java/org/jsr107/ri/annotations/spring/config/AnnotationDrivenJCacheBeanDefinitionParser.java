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
package org.jsr107.ri.annotations.spring.config;

import org.jsr107.ri.annotations.AbstractCacheInterceptor;
import org.jsr107.ri.annotations.DefaultCacheKeyGenerator;
import org.jsr107.ri.annotations.DefaultCacheResolverFactory;
import org.jsr107.ri.annotations.spring.CacheContextSourceImpl;
import org.jsr107.ri.annotations.spring.CachePutInterceptor;
import org.jsr107.ri.annotations.spring.CacheRemoveAllInterceptor;
import org.jsr107.ri.annotations.spring.CacheRemoveEntryInterceptor;
import org.jsr107.ri.annotations.spring.CacheResultInterceptor;
import org.springframework.aop.config.AopNamespaceUtils;
import org.springframework.aop.support.DefaultBeanFactoryPointcutAdvisor;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.beans.factory.xml.XmlReaderContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Class that handles the parsing the custom jcache namespace elements in a spring bean definition file
 * 
 * @author Eric Dalquist
 */
public class AnnotationDrivenJCacheBeanDefinitionParser implements BeanDefinitionParser {
    private static final String XSD_ATTR_CACHE_MANAGER = "cache-manager";

    private static final String JCACHE_CACHE_OPERATION_SOURCE_BEAN_NAME = AnnotationDrivenJCacheBeanDefinitionParser.class
            .getPackage().getName() + ".internalJCacheOperationSourceAdvisor";
    
    /* (non-Javadoc)
     * @see org.springframework.beans.factory.xml.BeanDefinitionParser#parse(org.w3c.dom.Element, org.springframework.beans.factory.xml.ParserContext)
     */
    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        AopNamespaceUtils.registerAutoProxyCreatorIfNecessary(parserContext, element);
        
        final BeanDefinitionRegistry registry = parserContext.getRegistry();
        if (!registry.containsBeanDefinition(JCACHE_CACHE_OPERATION_SOURCE_BEAN_NAME)) {
            final Object elementSource = parserContext.extractSource(element);

            final RuntimeBeanReference cacheOperationSourceReference = this.setupCacheOperationSource(element,
                    parserContext,
                    elementSource);

            
            this.setupPointcutAdvisor(CacheResultInterceptor.class,
                    element, parserContext, elementSource, cacheOperationSourceReference);
            
            this.setupPointcutAdvisor(CachePutInterceptor.class,
                    element, parserContext, elementSource, cacheOperationSourceReference);
            
            this.setupPointcutAdvisor(CacheRemoveEntryInterceptor.class,
                    element, parserContext, elementSource, cacheOperationSourceReference);

            this.setupPointcutAdvisor(CacheRemoveAllInterceptor.class,
                    element, parserContext, elementSource, cacheOperationSourceReference);

            return registry.getBeanDefinition(JCACHE_CACHE_OPERATION_SOURCE_BEAN_NAME);
        }
        
        return null;
    }

    /**
     * Create a {@link CacheContextSourceImpl} bean that will be used by the advisor and interceptor
     * 
     * @return Reference to the {@link CacheContextSourceImpl}. Should never be null.
     */
    protected RuntimeBeanReference setupCacheOperationSource(Element element, ParserContext parserContext,
            Object elementSource) {

        final RootBeanDefinition cacheAttributeSource = new RootBeanDefinition(CacheContextSourceImpl.class);
        cacheAttributeSource.setSource(elementSource);
        cacheAttributeSource.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        
        final RootBeanDefinition defaultCacheResolverFactory = new RootBeanDefinition(DefaultCacheResolverFactory.class);
        cacheAttributeSource.setSource(elementSource);
        cacheAttributeSource.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        final String cacheManagerName = element.getAttribute(XSD_ATTR_CACHE_MANAGER);
        if (StringUtils.hasText(cacheManagerName)) {
            final RuntimeBeanReference cacheManagerReference = new RuntimeBeanReference(cacheManagerName);
            
            final ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();
            constructorArgumentValues.addIndexedArgumentValue(0, cacheManagerReference);
            cacheAttributeSource.setConstructorArgumentValues(constructorArgumentValues);
            
        }
        
        final RootBeanDefinition defaultCacheKeyGenerator = new RootBeanDefinition(DefaultCacheKeyGenerator.class);
        cacheAttributeSource.setSource(elementSource);
        cacheAttributeSource.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

        final MutablePropertyValues propertyValues = cacheAttributeSource.getPropertyValues();
        propertyValues.addPropertyValue("defaultCacheKeyGenerator", defaultCacheKeyGenerator);
        propertyValues.addPropertyValue("defaultCacheResolverFactory", defaultCacheResolverFactory);

        final BeanDefinitionRegistry registry = parserContext.getRegistry();
        registry.registerBeanDefinition(JCACHE_CACHE_OPERATION_SOURCE_BEAN_NAME, cacheAttributeSource);
        
        return new RuntimeBeanReference(JCACHE_CACHE_OPERATION_SOURCE_BEAN_NAME);
    }

    /**
     * Create the {@link Pointcut} used to apply the caching interceptor
     * 
     * @return Reference to the {@link Pointcut}. Should never be null.
     */
    protected RuntimeBeanReference setupPointcut(ParserContext parserContext, Object elementSource,
            RuntimeBeanReference cacheOperationSourceRuntimeReference, RuntimeBeanReference cacheInterceptorSourceRuntimeReference) {
        
        final RootBeanDefinition pointcut = new RootBeanDefinition(CacheStaticMethodMatcherPointcut.class);
        pointcut.setSource(elementSource);
        pointcut.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        
        final ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();
        constructorArgumentValues.addIndexedArgumentValue(0, cacheOperationSourceRuntimeReference);
        constructorArgumentValues.addIndexedArgumentValue(1, cacheInterceptorSourceRuntimeReference);
        pointcut.setConstructorArgumentValues(constructorArgumentValues);

        final String pointcutBeanName = pointcut.getBeanClassName() + "_" + cacheInterceptorSourceRuntimeReference.getBeanName();
        
        final BeanDefinitionRegistry registry = parserContext.getRegistry();
        registry.registerBeanDefinition(pointcutBeanName, pointcut);
        
        return new RuntimeBeanReference(pointcutBeanName);
    }

    /**
     * Create {@link org.aopalliance.intercept.MethodInterceptor} that is applies the caching logic to advised methods.
     * 
     * @return Reference to the {@link org.aopalliance.intercept.MethodInterceptor}. Should never be null.
     */
    protected RuntimeBeanReference setupInterceptor(Class<? extends AbstractCacheInterceptor<?>> interceptorClass, 
            ParserContext parserContext, Object elementSource,
            RuntimeBeanReference cacheOperationSourceRuntimeReference) {

        final RootBeanDefinition interceptor = new RootBeanDefinition(interceptorClass);
        interceptor.setSource(elementSource);
        interceptor.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
        
        final ConstructorArgumentValues constructorArgumentValues = new ConstructorArgumentValues();
        constructorArgumentValues.addIndexedArgumentValue(0, cacheOperationSourceRuntimeReference);
        interceptor.setConstructorArgumentValues(constructorArgumentValues);

        final XmlReaderContext readerContext = parserContext.getReaderContext();
        final String interceptorBeanName = readerContext.registerWithGeneratedName(interceptor);
        return new RuntimeBeanReference(interceptorBeanName);
    }

    /**
     * Create {@link org.springframework.aop.PointcutAdvisor} that puts the {@link org.springframework.aop.Pointcut} and {@link MethodInterceptor} together.
     */
    protected void setupPointcutAdvisor(Class<? extends AbstractCacheInterceptor<?>> interceptorClass,
            Element element, ParserContext parserContext,
            Object elementSource, RuntimeBeanReference cacheOperationSourceReference) {
        
        final RuntimeBeanReference interceptorReference = 
                this.setupInterceptor(interceptorClass, parserContext, elementSource, cacheOperationSourceReference);
        
        final RuntimeBeanReference pointcutReference = 
                this.setupPointcut(parserContext, elementSource, cacheOperationSourceReference, interceptorReference);
        
        
        final RootBeanDefinition pointcutAdvisor = new RootBeanDefinition(DefaultBeanFactoryPointcutAdvisor.class);
        pointcutAdvisor.setSource(elementSource);
        pointcutAdvisor.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

        final MutablePropertyValues propertyValues = pointcutAdvisor.getPropertyValues();
        propertyValues.addPropertyValue("adviceBeanName", interceptorReference.getBeanName());
        propertyValues.addPropertyValue("pointcut", pointcutReference);
        if (element.hasAttribute("order")) {
            propertyValues.addPropertyValue("order", element.getAttribute("order"));
        }
        
        final XmlReaderContext readerContext = parserContext.getReaderContext();
        readerContext.registerWithGeneratedName(pointcutAdvisor);
    }
}
