/*
 * Copyright (C) 2011 Harald Wellmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.exam.regression.multi.junit;

import org.junit.After;
import org.junit.Before;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.util.PathUtils;

import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.url;
import static org.ops4j.pax.exam.regression.multi.RegressionConfiguration.regressionDefaults;

public class BeforeAfterParent {

    @Configuration
    public Option[] config() {
        return options(
            regressionDefaults(),
            url(String.format("reference:file:%s/target/pax-exam-sample9-pde.jar", PathUtils.getBaseDir())),
            junitBundles(),
            mavenBundle().groupId("com.h2database").artifactId("h2-mvstore").versionAsInProject()
        );
    }

    @Before
    public void before() {
        Messages.addMessage("Before in parent");
    }

    @After
    public void after() {
        Messages.addMessage("After in parent");
    }

}
