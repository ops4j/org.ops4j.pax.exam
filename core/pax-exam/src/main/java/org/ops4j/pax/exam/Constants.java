/*
 * Copyright 2009 Alin Dreghiciu.
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

/**
 * Pax Exam related constants.
 *
 * @author Alin Dreghiciu (adreghiciu@gmail.com)
 * @author Harald Wellmann
 * @since 0.5.0, April 22, 2009
 */
public class Constants {


    /**
     * The start level at which Pax Exam system bundles are to be started.
     */
    public static final int START_LEVEL_SYSTEM_BUNDLES = 2;

    /**
     * The start level at which Pax Exam test bundle is to be started.
     */
    public static final int START_LEVEL_DEFAULT_PROVISION = 3;

    /**
     * The start level at which Pax Exam test bundle is to be started. This is also the startlevel,
     * the test container reaches in start.
     */
    public static final int START_LEVEL_TEST_BUNDLE = 5;

    /**
     * Timeout specifing that there should be no waiting.
     */
    public static final long NO_WAIT = 0;
    /**
     * Timeout specifing that it should wait forever.
     */
    public static final long WAIT_FOREVER = Long.MAX_VALUE;
    /**
     * Timeout specifing that it should wait .
     */
    public static final long WAIT_5_MINUTES = 5 * 60 * 1000;

    /**
     * Manifest header specifying the string of executable services.
     */
    public static final String PROBE_EXECUTABLE = "PaxExam-Executable";

    /** Name of configuration properties file. */
    public static final String EXAM_PROPERTIES_FILE = "exam.properties";

    /** Resource path of configuration properties file. */
    public static final String EXAM_PROPERTIES_PATH = "/" + EXAM_PROPERTIES_FILE;

    /** Configuration key for Exam system type. */
    public static final String EXAM_SYSTEM_KEY = "pax.exam.system";

    /**
     * URL of configuration properties, overriding the default classpath:/exam.properties. If you
     * want this to be a plain old file, make sure to include the {@code file:} protocol.
     */
    public static final String EXAM_CONFIGURATION_KEY = "pax.exam.configuration";

    /** Default exam system with no predefined options. */
    public static final String EXAM_SYSTEM_DEFAULT = "default";

    /** Exam system for Java EE containers. */
    public static final String EXAM_SYSTEM_JAVAEE = "javaee";

    /** Exam system for CDI containers. */
    public static final String EXAM_SYSTEM_CDI = "cdi";

    /** Test exam system with predefined options for Exam's own bundles etc. */
    public static final String EXAM_SYSTEM_TEST = "test";

    /**
     * Default reactor strategy. Legal values are {@code PerSuite, PerClass, PerMethod}.
     */
    public static final String EXAM_REACTOR_STRATEGY_KEY = "pax.exam.reactor.strategy";

    public static final String EXAM_REACTOR_STRATEGY_PER_SUITE = "PerSuite";
    public static final String EXAM_REACTOR_STRATEGY_PER_CLASS = "PerClass";
    public static final String EXAM_REACTOR_STRATEGY_PER_METHOD = "PerMethod";

    /**
     * Timeout for service lookup in milliseconds.
     */
    public static final String EXAM_SERVICE_TIMEOUT_KEY = "pax.exam.service.timeout";

    /**
     * Default value for service lookup timeout (10 seconds).
     */
    public static final String EXAM_SERVICE_TIMEOUT_DEFAULT = "10000";

    /**
     * The logging system to be provisioned by Pax Exam.
     */
    public static final String EXAM_LOGGING_KEY = "pax.exam.logging";

    /**
     * Provision Pax Logging as logging system. (Default value).
     */
    public static final String EXAM_LOGGING_PAX_LOGGING = "pax-logging";

    /**
     * Do not provision any logging system and leave it to the user.
     */
    public static final String EXAM_LOGGING_NONE = "none";

    /**
     * Should Pax Exam fail the test if any of the provisioned bundles is unresolved?
     * Values: true | false.
     */
    public static final String EXAM_FAIL_ON_UNRESOLVED_KEY = "pax.exam.osgi.unresolved.fail";

    /**
     * Port for socket-based communication with remote invoker.
     */
    public static final String EXAM_INVOKER_PORT = "pax.exam.invoker.port";

    /**
     * Lower bound port range for socket-based communication with remote invoker.
     */
    public static final String EXAM_INVOKER_PORT_RANGE_LOWERBOUND = "pax.exam.invoker.port.range.lowerbound";

    /**
     * Upper bound port range for socket-based communication with remote invoker.
     */
    public static final String EXAM_INVOKER_PORT_RANGE_UPPERBOUND = "pax.exam.invoker.port.range.upperbound";

    /**
     * Port for socket-based communication with remote invoker.
     */
    public static final String EXAM_FORKED_INVOKER_PORT = "pax.exam.forked.invoker.port";

    /**
     * Lower bound port range for socket-based communication with forked remote invoker.
     */
    public static final String EXAM_FORKED_INVOKER_PORT_RANGE_LOWERBOUND = "pax.exam.forked.invoker.port.range.lowerbound";

    /**
     * Upper bound port range for socket-based communication with forked remote invoker.
     */
    public static final String EXAM_FORKED_INVOKER_PORT_RANGE_UPPERBOUND = "pax.exam.forked.invoker.port.range.upperbound";

    /** Hidden utility class constructor. */
    private Constants() {
    }

}
