/**
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package javax.cache.annotation.impl;

/**
 * Utility which matches an object's type against a list of included and excluded classes.
 * 
 * @author Eric Dalquist
 * @version $Revision$
 */
public final class ClassFilter {
    private ClassFilter() {
    }
    
    /**
     * Determines if a candidate object's type passes a set of included/excluded filters
     * <p/>
     * Returns true of one of the following conditions is true
     * <ol>
     *  <li>If included and excluded are both empty then the value passed as includeBothEmpty is returned</li>
     *  <li>If included is not empty and excluded is empty and candidate is an instanceof a member of the included array</li>
     *  <li>If included is empty and excluded is not empty and candidate is not an instanceof a member of the excluded array</li>
     *  <li>If included and excluded are not empty and candidate is an instanceof a member of the included array and 
     *      candidate is not an instanceof a member of the excluded array</li>
     * </ol>
     * 
     * @param candidate The object to check if it is included or excluded
     * @param included Array of included classes, may be null
     * @param excluded Array of excluded classes, may be null
     * @param includeBothEmpty If true then if both the included and excluded arrays are null true will be returned.
     */
    public static <T> boolean isIncluded(T candidate, Class<? extends T>[] included, Class<? extends T>[] excluded, boolean includeBothEmpty) {
        if (candidate == null) {
            throw new IllegalArgumentException("candidate can not be null");
        }
        
        final boolean includedEmpty = included == null || included.length == 0;
        final boolean excludedEmpty = excluded == null || excluded.length == 0;
        if (includedEmpty && excludedEmpty) {
            return includeBothEmpty;
        }
        
        final boolean matchesInclude = matches(candidate, included);
        final boolean matchesExclude = matches(candidate, excluded);
        
        if (includedEmpty) {
            return !matchesExclude;
        }
        
        if (excludedEmpty) {
            return matchesInclude;
        }
        
        return matchesInclude && !matchesExclude;
    }
    
    /**
     * Determines if a candidate object's type matches an element in the classes array. 
     * 
     * @param candidate The object to check if its type matches one of the classes in the array, must not be null.
     * @param classes List of classes to check against, may be null.
     * @return null if classes array is null or if candidate is not an instanceof any member of the classes array.
     */
    public static <T> boolean matches(T candidate, Class<? extends T>[] classes) {
        if (classes == null) {
            return false;
        }
        
        final Class<? extends Object> candidateClass = candidate.getClass();
        for (final Class<? extends T> throwable : classes) {
            if (throwable.isAssignableFrom(candidateClass)) {
                return true;
            }
        }
        
        return false;
    }
}
