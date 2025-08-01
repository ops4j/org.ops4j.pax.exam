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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.sample9.pde.HelloService;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.BundleContext;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class BeforeAfterTest extends BeforeAfterParent {

    @Inject
    private BundleContext bundleContext;

    @Inject
    private HelloService helloService;

    @Before
    public void setUp() {
        Messages.addMessage("Before");
        assertThat(bundleContext, is(notNullValue()));
    }

    @After
    public void tearDown() {
        Messages.addMessage("After");
        assertThat(bundleContext, is(notNullValue()));
    }

    @Test
    public void getInjectedService() {
        Messages.addMessage("Test");
        assertThat(helloService, is(notNullValue()));
        assertThat(helloService.getMessage(), is(equalTo("Hello Pax!")));
    }

    @Test
    public void injectedBundleContext() {
        Messages.addMessage("Test");
        assertThat(bundleContext, is(notNullValue()));
    }
}
