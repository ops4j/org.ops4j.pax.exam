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
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.ops4j.exec.ExecutionException;
import org.ops4j.net.FreePort;
import org.ops4j.pax.exam.ExamJavaRunner;
import org.ops4j.pax.exam.TestContainerException;
import org.ops4j.pax.swissbox.framework.RemoteFramework;
import org.ops4j.pax.swissbox.framework.RemoteFrameworkImpl;
import org.ops4j.pax.swissbox.tracker.ServiceLookup;
import org.osgi.framework.launch.FrameworkFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Wraps an OSGi {@link FrameworkFactory} to create and launch a framework in a forked Java virtual
 * machine running in a separate process.
 * <p>
 * The framework in the forked process can be controlled via a {@link RemoteFramework} interface.
 *
 * @author Harald Wellmann
 *
 */
public class ForkedFrameworkFactory {

    private static final Logger LOG = LoggerFactory.getLogger(ForkedFrameworkFactory.class);

    // TODO make this configurable
    private static final long TIMEOUT = 60 * 1000;

    private FrameworkFactory frameworkFactory;
    private Registry registry;

    private int port;

    private ExamJavaRunner javaRunner;

    /**
     * Creates a ForkedFrameworkFactory wrapping a given OSGi FrameworkFactory and a given framework
     * storage directory
     *
     * @param frameworkFactory
     *            OSGi framework factory
     */
    public ForkedFrameworkFactory(FrameworkFactory frameworkFactory) {
        this.frameworkFactory = frameworkFactory;
    }

    public FrameworkFactory getFrameworkFactory() {
        return frameworkFactory;
    }

    public void setFrameworkFactory(FrameworkFactory frameworkFactory) {
        this.frameworkFactory = frameworkFactory;
    }

    /**
     * Forks a Java VM process running an OSGi framework and returns a {@link RemoteFramework}
     * handle to it.
     * <p>
     *
     * @param vmArgs
     *            VM arguments
     * @param systemProperties
     *            system properties for the forked Java VM
     * @param frameworkProperties
     *            framework properties for the remote framework
     * @param beforeFrameworkClasspath
     *            system classpath entries before the framework itself
     * @param afterFrameworkClasspath
     *            system classpath entries after the framework itself
     * @return remote framework
     */
    public RemoteFramework fork(List<String> vmArgs, Map<String, String> systemProperties,
        Map<String, Object> frameworkProperties, List<String> beforeFrameworkClasspath,
        List<String> afterFrameworkClasspath, String[] env) {
        // TODO make port range configurable
        FreePort freePort = new FreePort(21000, 21099);
        port = freePort.getPort();
        LOG.debug("using RMI registry at port {}", port);

        String rmiName = "ExamRemoteFramework-" + UUID.randomUUID().toString();

        try {
            String address = InetAddress.getLoopbackAddress().getHostAddress();
            System.setProperty("java.rmi.server.hostname", address);
            registry = LocateRegistry.createRegistry(port);

            Map<String, String> systemPropsNew = new HashMap<>(systemProperties);
            systemPropsNew.put("java.rmi.server.hostname", address);
            systemPropsNew.put(RemoteFramework.RMI_PORT_KEY, Integer.toString(port));
            systemPropsNew.put(RemoteFramework.RMI_NAME_KEY, rmiName);
            String[] vmOptions = buildSystemProperties(vmArgs, systemPropsNew);
            String[] args = buildFrameworkProperties(frameworkProperties);
            javaRunner = new ExamJavaRunner(false);
            javaRunner.exec(vmOptions, buildClasspath(beforeFrameworkClasspath, afterFrameworkClasspath),
                RemoteFrameworkImpl.class.getName(), args, getJavaHome(), null, env);
            return findRemoteFramework(port, rmiName);
        }
        catch (RemoteException | ExecutionException | URISyntaxException exc) {
            throw new TestContainerException(exc);
        }
    }

    /**
     * Forks a Java VM process running an OSGi framework and returns a {@link RemoteFramework}
     * handle to it.
     * <p>
     *
     * @param vmArgs
     *            VM arguments
     * @param systemProperties
     *            system properties for the forked Java VM
     * @param frameworkProperties
     *            framework properties for the remote framework
     * @return remote framework
     */
    public RemoteFramework fork(List<String> vmArgs, Map<String, String> systemProperties,
        Map<String, Object> frameworkProperties) {
        return fork(vmArgs, systemProperties, frameworkProperties, null, null, new String[0]);
    }

