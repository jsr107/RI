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
 * Defines the available cache interceptor types
 * 
 * @author Eric Dalquist
 * @version $Revision$
 */
public enum InterceptorType {
    /**
     * @see javax.cache.annotation.CacheResult
     */
    CACHE_RESULT,
    /**
     * @see javax.cache.annotation.CachePut
     */
    CACHE_PUT,
    /**
     * @see javax.cache.annotation.CacheRemoveEntry
     */
    CACHE_REMOVE_ENTRY,
    /**
     * @see javax.cache.annotation.CacheRemoveAll
     */
    CACHE_REMOVE_ALL;
}
