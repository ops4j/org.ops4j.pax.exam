/*
 * Copyright (C) 2011 Harald Wellmann
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.exam.regression.multi.junit;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;

import org.junit.Assume;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

/**
 * To check that Pax Exam invokes {@code @Before} and {@code @After} methods in the correct order,
 * we need another level of indirection as Pax Exam cannot do checks on itself.
 * <p>
 * This test invokes JUnit to run a Pax Exam test. H2 MVStore is used as a simple
 * communication channel between the inner and the outer JUnit instance. Methods invoked by inner
 * methods append messages to the stored map.
 * <p>
 * The outer test checks that expected messages were appended in the correct order.
 * 
 * @author Harald Wellmann
 * 
 */
public class BeforeAfterInvokerTest {

    @Test
    public void beforeAfterMethodInvocationOrder() {
        Assume.assumeThat(System.getProperty("pax.exam.container"), is("native"));

        Messages.clearMessages();

        JUnitCore junit = new JUnitCore();
        Result run = junit.run(BeforeAfterTest.class);
        assertEquals(0, run.getFailureCount());

        int numMessages = Messages.countMessages();
        int messageNum = 0;

        // 2 tests * 1 framework * 5 messages per test
        assertEquals(10, numMessages);

        for (int testNum = 0; testNum < 2; testNum++) {
            assertMessage("Before in parent", messageNum++);
            assertMessage("Before", messageNum++);
            assertMessage("Test", messageNum++);
            assertMessage("After", messageNum++);
            assertMessage("After in parent", messageNum++);
        }
    }

    private void assertMessage(String expected, int messageNum) {
        final String message = Messages.getMessage(messageNum);
        assertEquals(expected, message);
    }
}
