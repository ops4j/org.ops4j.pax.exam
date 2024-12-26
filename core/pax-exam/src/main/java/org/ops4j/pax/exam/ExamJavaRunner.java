/*
 * Copyright 2008 Alin Dreghiciu.
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
package org.ops4j.pax.exam;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.ops4j.exec.CommandLineBuilder;
import org.ops4j.exec.ExecutionException;
import org.ops4j.exec.ProcessProvider;
import org.ops4j.exec.StoppableJavaRunner;
import org.ops4j.io.Pipe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Exam Java Runner, adjusted copy of Default Java Runner (see PAXEXAM-920)
 *
 * @author Alin Dreghiciu (adreghiciu@gmail.com)
 * @author Harald Wellmann
 * @since 0.6.1, December 09, 2008
 */
public class ExamJavaRunner implements StoppableJavaRunner, ProcessProvider {

    /**
     * If the execution should wait for platform shutdown.
     */
    private final boolean m_wait;

    /**
     * Framework process.
     */
    private Process m_frameworkProcess;

    /**
     * Shutdown hook.
     */
    private Thread m_shutdownHook;

    private static final Logger LOG = LoggerFactory.getLogger(ExamJavaRunner.class);

    /**
     * Constructor.
     */
    public ExamJavaRunner() {
        this(true);
    }

    /**
     * Constructor.
     *
     * @param wait should wait for framework exis
     */
    public ExamJavaRunner(boolean wait) {
        m_wait = wait;
    }

