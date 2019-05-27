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

import static java.util.stream.Collectors.joining;
import static org.ops4j.pax.exam.Constants.EXAM_FAIL_ON_UNRESOLVED_KEY;
import static org.ops4j.pax.exam.Constants.EXAM_FORKED_INVOKER_PORT;
import static org.ops4j.pax.exam.Constants.EXAM_INVOKER_PORT;
import static org.ops4j.pax.exam.Constants.EXAM_INVOKER_PORT_RANGE_LOWERBOUND;
import static org.ops4j.pax.exam.Constants.EXAM_INVOKER_PORT_RANGE_UPPERBOUND;
import static org.ops4j.pax.exam.Constants.START_LEVEL_TEST_BUNDLE;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.osgi.framework.Constants.FRAMEWORK_BOOTDELEGATION;
import static org.osgi.framework.Constants.FRAMEWORK_STORAGE;
import static org.osgi.framework.Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.ops4j.io.StreamUtils;
import org.ops4j.net.FreePort;
import org.ops4j.pax.exam.ConfigurationManager;
import org.ops4j.pax.exam.ExamConfigurationException;
import org.ops4j.pax.exam.ExamSystem;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.RelativeTimeout;
import org.ops4j.pax.exam.TestContainer;
import org.ops4j.pax.exam.TestContainerException;
import org.ops4j.pax.exam.TestDescription;
import org.ops4j.pax.exam.TestListener;
import org.ops4j.pax.exam.forked.provision.PlatformImpl;
import org.ops4j.pax.exam.options.BootClasspathLibraryOption;
import org.ops4j.pax.exam.options.BootDelegationOption;
import org.ops4j.pax.exam.options.FrameworkPropertyOption;
import org.ops4j.pax.exam.options.FrameworkStartLevelOption;
import org.ops4j.pax.exam.options.PropagateSystemPropertyOption;
import org.ops4j.pax.exam.options.ProvisionOption;
import org.ops4j.pax.exam.options.SystemPackageOption;
import org.ops4j.pax.exam.options.SystemPropertyOption;
import org.ops4j.pax.exam.options.UrlReference;
import org.ops4j.pax.exam.options.ValueOption;
import org.ops4j.pax.exam.options.extra.RepositoryOption;
import org.ops4j.pax.exam.options.extra.VMOption;
import org.ops4j.pax.exam.spi.listener.TestListenerTask;
import org.ops4j.pax.swissbox.framework.RemoteFramework;
import org.ops4j.pax.swissbox.framework.RemoteServiceReference;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.launch.FrameworkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link TestContainer} which launches an OSGi framework in a forked Java VM to isolate the
 * framework parent class loader from the application class loader containing Pax Exam and
 * additional user classes.
 * <p>
 * The drawback of this container is that remote debugging is required to debug the tests executed
 * by the forked framework.
 *
 * @author Harald Wellmann
 */
public class ForkedTestContainer implements TestContainer {

    private static final Logger LOG = LoggerFactory.getLogger(ForkedTestContainer.class);

    private ExamSystem system;
    private final ForkedFrameworkFactory frameworkFactory;
    private RemoteFramework remoteFramework;
    private final PlatformImpl platform;
    private final String name;

    private int port;
    private HashMap<Long, String> bundlesById;

    public ForkedTestContainer(ExamSystem system, FrameworkFactory frameworkFactory) {
        this.system = system;
        this.frameworkFactory = new ForkedFrameworkFactory(frameworkFactory);
        this.platform = new PlatformImpl();
        this.name = "Forked:" + frameworkFactory.getClass().getSimpleName();
    }

    private long install(InputStream stream) {
        try {
            long bundleId = remoteFramework.installBundle("local", pack(stream));
            remoteFramework.startBundle(bundleId);
            return bundleId;
        }
        catch (RemoteException | BundleException exc) {
            throw new TestContainerException(exc);
        }
    }

