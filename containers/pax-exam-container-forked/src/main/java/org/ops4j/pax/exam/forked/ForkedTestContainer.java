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

import static org.ops4j.pax.exam.Constants.EXAM_FAIL_ON_UNRESOLVED_KEY;
import static org.ops4j.pax.exam.Constants.START_LEVEL_TEST_BUNDLE;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.osgi.framework.Constants.FRAMEWORK_BOOTDELEGATION;
import static org.osgi.framework.Constants.FRAMEWORK_STORAGE;
import static org.osgi.framework.Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.ops4j.io.StreamUtils;
import org.ops4j.pax.exam.ConfigurationManager;
import org.ops4j.pax.exam.ExamSystem;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestAddress;
import org.ops4j.pax.exam.TestContainer;
import org.ops4j.pax.exam.TestContainerException;
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
import org.ops4j.pax.exam.options.extra.EnvironmentOption;
import org.ops4j.pax.exam.options.extra.RepositoryOption;
import org.ops4j.pax.exam.options.extra.VMOption;
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
    private Long probeId;

    public ForkedTestContainer(ExamSystem system, FrameworkFactory frameworkFactory) {
        this.system = system;
        this.frameworkFactory = new ForkedFrameworkFactory(frameworkFactory);
        this.platform = new PlatformImpl();
        this.name = "Forked:" + frameworkFactory.getClass().getSimpleName();
    }

    @Override
    public void call(TestAddress address) {
        String filterExpression = "(&(objectClass=org.ops4j.pax.exam.ProbeInvoker)(Probe-Signature="
            + address.root().identifier() + "))";
        try {
            RemoteServiceReference[] references = remoteFramework.getServiceReferences(
                filterExpression, system.getTimeout().getValue(), TimeUnit.MILLISECONDS);
            remoteFramework.invokeMethodOnService(references[0], "call",
                (Object) address.arguments());
        }
        // CHECKSTYLE:SKIP
        catch (Exception exc) {
            throw new TestContainerException(exc);
        }
    }

    @Override
    public long install(String location, InputStream stream) {
        try {
            return remoteFramework.installBundle(location);
        }
        catch (RemoteException | BundleException exc) {
            throw new TestContainerException(exc);
        }
    }

    @Override
    public long install(InputStream stream) {
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
    public TestContainer start() {
        try {
            system = system.fork(new Option[] { systemProperty("java.protocol.handler.pkgs").value(
                "org.ops4j.pax.url") });
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

            List<String> environment = new ArrayList<>();
            EnvironmentOption[] environmentOptions = system.getOptions(EnvironmentOption.class);
            for (EnvironmentOption environmentOption : environmentOptions) {
                environment.add(environmentOption.getEnvironment());
            }
            String[] env = environment.toArray(new String[environment.size()]);

            remoteFramework = frameworkFactory.fork(vmArgs, systemProperties, frameworkProperties,
                beforeFrameworkClasspath, afterFrameworkClasspath, env);
            remoteFramework.init();
            installAndStartBundles();
        }
        catch (BundleException | IOException exc) {
            throw new TestContainerException(exc);
        }
        return this;
    }

    @Override
    public TestContainer stop() {
        try {
            remoteFramework.stop();
            system.clear();
        }
        catch (RemoteException | BundleException exc) {
            throw new TestContainerException(exc);
        }
        frameworkFactory.join();
        system.clear();
        return this;
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
            p.put(FRAMEWORK_SYSTEMPACKAGES_EXTRA, buildString(systemPackageOptions));
        }
        p.put(FRAMEWORK_BOOTDELEGATION, buildString(system.getOptions(BootDelegationOption.class)));

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
            System.setProperty("org.ops4j.pax.url.mvn.repositories", buildString(repositories));
        }

        return p;
    }

    private String buildString(ValueOption<?>[] options) {
        return buildString(new String[0], options, new String[0]);
    }

    private String buildString(String[] prepend, ValueOption<?>[] options, String[] append) {
        StringBuilder builder = new StringBuilder();
        for (String a : prepend) {
            builder.append(a);
            builder.append(",");
        }
        for (ValueOption<?> option : options) {
            builder.append(option.getValue());
            builder.append(",");
        }
        for (String a : append) {
            builder.append(a);
            builder.append(",");
        }
        if (builder.length() > 0) {
            return builder.substring(0, builder.length() - 1);
        }
        else {
            return "";
        }
    }

    private void installAndStartBundles() throws BundleException, RemoteException {
        File workDir = new File(system.getTempFolder(), "pax-exam-downloads");
        workDir.mkdirs();
        List<Long> bundleIds = new ArrayList<Long>();
        ProvisionOption<?>[] options = system.getOptions(ProvisionOption.class);
        Map<String, Long> remoteMappings = new HashMap<String, Long>();
        Map<Long, String> bundlesById = new HashMap<Long, String>();
        for (ProvisionOption<?> bundle : options) {
            String localUrl = downloadBundle(workDir, bundle.getURL());
            long bundleId = remoteFramework.installBundle(localUrl);
            remoteMappings.put(bundle.getURL(), bundleId);
            bundlesById.put(bundleId, bundle.getURL());
        }
        // All bundles are installed, we can now start the framework...
        remoteFramework.start();

        // iterate over the bundles, set start level and start them
        // TODO Simplify with new method in Pax Swissbox 1.7.0:
        // remoteFramework.installBundle(localUrl, startLevel, autostart);
        for (ProvisionOption<?> bundle : options) {
            int startLevel = getStartLevel(bundle);
            Long bundleId = remoteMappings.get(bundle.getURL());
            remoteFramework.setBundleStartLevel(bundleId, startLevel);
            if (bundle.shouldStart()) {
                bundleIds.add(bundleId);
                remoteFramework.startBundle(bundleId);
                LOG.debug("+ Install (start@{}) {}", startLevel, bundle);
            }
            else {
                LOG.debug("+ Install (no start) {}", bundle);
            }
        }
        setFrameworkStartLevel();
        verifyThatBundlesAreResolved(bundleIds, bundlesById);
    }

    private void setFrameworkStartLevel() throws RemoteException {
        FrameworkStartLevelOption startLevelOption = system
            .getSingleOption(FrameworkStartLevelOption.class);
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

    private void verifyThatBundlesAreResolved(List<Long> bundleIds, Map<Long, String> bundlesById) throws RemoteException {
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
    public long installProbe(InputStream stream) {
        this.probeId = install(stream);
        return probeId;
    }

    @Override
    public void uninstallProbe() {
        try {
            remoteFramework.uninstallBundle(probeId);
        }
        catch (RemoteException | BundleException exc) {
            throw new TestContainerException(exc);
        }
    }
}
