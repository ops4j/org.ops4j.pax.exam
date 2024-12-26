/*
 * Copyright 2014 Ancoron.
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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsInstanceOf;
import org.hamcrest.core.StringStartsWith;
import org.junit.Assert;
import org.junit.Test;
import org.ops4j.pax.exam.CoreOptions;
import org.ops4j.pax.exam.ExamSystem;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestContainer;
import org.ops4j.pax.exam.TestContainerException;
import org.ops4j.pax.exam.options.MavenArtifactUrlReference;
import org.ops4j.pax.exam.options.UrlReference;
import org.ops4j.pax.exam.options.extra.EnvironmentOption;
import org.ops4j.pax.exam.spi.PaxExamRuntime;
import org.ops4j.pax.tinybundles.TinyBundles;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;

import static org.ops4j.pax.tinybundles.TinyBundles.rawBuilder;

public class ForkedTestContainerFactoryTest {

    @Test
    public void withBootClasspathMvn() throws BundleException, IOException,
        InterruptedException, NotBoundException, URISyntaxException {

        MavenArtifactUrlReference mvn = CoreOptions.maven(
            "org.kohsuke.metainf-services", "metainf-services", "1.2");
        startWithBootClasspath(mvn);
    }

    @Test
    public void failWithoutBootClasspath() throws BundleException, IOException,
        InterruptedException, NotBoundException, URISyntaxException {

        try {
            startWithBootClasspath(null);
            Assert.fail("This should have failed");
        } catch (TestContainerException x) {
            Throwable cause = x.getCause();

            // BundleException from BundleActivator.start() ...
            Assert.assertNotNull(cause);
            Assert.assertEquals(BundleException.class.getName(),
                cause.getClass().getName());

            // ClassNotFoundException from within BundleActivator.start() ...
            cause = cause.getCause();
            Assert.assertNotNull(cause);
            Assert.assertEquals(ClassNotFoundException.class.getName(),
                cause.getClass().getName());
            Assert.assertTrue(cause.getMessage().startsWith("org.kohsuke.metainf_services.AnnotationProcessorImpl"));
        }
    }

    @Test
    public void withBootClasspathReference() throws BundleException, IOException,
        InterruptedException, NotBoundException, URISyntaxException {

        File file = new File("target/bundles/metainf-services.jar");
        UrlReference ref = CoreOptions.url("reference:file:" + file.getAbsolutePath());
        startWithBootClasspath(ref);
    }

    @Test
    public void withBootClasspathFile() throws BundleException, IOException,
        InterruptedException, NotBoundException, URISyntaxException {

        File file = new File("target/bundles/metainf-services.jar");
        UrlReference url = CoreOptions.url("file:" + file.getAbsolutePath());
        startWithBootClasspath(url);
    }

    public void startWithBootClasspath(UrlReference url) throws BundleException,
        IOException, InterruptedException, NotBoundException, URISyntaxException {

        List<Option> options = new ArrayList<>();
        if (url != null) {
            options.add(CoreOptions.bootClasspathLibrary(url));
        }
        options.add(CoreOptions.systemPackages("org.kohsuke.metainf_services"));

        Option[] opts = options.toArray(new Option[options.size()]);
        ExamSystem system = PaxExamRuntime.createServerSystem(opts);
        ForkedTestContainerFactory factory = new ForkedTestContainerFactory();
        TestContainer[] containers = factory.create(system);

        Assert.assertNotNull(containers);
        Assert.assertNotNull(containers[0]);

        ForkedTestContainer container = (ForkedTestContainer) containers[0];
        container.start();

        File testBundle = generateBundle();
        CoreOptions.provision("file:" + testBundle.getAbsolutePath());

        // this also starts it, so we get the errors...
        container.install(new FileInputStream(testBundle));

        container.stop();
    }

    private File generateBundle() throws IOException {
        InputStream stream = TinyBundles.bundle().addClass(ClasspathTestActivator.class)
                .setHeader(Constants.BUNDLE_MANIFESTVERSION, "2")
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

    @Test
    public void withEnvironmentPassthrough() throws IOException {
        // This test only works if the environment variable 'PROPAGATE' is set externally to 'test'
        Assert.assertTrue(System.getenv().containsKey("PROPAGATE"));
        Assert.assertThat(System.getenv().get("PROPAGATE"), IsEqual.equalTo("test"));

        List<Option> options = new ArrayList<>();
        // Configure the EnvironmentOption to passthrough the 'PROPAGATE' environment variable
        options.add(new EnvironmentOption("PROPAGATE"));
        options.add(CoreOptions.frameworkProperty("test.expected.variable").value("PROPAGATE"));
        options.add(CoreOptions.frameworkProperty("test.expected.value").value("test"));

        forkWithEnvironment(options);
    }

    @Test
    public void withEnvironmentOverride() throws IOException {
        // This test only works if the environment variable 'PROPAGATE' is set externally to 'test'
        Assert.assertTrue(System.getenv().containsKey("PROPAGATE"));
        Assert.assertThat(System.getenv().get("PROPAGATE"), IsEqual.equalTo("test"));

        List<Option> options = new ArrayList<>();
        // Configure the EnvironmentOption to override the value of environment variable PROPAGATE
        options.add(new EnvironmentOption("PROPAGATE=testOverride"));
        options.add(CoreOptions.frameworkProperty("test.expected.variable").value("PROPAGATE"));
        options.add(CoreOptions.frameworkProperty("test.expected.value").value("testOverride"));

        forkWithEnvironment(options);
    }

    @Test
    public void withEnvironmentDefinition() throws IOException {
        List<Option> options = new ArrayList<>();
        // Configure the EnvironmentOption to define environment variable OTHER
        options.add(new EnvironmentOption("OTHER=value"));
        options.add(CoreOptions.frameworkProperty("test.expected.variable").value("OTHER"));
        options.add(CoreOptions.frameworkProperty("test.expected.value").value("value"));

        forkWithEnvironment(options);
    }

    @Test
    public void withoutEnvironmentPassthrough() throws IOException {
        // This test only works if the environment variable 'PROPAGATE' is set externally to 'test'
        Assert.assertTrue(System.getenv().containsKey("PROPAGATE"));
        Assert.assertThat(System.getenv().get("PROPAGATE"), IsEqual.equalTo("test"));

        List<Option> options = new ArrayList<>();
        // Do not configure a value or pass through of environment variable PROPAGATE
        options.add(CoreOptions.frameworkProperty("test.expected.variable").value("PROPAGATE"));
        options.add(CoreOptions.frameworkProperty("test.expected.value").value("test"));

        // The fork should fail as the expected environment variable is not found
        TestContainerException testContainerException = Assert.assertThrows(TestContainerException.class, () -> forkWithEnvironment(options));

        Throwable bundleException = testContainerException.getCause();
        Assert.assertThat(bundleException, IsInstanceOf.instanceOf(BundleException.class));

        Throwable illegalStateException = bundleException.getCause();
        Assert.assertThat(illegalStateException, IsInstanceOf.instanceOf(IllegalStateException.class));

        // Verify the reason of the failure
        Assert.assertThat(illegalStateException.getMessage(), StringStartsWith.startsWith("Unable to find environment variable 'PROPAGATE' with expected value 'test'"));
    }

    public void forkWithEnvironment(List<Option> options) throws IOException {
        Option[] opts = options.toArray(new Option[options.size()]);

        ExamSystem system = PaxExamRuntime.createServerSystem(opts);
        ForkedTestContainerFactory factory = new ForkedTestContainerFactory();
        TestContainer[] containers = factory.create(system);

        Assert.assertNotNull(containers);
        Assert.assertNotNull(containers[0]);

        ForkedTestContainer container = (ForkedTestContainer) containers[0];
        container.start();
        try {
            // Prepare a test bundle with EnvironmentTestActivator as activator
            File testBundle = generateEnvironmentBundle();

            // Install the test bundle, the start on EnvironmentTestActivator will fail if the configured environment
            // variable does not have the expected value
            container.install(new FileInputStream(testBundle));
        } finally {
            container.stop();
        }
    }

    private File generateEnvironmentBundle() throws IOException {
        InputStream stream = TinyBundles.bundle().addClass(EnvironmentTestActivator.class)
                .setHeader(Constants.BUNDLE_MANIFESTVERSION, "2")
                .setHeader(Constants.BUNDLE_SYMBOLICNAME, "environment.test.generated")
                .setHeader(Constants.BUNDLE_ACTIVATOR, EnvironmentTestActivator.class.getName())
                .setHeader(Constants.IMPORT_PACKAGE, "org.osgi.framework")
                .build(rawBuilder());

        File bundle = new File("target/bundles/environment-generated.jar");
        FileUtils.copyInputStreamToFile(stream, bundle);
        return bundle;
    }

    public static class EnvironmentTestActivator implements BundleActivator {

        @Override
        public void start(BundleContext bc) throws Exception {
            final String variable = bc.getProperty("test.expected.variable");
            final String expectedValue = bc.getProperty("test.expected.value");
            if (variable == null || expectedValue == null) {
                throw new IllegalStateException("Unable to retrieve value for framework property " +
                        "'test.expected.variable' and/or 'test.expected.value'");
            }
            if (!System.getenv().containsKey(variable) || !expectedValue.equals(System.getenv().get(variable))) {
                throw new IllegalStateException(
                        "Unable to find environment variable '" + variable +
                                "' with expected value '" + expectedValue + "' in: " + System.getenv());
            }
        }

        @Override
        public void stop(BundleContext bc) throws Exception {}
    }
}
