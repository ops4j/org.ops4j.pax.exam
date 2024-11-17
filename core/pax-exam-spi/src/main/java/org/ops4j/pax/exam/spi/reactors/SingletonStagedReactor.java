/*
 * Copyright 2012 Harald Wellmann.
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
package org.ops4j.pax.exam.spi.reactors;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.ops4j.pax.exam.TestAddress;
import org.ops4j.pax.exam.TestContainer;
import org.ops4j.pax.exam.TestContainerException;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.spi.StagedExamReactor;
import org.ops4j.pax.exam.spi.intern.DefaultTestAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A singleton reactor which starts the container and installs the probes just once. The teardown
 * logic is postponed to a JVM shutdown hook.
 * 
 * @author Harald Wellmann
 * @since 3.0.0
 */
public class SingletonStagedReactor implements StagedExamReactor {

    private static final Logger LOG = LoggerFactory.getLogger(SingletonStagedReactor.class);

    private static SingletonStagedReactor instance;

    private List<TestContainer> testContainers;
    private List<TestProbeBuilder> probes;
    private Map<TestAddress, TestContainer> testToContainerMap;

    private SingletonStagedReactor(List<TestContainer> containers, List<TestProbeBuilder> mProbes) {
        testToContainerMap = new LinkedHashMap<TestAddress, TestContainer>();
        testContainers = containers;
        probes = mProbes;
    }

    private void buildTestMap(List<TestContainer> containers, List<TestProbeBuilder> mProbes) {
        int index = 0;
        for (TestContainer container : containers) {
            String caption = buildCaption(containers, container, index);
            for (TestProbeBuilder builder : mProbes) {
                // each probe has addresses.
                for (TestAddress a : builder.getTests()) {
                    // we need to create a new, because "a" exists for each test container
                    // this new address makes the test (reachable via getTargets() ) reachable
                    // directly.
                    testToContainerMap.put(new DefaultTestAddress(a, caption), container);
                }
            }
            index++;
        }
    }

    private String buildCaption(List<TestContainer> containers, TestContainer container, int index) {
        if (containers.size() == 1) {
            return container.toString();
        }
        else {
            return String.format("%s[%d]", container.toString(), index);
        }
    }

    /**
     * @param containers
     *            to be used
     * @param mProbes
     *            to be installed on all probes
     * @return staged reactor           
     */
    public static synchronized StagedExamReactor getInstance(List<TestContainer> containers,
        List<TestProbeBuilder> mProbes) {
        if (instance == null) {
            instance = new SingletonStagedReactor(containers, mProbes);
        }
        else {
            if ( /* ! instance.testContainers.equals( containers ) || */
            !instance.probes.equals(mProbes)) {
                throw new TestContainerException(
                    "using the PerSuite reactor strategy, all test classes must share the same probes");
            }
        }
        return instance;
    }

    public void invoke(TestAddress address) throws Exception {
        Objects.requireNonNull(address, "TestAddress must not be null.");

        TestContainer testContainer = testToContainerMap.get(address);
        if (testContainer == null) {
            throw new IllegalArgumentException("TestAddress " + address
                + " not from this reactor? Got it from getTargets() really?");
        }
        testContainer.call(address);
    }

    public Set<TestAddress> getTargets() {
        buildTestMap(testContainers, probes);
        return testToContainerMap.keySet();
    }

    public void tearDown() {
    }

    @Override
    public void beforeSuite() {
        for (TestContainer container : testContainers) {
            container.start();

            for (TestProbeBuilder builder : probes) {
                LOG.debug("installing probe " + builder);

                try {
                    container.installProbe(builder.build().getStream());
                }
                catch (IOException e) {
                    throw new TestContainerException("Unable to build the probe.", e);
                }
            }
        }
    }

    public void afterSuite() {
        for (TestContainer container : testContainers) {
            container.stop();
        }
    }

    @Override
    public void beforeClass() {

    }

    @Override
    public void afterClass() {
    }
}
