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
package org.ops4j.pax.exam.karaf.container.internal;

import java.io.File;

public class KarafCfgFileTest extends KarafConfigurationFileTest {

    @Override
    protected KarafConfigurationFile newKarafConfigurationFile(File karafHome, String location) {
        return new KarafCfgFile(karafHome, location + ".cfg");
    }

    @Override
    protected String[] toStrings(Object strings) {
        return ((String)strings).split(",");
    }
}