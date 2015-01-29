/*
 * Copyright 2012 Harald Wellmann
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.ops4j.pax.exam.regression.multi.server;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;
import static org.ops4j.pax.exam.CoreOptions.frameworkProperty;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.regression.multi.RegressionConfiguration.isKnopflerfish;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;

import org.junit.Rule;
import org.junit.Test;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExamServer;

public class WabSampleTest {

    @Rule
    public PaxExamServer exam = new PaxExamServer();
    
    private String port = System.getProperty("pax.exam.itest.http.port", "18181");


    @Configuration
    public Option[] configuration() {
        return options(frameworkProperty("felix.bootdelegation.implicit").value("false"),
            frameworkProperty("osgi.console").value("6666"),
            frameworkProperty("org.osgi.service.http.port").value(port),

            mavenBundle("org.ops4j.pax.web", "pax-web-spi").version("2.0.2"),
            mavenBundle("org.ops4j.pax.web", "pax-web-api").version("2.0.2"),
            mavenBundle("org.ops4j.pax.web", "pax-web-extender-war").version("2.0.2"),
            mavenBundle("org.ops4j.pax.web", "pax-web-extender-whiteboard").version("2.0.2"),
            mavenBundle("org.ops4j.pax.web", "pax-web-jetty").version("2.0.2"),
            mavenBundle("org.ops4j.pax.web", "pax-web-runtime").version("2.0.2"),
            mavenBundle("org.ops4j.pax.web", "pax-web-jsp").version("2.0.2"),
            mavenBundle("org.eclipse.jdt.core.compiler", "ecj").version("3.5.1"),
            mavenBundle("org.eclipse.jetty", "jetty-util").version("8.1.4.v20120524"),
            mavenBundle("org.eclipse.jetty", "jetty-io").version("8.1.4.v20120524"),
            mavenBundle("org.eclipse.jetty", "jetty-http").version("8.1.4.v20120524"),
            mavenBundle("org.eclipse.jetty", "jetty-continuation").version("8.1.4.v20120524"),
            mavenBundle("org.eclipse.jetty", "jetty-server").version("8.1.4.v20120524"),
            mavenBundle("org.eclipse.jetty", "jetty-security").version("8.1.4.v20120524"),
            mavenBundle("org.eclipse.jetty", "jetty-xml").version("8.1.4.v20120524"),
            mavenBundle("org.eclipse.jetty", "jetty-servlet").version("8.1.4.v20120524"),
            mavenBundle("org.apache.geronimo.specs", "geronimo-servlet_3.0_spec").version("1.0"),
            mavenBundle("org.osgi", "org.osgi.compendium", "4.3.0"),

            mavenBundle("org.slf4j", "slf4j-api", "1.6.4"),
            mavenBundle("org.slf4j", "slf4j-simple", "1.6.4").noStart(),

            mavenBundle("org.ops4j.pax.exam.samples", "pax-exam-sample11-wab").versionAsInProject()

        );
    }

    @Test
    public void checkPlainTextFromWabServlet() {
        assumeTrue(!isKnopflerfish());
        Client client = ClientBuilder.newClient();
        String url = String.format("http://localhost:%s/wab/WABServlet", port);
        WebTarget resource = client.target(url);
        String response = resource.request().get(String.class);
        assertThat(response, containsString("wab symbolic name : org.ops4j.pax.exam.samples.pax-exam-sample11-wab"));
    }
}
