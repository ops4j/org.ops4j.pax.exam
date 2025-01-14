/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ops4j.pax.exam.regression.karaf;

import static org.ops4j.pax.exam.regression.karaf.RegressionConfiguration.regressionDefaults;

import java.io.File;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

@RunWith(PaxExam.class)
public class RemoveRuntimeFolderTest extends TestBase {

    private static File runtimeFolder;

    @Configuration
    public Option[] config() {
        return new Option[] { regressionDefaults(unpackDirectory()), };
    }

    @Test
    public void test() throws Exception {
        runtimeFolder = new File(".").getAbsoluteFile().getParentFile();
        Assert.assertTrue("Runtime folder should exist while test runs", runtimeFolder.exists());
        // TODO keepRuntimeFolder() option is configured in regressionDefaults()
        System.out.println("Please check manually that the folder "
            + runtimeFolder.getAbsolutePath() + " is deleted after this test");
    }

}
