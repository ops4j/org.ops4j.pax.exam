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
package org.ops4j.pax.exam.testng.servlet;

import java.util.Collections;
import java.util.List;

import org.testng.annotations.Test;
import org.testng.TestListenerAdapter;
import org.testng.TestNG;
import org.testng.xml.XmlClass;
import org.testng.xml.XmlInclude;
import org.testng.xml.XmlSuite;
import org.testng.xml.XmlTest;

public class TestNGRunnerTest
{

    @SuppressWarnings( "rawtypes" )
    @Test
    public void runSingleMethod()
    {
        TestNG testNG = new TestNG();
        testNG.setListenerClasses(Collections.emptyList());
        TestListenerAdapter listener = new TestListenerAdapter();
        testNG.addListener( listener );
        XmlSuite suite = new XmlSuite();
        suite.setName( "Pax Exam Suite" );
        XmlTest xmlTest = new XmlTest(suite);
        xmlTest.setName( "Pax Exam Test" );
        xmlTest.setVerbose( 0 );
        XmlClass xmlClass = new XmlClass(SimpleTest.class);
        xmlTest.getClasses().add(xmlClass);
        XmlInclude xmlInclude = new XmlInclude("checkNewList");
        xmlClass.getIncludedMethods().add(xmlInclude);
        

        testNG.setXmlSuites(List.of(suite));
        testNG.setUseDefaultListeners( false );
        testNG.run();
        
    }
}