    @Override
    public void start() throws IOException {
        try {
            port = getPort();
            system = system.fork(new Option[] {
                systemProperty("java.protocol.handler.pkgs").value("org.ops4j.pax.url"),
                systemProperty(EXAM_INVOKER_PORT).value(port)
                });
            List<String> vmArgs = createVmArguments();
            Map<String, String> systemProperties = createSystemProperties();
            Map<String, Object> frameworkProperties = createFrameworkProperties();
            List<String> beforeFrameworkClasspath = new ArrayList<>();
            List<String> afterFrameworkClasspath = new ArrayList<>();

            BootClasspathLibraryOption[] bootClasspathLibraryOptions = system
                .getOptions(BootClasspathLibraryOption.class);

            if (bootClasspathLibraryOptions != null && bootClasspathLibraryOptions.length > 0) {
                for (BootClasspathLibraryOption bootClasspathLibraryOption : bootClasspathLibraryOptions) {
                    UrlReference libraryUrl = bootClasspathLibraryOption.getLibraryUrl();
                    String library = localize(libraryUrl.getURL());

                    if (bootClasspathLibraryOption.isAfterFramework()) {
                        afterFrameworkClasspath.add(library);
                    }
                    else {
                        beforeFrameworkClasspath.add(library);
                    }
                }
            }

            remoteFramework = frameworkFactory.fork(vmArgs, systemProperties, frameworkProperties,
                beforeFrameworkClasspath, afterFrameworkClasspath);
            remoteFramework.init();
            installAndStartBundles();
        }
        catch (BundleException exc) {
            throw new TestContainerException(exc);
        }
        catch (ExamConfigurationException e) {
            throw new TestContainerException("Problem in test container configuration", e);
        }
    }

    protected int getPort() {
        String configuredPort = System.getProperty(EXAM_INVOKER_PORT);
        if (configuredPort != null) {
            return Integer.parseInt(configuredPort);
        } else {
            // fails if user configure a wrong port value
            int lowerBound = Integer.parseInt(System.getProperty(EXAM_INVOKER_PORT_RANGE_LOWERBOUND, "20000"));
            int upperBound = Integer.parseInt(System.getProperty(EXAM_INVOKER_PORT_RANGE_UPPERBOUND, "21000"));
            return new FreePort(lowerBound, upperBound).getPort();
        }
    }

    @Override
    public void stop() {
        try {
            remoteFramework.stop();
            system.clear();
        }
        catch (RemoteException | BundleException exc) {
            throw new TestContainerException(exc);
        }
        frameworkFactory.join();
        system.clear();
    }

