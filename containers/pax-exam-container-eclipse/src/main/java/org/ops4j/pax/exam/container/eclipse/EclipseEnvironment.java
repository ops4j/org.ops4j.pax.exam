/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.exam.container.eclipse;

import org.osgi.framework.Filter;

/**
 * Defines the system a particular software product runs on
 * 
 * @author Christoph Läubrich
 *
 */
public interface EclipseEnvironment {

    boolean matches(String value, String... keys);

    boolean matches(Filter filter);

    ModifiableEclipseEnvironment copy();

    public static interface ModifiableEclipseEnvironment extends EclipseEnvironment {

        void set(String key, String value);
    }

}
