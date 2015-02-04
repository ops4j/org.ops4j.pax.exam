/*
 * Copyright 2009 Toni Menzel.
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
package org.ops4j.pax.exam.nat.internal;

import static org.ops4j.pax.exam.Constants.EXAM_FAIL_ON_UNRESOLVED_KEY;
import static org.ops4j.pax.exam.Constants.START_LEVEL_TEST_BUNDLE;
import static org.ops4j.pax.exam.CoreOptions.systemPackage;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.osgi.framework.Constants.FRAMEWORK_BOOTDELEGATION;
import static org.osgi.framework.Constants.FRAMEWORK_STORAGE;
import static org.osgi.framework.Constants.FRAMEWORK_STORAGE_CLEAN;
import static org.osgi.framework.Constants.FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT;
import static org.osgi.framework.Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.Stack;
import java.util.TreeMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.ops4j.pax.exam.ConfigurationManager;
import org.ops4j.pax.exam.Constants;
import org.ops4j.pax.exam.ExamSystem;
import org.ops4j.pax.exam.Info;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.ProbeInvoker;
import org.ops4j.pax.exam.TestAddress;
import org.ops4j.pax.exam.TestContainer;
import org.ops4j.pax.exam.TestContainerException;
import org.ops4j.pax.exam.options.BootDelegationOption;
import org.ops4j.pax.exam.options.FrameworkPropertyOption;
import org.ops4j.pax.exam.options.FrameworkStartLevelOption;
import org.ops4j.pax.exam.options.ProvisionOption;
import org.ops4j.pax.exam.options.SystemPackageOption;
import org.ops4j.pax.exam.options.SystemPropertyOption;
import org.ops4j.pax.exam.options.ValueOption;
import org.ops4j.pax.exam.options.extra.CleanCachesOption;
import org.ops4j.pax.exam.options.extra.RepositoryOption;
import org.ops4j.pax.swissbox.tracker.ServiceLookup;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkEvent;
import org.osgi.framework.FrameworkListener;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;
import org.osgi.service.startlevel.StartLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Native Test Container starts an OSGi framework using {@link FrameworkFactory} and provisions
 * the bundles configured in the Exam system.
 * <p>
 * When the framework has reached the configured start level, the container checks that all bundles
 * are resolved and throws an exception otherwise.
 * 
 * @author Toni Menzel
 * @author Harald Wellmann
 * @since Jan 7, 2010
 */
@SuppressWarnings("deprecation")
public class NativeTestContainer implements TestContainer {

    private static final Logger LOG = LoggerFactory.getLogger(NativeTestContainer.class);
    private static final String PROBE_SIGNATURE_KEY = "Probe-Signature";
    private final Stack<Long> installed = new Stack<Long>();
    private Long probeId;

    private final FrameworkFactory frameworkFactory;
    private ExamSystem system;

    private volatile Framework framework;

    public NativeTestContainer(ExamSystem system, FrameworkFactory frameworkFactory)
        throws IOException {
        this.frameworkFactory = frameworkFactory;
        this.system = system;
    }

    @Override
    public synchronized void call(TestAddress address) {
        Map<String, String> props = new HashMap<String, String>();
        props.put(PROBE_SIGNATURE_KEY, address.root().identifier());
        BundleContext bundleContext = framework.getBundleContext();
        ProbeInvoker probeInvokerService = ServiceLookup.getService(bundleContext,
            ProbeInvoker.class, props);
        probeInvokerService.call(address.arguments());
    }

    @Override
    public synchronized long install(String location, InputStream stream) {
        try {
            Bundle b = framework.getBundleContext().installBundle(location, stream);
            installed.push(b.getBundleId());
            LOG.debug("Installed bundle " + b.getSymbolicName() + " as Bundle ID "
                + b.getBundleId());
            setBundleStartLevel(b, Constants.START_LEVEL_TEST_BUNDLE);
            b.start();
            return b.getBundleId();
        }
        catch (BundleException e) {
            e.printStackTrace();
        }
        return -1;
    }