    private byte[] pack(InputStream stream) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            StreamUtils.copyStream(stream, out, true);
        }
        catch (IOException e) {

        }
        return out.toByteArray();
    }

    private Map<String, Object> createFrameworkProperties() throws IOException {
        final Map<String, Object> p = new HashMap<String, Object>();
        p.put(FRAMEWORK_STORAGE, system.getTempFolder().getAbsolutePath());
        SystemPackageOption[] systemPackageOptions = system.getOptions(SystemPackageOption.class);
        if (systemPackageOptions.length > 0) {
            p.put(FRAMEWORK_SYSTEMPACKAGES_EXTRA, join(systemPackageOptions));
        }
        p.put(FRAMEWORK_BOOTDELEGATION, join(system.getOptions(BootDelegationOption.class)));

        for (FrameworkPropertyOption option : system.getOptions(FrameworkPropertyOption.class)) {
            p.put(option.getKey(), option.getValue());
        }

        for (SystemPropertyOption option : system.getOptions(SystemPropertyOption.class)) {
            System.setProperty(option.getKey(), option.getValue());
        }
        return p;
    }

    private List<String> createVmArguments() {
        VMOption[] options = system.getOptions(VMOption.class);
        List<String> args = new ArrayList<String>();
        for (VMOption option : options) {
            args.add(option.getOption());
        }
        return args;

    }

    private Map<String, String> createSystemProperties() {
        Map<String, String> p = new HashMap<String, String>();
        for (PropagateSystemPropertyOption option : system
            .getOptions(PropagateSystemPropertyOption.class)) {
            String key = option.getKey();
            String value = System.getProperty(key);
            if (value != null) {
                p.put(key, value);
            }
        }

        for (SystemPropertyOption option : system.getOptions(SystemPropertyOption.class)) {
            p.put(option.getKey(), option.getValue());
        }

        RepositoryOption[] repositories = system.getOptions(RepositoryOption.class);
        if (repositories.length != 0) {
            System.setProperty("org.ops4j.pax.url.mvn.repositories", join(repositories));
        }

        return p;
    }

    private String join(ValueOption<?>[] options) {
        return Arrays.stream(options).map(o -> o.getValue().toString()).collect(joining(","));
    }

    private void installAndStartBundles() throws BundleException, RemoteException, ExamConfigurationException {
        File workDir = new File(system.getTempFolder(), "pax-exam-downloads");
        workDir.mkdirs();
        List<Long> bundleIds = new ArrayList<Long>();
        ProvisionOption<?>[] options = system.getOptions(ProvisionOption.class);
        bundlesById = new HashMap<Long, String>();
        for (ProvisionOption<?> bundle : options) {
            String localUrl = downloadBundle(workDir, bundle.getURL());
            long id = remoteFramework.installBundle(localUrl, bundle.shouldStart(),getStartLevel(bundle));
            bundlesById.put(id,bundle.getURL());
            if (bundle.shouldStart()) {
                bundleIds.add(id);
            }
        }
        remoteFramework.start();
        setFrameworkStartLevel();
        verifyThatBundlesAreResolved(bundleIds);
    }

    private void setFrameworkStartLevel() throws RemoteException, ExamConfigurationException {
        FrameworkStartLevelOption startLevelOption = system.getOption(FrameworkStartLevelOption.class);
        int startLevel = startLevelOption == null ? START_LEVEL_TEST_BUNDLE : startLevelOption
            .getStartLevel();
        LOG.debug("Jump to startlevel [{}]", startLevel);
        long timeout = system.getTimeout().getValue();
        boolean startLevelReached = remoteFramework.setFrameworkStartLevel(startLevel, timeout);

        if (!startLevelReached) {
            String msg = String.format("start level %d has not been reached within %d ms",
                startLevel, timeout);
            throw new TestContainerException(msg);
        }
    }

    private void verifyThatBundlesAreResolved(List<Long> bundleIds) throws RemoteException {
        boolean hasUnresolvedBundles = false;
        for (long bundleId : bundleIds) {
            try {
                if (remoteFramework.getBundleState(bundleId) == Bundle.INSTALLED) {
                    LOG.error("Bundle [id:{}, url:{}] is not resolved", bundleId, bundlesById.get(bundleId));
                    hasUnresolvedBundles = true;
                }
            }
            catch (BundleException exc) {
                throw new TestContainerException(exc);
            }
        }
        ConfigurationManager cm = new ConfigurationManager();
        boolean failOnUnresolved = Boolean.parseBoolean(cm.getProperty(EXAM_FAIL_ON_UNRESOLVED_KEY,
            "false"));
        if (hasUnresolvedBundles && failOnUnresolved) {
            throw new TestContainerException(
                "There are unresolved bundles. See previous ERROR log messages for details.");
        }
    }

    private String downloadBundle(File workDir, String url) {
        try {
            URL realUrl = new URL(url);
            if (realUrl.getProtocol().equals("reference")) {
                return url;
            }
            File localBundle = platform.download(workDir, realUrl, url, false, true,
                true, false);
            return localBundle.toURI().toURL().toString();
        }
        catch (MalformedURLException exc) {
            throw new TestContainerException(exc);
        }
    }

    private String localize(String url) {
        try {
            URL realUrl = new URL(url);
            if (realUrl.getProtocol().equals("reference")) {
                // must be "reference:file:..."
                return new URL(realUrl.getPath()).getPath();
            }
            else if (realUrl.getProtocol().equals("file")) {
                return realUrl.getPath();
            }
            File artifact = platform.download(system.getTempFolder(), realUrl, url, false, false,
                false, false);
            return artifact.getCanonicalPath();
        }
        catch (MalformedURLException exc) {
            throw new TestContainerException(exc);
        }
        catch (IOException exc) {
            throw new TestContainerException(exc);
        }
    }

    private int getStartLevel(ProvisionOption<?> bundle) {
        Integer start = bundle.getStartLevel();
        if (start == null) {
            start = org.ops4j.pax.exam.Constants.START_LEVEL_DEFAULT_PROVISION;
        }
        return start;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public void installProbe(InputStream stream) throws IOException {
        install(stream);
    }


    @Override
    public void runTest(TestDescription description, TestListener listener) {
        String filterExpression = "(&(objectClass=org.ops4j.pax.exam.ProbeInvoker))";
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            TestListenerTask task = new TestListenerTask(serverSocket, listener);
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(task);

            RemoteServiceReference[] references = remoteFramework.getServiceReferences(
                filterExpression, system.getTimeout().getValue(), TimeUnit.MILLISECONDS);
            remoteFramework.invokeMethodOnService(references[0], "runTestClass", description.toString());
            executor.shutdown();
            serverSocket.close();
        }
        catch (InvocationTargetException exc) {
            LOG.error("", exc.getCause());
            throw new TestContainerException(exc.getCause());
        }
        // CHECKSTYLE:SKIP
        catch (Exception exc) {
            throw new TestContainerException(exc);
        }
    }

    @Override
    public Object remoteCall(Class<?> serviceType, String methodName, Class<?>[] methodParamTypes,
                     String filter, RelativeTimeout timeout, Object... actualParams) {
        try {
            String realFilter = "(&(objectClass=" + serviceType.getName() + "))";
            RemoteServiceReference[] references = remoteFramework.getServiceReferences(
                    realFilter, timeout.getValue(), TimeUnit.MILLISECONDS);
            return remoteFramework.invokeMethodOnService(references[0], methodName, actualParams);
        }
        catch (Exception ise) {
            throw new RuntimeException(ise);
        }
    }
}
