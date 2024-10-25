/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.exam.karaf.container;

import java.io.File;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.ConfigurationManager;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;

@RunWith(PaxExam.class)
public abstract class AbstractKarafTestContainerIT {

    protected static final File UNPACK_DIRECTORY = new File("target/paxexam/unpack/");

    protected static final MavenArtifactUrlReference KARAF_URL = maven("org.apache.karaf", "apache-karaf").type("zip");

    @Inject
    private BundleContext bundleContext;

    @Configuration
    public Option[] config() {
        final String karafVersion = karafVersion();
        return options(
            karafDistributionConfiguration().
                frameworkUrl(KARAF_URL.version(karafVersion)).
                karafVersion(karafVersion).
                useDeployFolder(false).
                unpackDirectory(UNPACK_DIRECTORY),
            configureConsole().
                startLocalConsole().
                ignoreRemoteShell(),
            logLevel(LogLevel.DEBUG),
            keepRuntimeFolder()
        );
    }

    public String karafVersion() {
        final ConfigurationManager cm = new ConfigurationManager();
        return cm.getProperty("pax.exam.karaf.version", getDefaultKarafVersion());
    }

    protected String getDefaultKarafVersion() {
        return "4.4.6";
    }

    @Test
    public void checkKarafSystemService() {
        assertThat(bundleContext, is(notNullValue()));
        final ServiceReference<?> serviceRef = bundleContext.getServiceReference("org.apache.karaf.system.SystemService");
        final Object service = bundleContext.getService(serviceRef);
        assertThat(service, is(notNullValue()));
    }

    public Bundle findBundle(final String symbolicName) {
        final Bundle[] bundles = bundleContext.getBundles();
        for (final Bundle bundle : bundles) {
            if (bundle.getSymbolicName().equals(symbolicName)) {
                return bundle;
            }
        }
        return null;
    }

}
