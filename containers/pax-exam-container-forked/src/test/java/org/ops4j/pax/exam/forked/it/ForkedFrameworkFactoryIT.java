/*
 * Copyright 2025 OPS4J
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
package org.ops4j.pax.exam.forked.it;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.UUID;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.util.PathUtils;
import org.ops4j.pax.swissbox.framework.RemoteFramework;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.keepCaches;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;
import static org.ops4j.pax.exam.CoreOptions.workingDirectory;
import static org.ops4j.pax.exam.util.NetUtils.findFreePort;

@RunWith(PaxExam.class)
public class ForkedFrameworkFactoryIT {

    private final String workingDirectory = String.format("%s/target/paxexam/%s/%s", PathUtils.getBaseDir(), getClass().getSimpleName(), UUID.randomUUID());

    @Configuration
    public Option[] configuration() {
        final String port = Integer.toString(findFreePort());
        final String name = String.format("remote-%s", UUID.randomUUID());
        return options(
            systemProperty(RemoteFramework.RMI_PORT_KEY).value(port),
            systemProperty(RemoteFramework.RMI_NAME_KEY).value(name),
            keepCaches(),
            workingDirectory(workingDirectory),
            junitBundles()
        );
    }

    @Test
    public void testRemote() throws RemoteException, NotBoundException {
        final String address = System.getProperty("java.rmi.server.hostname");
        final String port = System.getProperty(RemoteFramework.RMI_PORT_KEY);
        final String name = System.getProperty(RemoteFramework.RMI_NAME_KEY);
        final Registry registry = LocateRegistry.getRegistry(address, Integer.parseInt(port));
        final RemoteFramework remoteFramework = (RemoteFramework) registry.lookup(name);
        assertThat(remoteFramework, is(notNullValue()));
    }

}