    @Override
    public synchronized long install(InputStream stream) {
        return install("local", stream);
    }

    public synchronized void cleanup() {
        while ((!installed.isEmpty())) {
            try {
                Long id = installed.pop();
                Bundle bundle = framework.getBundleContext().getBundle(id);
                bundle.uninstall();
                LOG.debug("Uninstalled bundle " + id);
            }
            catch (BundleException e) {
                // Sometimes bundles go mad when install + uninstall happens too
                // fast.
            }
        }
    }

    public Bundle getSystemBundle() {
        return framework;
    }
    
    public void setBundleStartLevel(Bundle bundle, int startLevel) {
        Method adaptMethod = getAdaptMethod(bundle);
        if (adaptMethod != null) {
            org.osgi.framework.startlevel.BundleStartLevel sl;
            try {
                sl = (org.osgi.framework.startlevel.BundleStartLevel)
                    adaptMethod.invoke(bundle, org.osgi.framework.startlevel.BundleStartLevel.class);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                LOG.error("Unable to call adapt method", e);
                throw new RuntimeException(e);
            }
            sl.setStartLevel(startLevel);
        } else {
            org.osgi.service.startlevel.StartLevel sl = ServiceLookup.getService(
                            framework.getBundleContext(),
                            org.osgi.service.startlevel.StartLevel.class);
            sl.setBundleStartLevel(bundle, startLevel);
        }
    }

    private Method getAdaptMethod(Object object) {
        Method[] methods = object.getClass().getMethods();
        for (Method method : methods) {
            if ("adapt".equals(method.getName())) {
                if (method.getParameterTypes().length == 1
                        && method.getParameterTypes()[0].isLocalClass()) {
                    return method;
                }
            }
        }
        return null;
    }

    @Override
    public TestContainer start() {
        try {
            system = system.fork(new Option[] {
                systemPackage("org.ops4j.pax.exam;version="
                    + skipSnapshotFlag(Info.getPaxExamVersion())),
                systemPackage("org.ops4j.pax.exam.options;version="
                    + skipSnapshotFlag(Info.getPaxExamVersion())),
                systemPackage("org.ops4j.pax.exam.util;version="
                    + skipSnapshotFlag(Info.getPaxExamVersion())),
                systemProperty("java.protocol.handler.pkgs").value("org.ops4j.pax.url") });
            Map<String, String> p = createFrameworkProperties();
            if (LOG.isDebugEnabled()) {
                logFrameworkProperties(p);
                logSystemProperties();
            }
            framework = frameworkFactory.newFramework(p);
            framework.init();
            installAndStartBundles(framework.getBundleContext());
        }
        catch (BundleException e) {
            throw new TestContainerException("Problem starting test container.", e);
        }
        catch (IOException e) {
            throw new TestContainerException("Problem starting test container.", e);
        }
        return this;
    }

    private void logFrameworkProperties(Map<String, String> p) {
        LOG.debug("==== Framework properties:");
        for (String key : p.keySet()) {
            LOG.debug("{} = {}", key, p.get(key));
        }
    }

