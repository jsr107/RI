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

/**
 This package contains the reference implementation for JSR107.
 <p/>
 This is meant to act as a proof of concept for the API. It is not threadsafe or high performance. It therefore is
 not suitable for use in production. Please use a production implementation of the API.
 <p/>
 This implementation implements all optional parts of JSR107 except for the Transactions chapter. Transactions support
 simply uses the JTA API. The JSR107 specification details how JTA should be applied to caches.

 @author Greg Luck
 @author Yannis Cosmadopoulos
 */
package org.jsr107.ri;
