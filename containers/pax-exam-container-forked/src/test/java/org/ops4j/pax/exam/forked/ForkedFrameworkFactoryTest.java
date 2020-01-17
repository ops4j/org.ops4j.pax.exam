/*
 * Copyright 2011 Harald Wellmann.
 *
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
package org.ops4j.pax.exam.forked;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.rmi.NotBoundException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.apache.commons.io.FileUtils;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.TestContainerException;
import org.ops4j.pax.swissbox.framework.RemoteFramework;
import org.ops4j.pax.tinybundles.TinyBundles;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.FrameworkFactory;

import static org.ops4j.pax.tinybundles.TinyBundles.rawBuilder;

public class ForkedFrameworkFactoryTest {

    private File storage;

    @Rule
    public TestName test = new TestName();

    @Before
    public void beforeTest() {
        storage = new File("target/storage/" + test.getMethodName());
        storage.mkdirs();
    }

    @After
    public void afterTest() throws IOException {
        // FileUtils.deleteDirectory(storage);
        storage = null;
        System.clearProperty(org.ops4j.pax.exam.Constants.EXAM_INVOKER_PORT); // set implicitly
    }

    @Test
    public void forkEquinox() throws BundleException, IOException, InterruptedException,
        NotBoundException, URISyntaxException {
        ServiceLoader<FrameworkFactory> loader = ServiceLoader.load(FrameworkFactory.class);
        FrameworkFactory frameworkFactory = loader.iterator().next();

        ForkedFrameworkFactory forkedFactory = new ForkedFrameworkFactory(frameworkFactory);

        Map<String, Object> frameworkProperties = new HashMap<String, Object>();
        frameworkProperties.put(Constants.FRAMEWORK_STORAGE, storage.getAbsolutePath());
        RemoteFramework framework = forkedFactory.fork(Collections.<String> emptyList(),
            Collections.<String, String> emptyMap(), frameworkProperties);
        framework.start();

        long bundleId = framework
            .installBundle("file:target/bundles/pax-exam-sample9-pde.jar");
        framework.startBundle(bundleId);

        framework.callService("(objectClass=org.ops4j.pax.exam.sample9.pde.HelloService)",
            "getMessage");

        Thread.sleep(3000);
        framework.stop();

        forkedFactory.join();
    }

    @Test
    public void forkWithBootClasspath() throws BundleException, IOException, InterruptedException,
        NotBoundException, URISyntaxException {
        ServiceLoader<FrameworkFactory> loader = ServiceLoader.load(FrameworkFactory.class);
        FrameworkFactory frameworkFactory = loader.iterator().next();

        ForkedFrameworkFactory forkedFactory = new ForkedFrameworkFactory(frameworkFactory);

        List<String> bootClasspath = Arrays.asList(
            new File("target/bundles/metainf-services.jar").getCanonicalPath()
        );

        Map<String, Object> frameworkProperties = new HashMap<String, Object>();
        frameworkProperties.put(Constants.FRAMEWORK_STORAGE, storage.getAbsolutePath());
        frameworkProperties.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA,
            "org.kohsuke.metainf_services");
        RemoteFramework framework = forkedFactory.fork(Collections.<String> emptyList(),
            Collections.<String, String> emptyMap(), frameworkProperties, null,
            bootClasspath);
        framework.start();

        File testBundle = generateBundle();
        long bundleId = framework.installBundle("file:" + testBundle.getAbsolutePath());
        framework.startBundle(bundleId);

        // START>>> not yet implemented
        // framework.waitForState(bundleId, Bundle.ACTIVE, 1500);
        Thread.sleep(3000);
        // <<<END not yet implemented

        framework.stop();

        forkedFactory.join();
    }

    @Test(expected = TestContainerException.class)
    public void forkWithInvalidBootClasspath() throws BundleException, IOException, InterruptedException,
        NotBoundException, URISyntaxException {
        ServiceLoader<FrameworkFactory> loader = ServiceLoader.load(FrameworkFactory.class);
        FrameworkFactory frameworkFactory = loader.iterator().next();

        ForkedFrameworkFactory forkedFactory = new ForkedFrameworkFactory(frameworkFactory);

        List<String> bootClasspath = Arrays.asList(
                CoreOptions.maven("org.kohsuke.metainf-services", "metainf-services", "1.2").getURL()
        );

        Map<String, Object> frameworkProperties = new HashMap<String, Object>();
        frameworkProperties.put(Constants.FRAMEWORK_STORAGE, storage.getAbsolutePath());
        frameworkProperties.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA,
            "org.kohsuke.metainf_services");
        forkedFactory.fork(Collections.<String> emptyList(),
            Collections.<String, String> emptyMap(), frameworkProperties, null,
            bootClasspath);
    }

    @Test()
    public void verifyPortConfiguration() throws Exception {
        ForkedFrameworkFactory frameworkFactory = new ForkedFrameworkFactory(null);

        new SystemPropertyRunnable(org.ops4j.pax.exam.Constants.EXAM_FORKED_INVOKER_PORT, "15000") {
            @Override
            protected void doRun() {
                Assert.assertThat(frameworkFactory.getPort(), CoreMatchers.equalTo(15000));
            }
        }.run();
        new SystemPropertyRunnable(org.ops4j.pax.exam.Constants.EXAM_FORKED_INVOKER_PORT_RANGE_LOWERBOUND, "15000") {
            @Override
            protected void doRun() {
                Assert.assertThat(frameworkFactory.getPort(), CoreMatchers.equalTo(15000));
            }
        }.run();
    }

    private File generateBundle() throws IOException {
        InputStream stream = TinyBundles.bundle().addClass(ClasspathTestActivator.class)
                .setHeader(Constants.BUNDLE_SYMBOLICNAME, "boot.classpath.test.generated")
                .setHeader(Constants.IMPORT_PACKAGE, "org.osgi.framework, org.kohsuke.metainf_services")
                .setHeader(Constants.BUNDLE_ACTIVATOR, ClasspathTestActivator.class.getName())
                .build(rawBuilder());

        File bundle = new File("target/bundles/boot-classpath-generated.jar");
        FileUtils.copyInputStreamToFile(stream, bundle);
        return bundle;
    }

    public static class ClasspathTestActivator implements BundleActivator {

        private final String className = "org.kohsuke.metainf_services.AnnotationProcessorImpl";

        @Override
        public void start(BundleContext bc) throws Exception {
            Class<?> clazz = getClass().getClassLoader().loadClass(className);

            if (clazz == null) {
                throw new IllegalStateException("Class '" + className + "' not loaded");
            }
        }

        @Override
        public void stop(BundleContext bc) throws Exception {}
    }
}