    public synchronized void exec(final String[] vmOptions,
                                  final String[] classpath,
                                  final String mainClass,
                                  final String[] programOptions,
                                  final String javaHome,
                                  final File workingDirectory)
        throws ExecutionException {
        exec(vmOptions, classpath, mainClass, programOptions, javaHome, workingDirectory, new String[0]);
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void exec(final String[] vmOptions,
                                  final String[] classpath,
                                  final String mainClass,
                                  final String[] programOptions,
                                  final String javaHome,
                                  final File workingDirectory,
                                  final String[] envOptions)
        throws ExecutionException {
        if (m_frameworkProcess != null) {
            throw new ExecutionException("Platform already started");
        }

        String cp = String.join(File.pathSeparator, classpath);

        final CommandLineBuilder commandLine = new CommandLineBuilder()
            .append(getJavaExecutable(javaHome))
            .append(vmOptions)
            .append("-cp")
            .append(cp)
            .append(mainClass)
            .append(programOptions);

        LOG.debug("Start command line [" + Arrays.toString(commandLine.toArray()) + "]");

        try {
            LOG.debug("Starting platform process.");
            ProcessBuilder pb = new ProcessBuilder(commandLine.toArray())
                .directory(workingDirectory);
            setEnv(envOptions, pb);
            m_frameworkProcess = pb.start();
        } catch (IOException e) {
            throw new ExecutionException("Could not start up the process", e);
        }

        m_shutdownHook = createShutdownHook(m_frameworkProcess);
        Runtime.getRuntime().addShutdownHook(m_shutdownHook);

        LOG.debug("Added shutdown hook.");
        LOG.info("ExamJavaRunner completed successfully");

        if (m_wait) {
            waitForExit();
        } else {
            System.out.println();
        }
    }

    private void setEnv(final String[] envOptions, ProcessBuilder pb) {
        final Map<String, String> envMap = toKeyValueMap(envOptions);

        final Map<String, String> env = pb.environment();

        // Remove all items from the ProcessBuilder's environment,
        // that are not explicitly referenced in the envMap's keyset
        final Set<String> toRemove = new HashSet<String>(env.keySet());
        toRemove.removeAll(envMap.keySet());
        env.keySet().removeAll(toRemove);

        // Any supplied values should be applied, possibly overriding an initial value from the ProcessBuilder.
        envMap.entrySet().stream()
                .filter(entry -> entry.getValue() != null)
                .forEach(entry -> env.put(entry.getKey(), entry.getValue()));
    }

    /**
     * Converts an array of strings in the format "key=value" into a map of key-value pairs.
     * If a string does not contain an '=' character, the entire string is treated as the key,
     * and the corresponding value is set to {@code null}.
     *
     * @param envOptions an array of strings, each representing an environment option in the
     *                   format "key=value". Strings without an '=' will have {@code null} as the value.
     * @return a {@code Map<String, String>} where each key-value pair is derived from the input array.
     *         Keys are extracted from the portion of the string before the first '=' character,
     *         and values are extracted from the portion after the '=' character. If no '=' is
     *         present, the value is set to {@code null}.
     */
    private static Map<String, String> toKeyValueMap(String[] envOptions) {
        final Map<String, String> envMap = new HashMap<>();
        for (String envOption : envOptions) {
            if (envOption == null || envOption.isEmpty()) {
                throw new IllegalArgumentException("Null or empty entry in envOptions");
            }
            final int equalsPosition = envOption.indexOf('=');
            final String key;
            final String value;
            if (equalsPosition < 0) {
                key = envOption.trim();
                value = null;
            } else {
                key = envOption.substring(0, equalsPosition).trim();
                value = envOption.substring(equalsPosition + 1).trim();
            }
            if (key.isEmpty()) {
                throw new IllegalArgumentException("Input " + envOption + " resulted in an empty key");
            }
            if (envMap.containsKey(key)) {
                throw new IllegalArgumentException("Duplicate key found: " + key);
            }
            envMap.put(key, value);
        }
        return envMap;
    }

    /**
     * {@inheritDoc}
     */
    public void shutdown() {
        try {
            if (m_shutdownHook != null) {
                synchronized (m_shutdownHook) {
                    if (m_shutdownHook != null) {
                        LOG.debug("Shutdown in progress...");
                        Runtime.getRuntime().removeShutdownHook(m_shutdownHook);
                        m_frameworkProcess = null;
                        m_shutdownHook.run();
                        m_shutdownHook = null;
                        LOG.info("Platform has been shutdown.");
                    }
                }
            }
        } catch (IllegalStateException ignore) {
            // just ignore
        }
    }

    /**
     * Wait till the framework process exits.
     */
    public void waitForExit() {
        synchronized (m_frameworkProcess) {
            try {
                LOG.debug("Waiting for framework exit.");
                System.out.println();
                m_frameworkProcess.waitFor();
                shutdown();
            } catch (Throwable e) {
                LOG.debug("Early shutdown.", e);
                shutdown();
            }
        }
    }

    /**
     * Create helper thread to safely shutdown the external framework process
     *
     * @param process framework process
     * @return stream handler
     */
    private Thread createShutdownHook(final Process process) {
        LOG.debug("Wrapping stream I/O.");

        final Pipe errPipe = new Pipe(process.getErrorStream(), System.err).start("Error pipe");
        final Pipe outPipe = new Pipe(process.getInputStream(), System.out).start("Out pipe");

        return new Thread(
            new Runnable() {
                public void run() {
                    System.out.println();
                    LOG.debug("Unwrapping stream I/O.");

                    // inPipe.stop();
                    outPipe.stop();
                    errPipe.stop();

                    try {
                        process.destroy();
                    } catch (Exception e) {
                        // ignore if already shutting down
                    }
                }
            },
            "ExamJavaRunner shutdown hook"
        );
    }

    /**
     * Return path to java executable.
     *
     * @param javaHome java home directory
     * @return path to java executable
     * @throws ExecutionException if java home could not be located
     */
    static String getJavaExecutable(final String javaHome)
        throws ExecutionException {
        if (javaHome == null) {
            throw new ExecutionException("JAVA_HOME is not set.");
        }
        return javaHome + "/bin/java";
    }

    @Override
    public Process getProcess() {
        return m_frameworkProcess;
    }

}