    private void logSystemProperties() {
        LOG.debug("==== System properties:");
        SortedMap<Object, Object> map = new TreeMap<Object, Object>(System.getProperties());
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            LOG.debug("{} = {}", entry.getKey(), entry.getValue());
        }
    }

    @Override
    public TestContainer stop() {
        if (framework != null) {
            try {
                cleanup();
                stopOrAbort();
                framework = null;
                system.clear();
            }
            catch (BundleException e) {
                LOG.warn("Problem during stopping fw.", e);
            }
            catch (InterruptedException e) {
                LOG.warn("InterruptedException during stopping fw.", e);
            }
        }
        else {
            LOG.warn("Framework does not exist. Called start() before ? ");
        }
        return this;
    }

    private void stopOrAbort() throws BundleException, InterruptedException {
        framework.stop();
        long timeout = system.getTimeout().getValue();
        Thread stopper = new Stopper(timeout);
        stopper.start();
        stopper.join(timeout + 500);

        // If the framework is not stopped, then we're in trouble anyway, so we do not worry
        // about stopping the worker thread.

        if (framework.getState() != Framework.RESOLVED) {
            String message = "Framework has not yet stopped after " + timeout
                + " ms. waitForStop did not return";
            throw new TestContainerException(message);
        }
    }

    private Map<String, String> createFrameworkProperties() throws IOException {
        final Map<String, String> p = new HashMap<String, String>();
        CleanCachesOption cleanCaches = system.getSingleOption(CleanCachesOption.class);
        if (cleanCaches != null && cleanCaches.getValue() != null && cleanCaches.getValue()) {
            p.put(FRAMEWORK_STORAGE_CLEAN, FRAMEWORK_STORAGE_CLEAN_ONFIRSTINIT);
        }

        p.put(FRAMEWORK_STORAGE, system.getTempFolder().getAbsolutePath());
        p.put(FRAMEWORK_SYSTEMPACKAGES_EXTRA,
            buildString(system.getOptions(SystemPackageOption.class)));
        p.put(FRAMEWORK_BOOTDELEGATION, buildString(system.getOptions(BootDelegationOption.class)));

        for (FrameworkPropertyOption option : system.getOptions(FrameworkPropertyOption.class)) {
            p.put(option.getKey(), (String) option.getValue());
        }

        for (SystemPropertyOption option : system.getOptions(SystemPropertyOption.class)) {
            System.setProperty(option.getKey(), option.getValue());
        }

        String repositories = buildString(system.getOptions(RepositoryOption.class));
        if (!repositories.isEmpty()) {
            System.setProperty("org.ops4j.pax.url.mvn.repositories", repositories);
        }
        return p;
    }

    private String buildString(ValueOption<?>[] options) {
        return buildString(new String[0], options, new String[0]);
    }

    @SuppressWarnings("unused")
    private String buildString(String[] prepend, ValueOption<?>[] options) {
        return buildString(prepend, options, new String[0]);
    }

    @SuppressWarnings("unused")
    private String buildString(ValueOption<?>[] options, String[] append) {
        return buildString(new String[0], options, append);
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

    private void installAndStartBundles(BundleContext context) throws BundleException {
        List<Bundle> bundles = new ArrayList<Bundle>();
        for (ProvisionOption<?> bundle : system.getOptions(ProvisionOption.class)) {
            Bundle b = context.installBundle(bundle.getURL());
            bundles.add(b);
            int startLevel = getStartLevel(bundle);
            setBundleStartLevel(b, startLevel);
            if (bundle.shouldStart()) {
                try {
                    b.start();
                } 
                catch (BundleException e) {
                    throw new BundleException("Error starting bundle " + b.getSymbolicName() + ". " + e.getMessage(), e);
                }
                LOG.debug("+ Install (start@{}) {}", startLevel, bundle);
            }
            else {
                LOG.debug("+ Install (no start) {}", bundle);
            }
        }
        // All bundles are installed, we can now start the framework...
        framework.start();
        StartLevel startLevel = null;
        Method adaptMethod = getAdaptMethod(framework);
        if (adaptMethod != null) {
            org.osgi.framework.startlevel.FrameworkStartLevel fsl;
            try {
                fsl = (org.osgi.framework.startlevel.FrameworkStartLevel)
                adaptMethod.invoke(framework, org.osgi.framework.startlevel.FrameworkStartLevel.class);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                LOG.error("Unable to invoke adapt method", e);
                throw new RuntimeException(e);
            }
            startLevel = new StartLevelAdapter(fsl);
        } else {
            startLevel = ServiceLookup.getService(
                            framework.getBundleContext(),
                            StartLevel.class);
        }
        setFrameworkStartLevel(context, startLevel);
        verifyThatBundlesAreResolved(bundles);
    }

    private void setFrameworkStartLevel(BundleContext context, final StartLevel sl) {
        FrameworkStartLevelOption startLevelOption = system
            .getSingleOption(FrameworkStartLevelOption.class);
        final int startLevel = startLevelOption == null ? START_LEVEL_TEST_BUNDLE
            : startLevelOption.getStartLevel();
        LOG.debug("Jump to startlevel: " + startLevel);
        final CountDownLatch latch = new CountDownLatch(1);
        context.addFrameworkListener(new FrameworkListener() {

            @Override
            public void frameworkEvent(FrameworkEvent frameworkEvent) {
                if (frameworkEvent.getType() == FrameworkEvent.STARTLEVEL_CHANGED) {
                    if (sl.getStartLevel() == startLevel) {
                        latch.countDown();
                    }
                }
            }
        });
        sl.setStartLevel(startLevel);

        // Check the current start level before starting to wait.
        if (sl.getStartLevel() == startLevel) {
            LOG.debug("requested start level reached");
            return;
        }
        else {
            LOG.debug("start level {} requested, current start level is {}", startLevel,
                sl.getStartLevel());
        }

        try {
            long timeout = system.getTimeout().getValue();
            if (!latch.await(timeout, TimeUnit.MILLISECONDS)) {
                // Before throwing an exception, do a last check
                if (startLevel != sl.getStartLevel()) {
                    String msg = String.format("start level %d has not been reached within %d ms",
                        startLevel, timeout);
                    throw new TestContainerException(msg);
                }
                else {
                    // We reached the requested start level.
                    LOG.debug("requested start level reached");
                }

            }
        }
        catch (InterruptedException e) {
            throw new TestContainerException(e);
        }
    }

    private void verifyThatBundlesAreResolved(List<Bundle> bundles) {
        boolean hasUnresolvedBundles = false;
        for (Bundle bundle : bundles) {
            if (bundle.getState() == Bundle.INSTALLED) {
                LOG.error("Bundle [{}] is not resolved", bundle);
                hasUnresolvedBundles = true;
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

    private int getStartLevel(ProvisionOption<?> bundle) {
        Integer start = bundle.getStartLevel();
        if (start == null) {
            start = Constants.START_LEVEL_DEFAULT_PROVISION;
        }
        return start;
    }

    private String skipSnapshotFlag(String version) {
        int idx = version.indexOf("-");
        if (idx >= 0) {
            return version.substring(0, idx);
        }
        else {
            return version;
        }
    }

    @Override
    public String toString() {
        return "Native:" + frameworkFactory.getClass().getSimpleName();
    }

    /**
     * Worker thread for shutting down the framework. We'd expect Framework.waitForStop(timeout) to
     * return after the given timeout, but this is not the case with Equinox (tested on 3.6.2 and
     * 3.7.0), so we use this worker thread to avoid blocking the main thread.
     * 
     * @author Harald Wellmann
     */
    private class Stopper extends Thread {

        private final long timeout;

        private Stopper(long timeout) {
            this.timeout = timeout;
        }

        @Override
        public void run() {
            try {
                FrameworkEvent frameworkEvent = framework.waitForStop(timeout);
                if (frameworkEvent.getType() != FrameworkEvent.STOPPED) {
                    LOG.error("Framework has not yet stopped after {} ms. "
                        + "waitForStop returned: {}", timeout, frameworkEvent);
                }
            }
            catch (InterruptedException exc) {
                LOG.error("Stopper thread was interrupted");
            }
        }
    }

    @Override
    public synchronized long installProbe(InputStream stream) {
        probeId = install(stream);
        installed.pop();
        return probeId;
    }

    @Override
    public synchronized void uninstallProbe() {
        Bundle bundle = framework.getBundleContext().getBundle(probeId);
        try {
            bundle.uninstall();
            probeId = null;
        }
        catch (BundleException exc) {
            throw new TestContainerException(exc);
        }
    }
}