    private String[] buildSystemProperties(List<String> vmArgs, Map<String, String> systemProperties) {
        String[] vmOptions = new String[vmArgs.size() + systemProperties.size()];
        int i = 0;
        for (String vmArg : vmArgs) {
            vmOptions[i++] = vmArg;
        }
        for (Map.Entry<String, String> entry : systemProperties.entrySet()) {
            vmOptions[i++] = String.format("-D%s=%s", entry.getKey(), entry.getValue());
        }
        return vmOptions;
    }

    private String[] buildFrameworkProperties(Map<String, Object> frameworkProperties) {
        String[] args = new String[frameworkProperties.size()];
        int i = 0;
        for (Map.Entry<String, Object> entry : frameworkProperties.entrySet()) {
            args[i++] = String.format("-F%s=%s", entry.getKey(), entry.getValue().toString());
        }
        return args;
    }

    private String getJavaHome() {
        String javaHome = System.getenv("JAVA_HOME");
        if (javaHome == null) {
            javaHome = System.getProperty("java.home");
        }
        return javaHome;
    }

    private String[] buildClasspath(List<String> beforeFrameworkClasspath,
        List<String> afterFrameworkClasspath) throws URISyntaxException {
        String frameworkPath = toPath(frameworkFactory.getClass());
        String launcherPath = toPath(RemoteFrameworkImpl.class);
        String serviceLookupPath = toPath(ServiceLookup.class);

        int entries = (beforeFrameworkClasspath != null ? beforeFrameworkClasspath.size() : 0)
            + 3 + (afterFrameworkClasspath != null ? afterFrameworkClasspath.size() : 0);
        String[] classpath = new String[entries];
        int i = 0;
        if (beforeFrameworkClasspath != null) {
            for (String beforeFrameworkLibrary : beforeFrameworkClasspath) {
                if (!new File(beforeFrameworkLibrary).exists()) {
                    throw new TestContainerException(
                            "Invalid boot classpath library: " + beforeFrameworkLibrary);
                }
                classpath[i++] = beforeFrameworkLibrary;
            }
        }
        classpath[i++] = frameworkPath;
        classpath[i++] = launcherPath;
        classpath[i++] = serviceLookupPath;
        if (afterFrameworkClasspath != null) {
            for (String afterFrameworkLibrary : afterFrameworkClasspath) {
                if (!new File(afterFrameworkLibrary).exists()) {
                    throw new TestContainerException(
                            "Invalid boot classpath library: " + afterFrameworkLibrary);
                }
                classpath[i++] = afterFrameworkLibrary;
            }
        }
        return classpath;
    }

    static String toPath(Class<?> klass) throws URISyntaxException {
        return klass.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
    }

    private RemoteFramework findRemoteFramework(int _port, String rmiName) {
        RemoteFramework framework = null;
        Throwable reason = null;
        long startedTrying = System.currentTimeMillis();

        do {
            try {
                String address = InetAddress.getLoopbackAddress().getHostAddress();
                Registry reg = LocateRegistry.getRegistry(address, _port);
                framework = (RemoteFramework) reg.lookup(rmiName);
            }
            catch (RemoteException e) {
                reason = e;
            }
            catch (NotBoundException e) {
                reason = e;
            }
        }
        while (framework == null && System.currentTimeMillis() < startedTrying + TIMEOUT);
        if (framework == null) {
            throw new TestContainerException("cannot find remote framework in RMI registry", reason);
        }
        return framework;

    }

    /**
     * Waits for the remote framework to shutdown and frees all resources.
     */
    public void join() {
        try {
            UnicastRemoteObject.unexportObject(registry, true);
            /*
             * NOTE: javaRunner.waitForExit() works for Equinox and Felix, but not for Knopflerfish,
             * need to investigate why. OTOH, it may be better to kill the process as we're doing
             * now, just to be on the safe side.
             */
            javaRunner.shutdown();
        }
        catch (NoSuchObjectException exc) {
            throw new TestContainerException(exc);
        }
    }
}
