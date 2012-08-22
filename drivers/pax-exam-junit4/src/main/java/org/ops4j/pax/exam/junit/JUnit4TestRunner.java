/*
 * Copyright 2010 - 2011 Toni Menzel.
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
package org.ops4j.pax.exam.junit;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.internal.runners.model.ReflectiveCallable;
import org.junit.internal.runners.statements.Fail;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.ops4j.pax.exam.ConfigurationManager;
import org.ops4j.pax.exam.Constants;
import org.ops4j.pax.exam.ExamConfigurationException;
import org.ops4j.pax.exam.ExamSystem;
import org.ops4j.pax.exam.ExceptionHelper;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.TestAddress;
import org.ops4j.pax.exam.TestContainerException;
import org.ops4j.pax.exam.TestContainerFactory;
import org.ops4j.pax.exam.TestProbeBuilder;
import org.ops4j.pax.exam.spi.DefaultExamReactor;
import org.ops4j.pax.exam.spi.DefaultExamSystem;
import org.ops4j.pax.exam.spi.ExamReactor;
import org.ops4j.pax.exam.spi.PaxExamRuntime;
import org.ops4j.pax.exam.spi.StagedExamReactor;
import org.ops4j.pax.exam.spi.StagedExamReactorFactory;
import org.ops4j.pax.exam.spi.reactors.AllConfinedStagedReactorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the default Test Runner using the Exam plumbing API.
 * Its also the blueprint for custom, much more specific runners.
 * This will make a single probe bundling in all @Tests in this class.
 *
 * This uses the whole regression class as a single unit of tests with the following valid annotations:
 * - @Configuration -> Configuration 1:N. Multiple configurations will result in multiple invocations of the same regression.
 * - @ProbeBuilder -> Customize the probe creation.
 * - @Test -> Single tests to be invoked. Note that in @Configuration you can specify the invocation strategy.
 * 
 * @author Toni Menzel
 * @author Harald Wellmann
 */
public class JUnit4TestRunner extends BlockJUnit4ClassRunner {

    private static Logger LOG = LoggerFactory.getLogger( JUnit4TestRunner.class );

    final private StagedExamReactor m_reactor;
    final private Map<TestAddress, FrameworkMethod> m_map = new LinkedHashMap<TestAddress, FrameworkMethod>();
    final private Map<FrameworkMethod, TestAddress> m__childs = new LinkedHashMap<FrameworkMethod, TestAddress>();

	private ExamSystem m_system;

    public JUnit4TestRunner( Class<?> klass )
        throws Exception
    {
        super( klass );

        m_reactor = prepareReactor();
    }

    @Override
    public void run( RunNotifier notifier )
    {
        try {
            super.run( notifier );
        } catch( Exception e ) {
            throw new TestContainerException( "Problem interacting with reactor.", e );
        } finally {
            m_reactor.tearDown();
        }
    }

    /**
     * Override to avoid running BeforeClass and AfterClass by the driver.
     * They shall only be run by the container.
     */
    protected Statement classBlock(final RunNotifier notifier) {
        Statement statement= childrenInvoker(notifier);
        return statement;
    }

    /**
     * Override to avoid running Before, After and Rule methods by the driver.
     * They shall only be run by the container.
     */
    protected Statement methodBlock(FrameworkMethod method) {
        Object test;
        try {
            test= new ReflectiveCallable() {
                @Override
                protected Object runReflectiveCall() throws Throwable {
                    return createTest();
                }
            }.run();
        } catch (Throwable e) {
            return new Fail(e);
        }

        Statement statement= methodInvoker(method, test);
        return statement;
    }

    
    /**
     * We overwrite those with reactor content
     */
    @Override
    protected List<FrameworkMethod> getChildren()
    {
        if( m__childs.isEmpty() ) {
            fillChildren();
        }
        return Arrays.asList( m__childs.keySet().toArray( new FrameworkMethod[ m__childs.size() ] ) );
    }

    private void fillChildren()
    {
        Set<TestAddress> targets = m_reactor.getTargets();
        for( final TestAddress address : targets ) {
            final FrameworkMethod frameworkMethod = m_map.get( address.root() );

            // now, someone later may refer to that artificial FrameworkMethod. We need to be able to tell the address.
            FrameworkMethod method = new DecoratedFrameworkMethod( address, frameworkMethod );
            m__childs.put( method, address );
        }
    }

    @Override
    protected void collectInitializationErrors
        ( List<Throwable> errors )
    {
        // do nothing
    }

    private synchronized StagedExamReactor prepareReactor()
        throws Exception
    {
        ConfigurationManager cm = new ConfigurationManager();        
        String systemType = cm.getProperty( Constants.EXAM_SYSTEM_KEY );
        if( Constants.EXAM_SYSTEM_DEFAULT.equals( systemType ) )
        {
            m_system = DefaultExamSystem.create( new Option[0] );
        }
        else
        {
            m_system = PaxExamRuntime.createTestSystem();
        }
        Class<?> testClass = getTestClass().getJavaClass();
        Object testClassInstance = testClass.newInstance();
        ExamReactor reactor = getReactor( testClass );

        addConfigurationsToReactor( reactor, testClass, testClassInstance );
        addTestsToReactor( reactor, testClass, testClassInstance );
        return reactor.stage( getFactory( testClass ) );
    }

    private void addConfigurationsToReactor( ExamReactor reactor, Class<?> testClass, Object testClassInstance )
        throws IllegalAccessException, InvocationTargetException, IllegalArgumentException, IOException
    {
        Method[] methods = testClass.getMethods();
        for( Method m : methods ) {
            Configuration conf = m.getAnnotation( Configuration.class );
            if( conf != null ) {
                // consider as option, so prepare that one:
                reactor.addConfiguration( ( (Option[]) m.invoke( testClassInstance ) ) );
            }
        }
    }

    private void addTestsToReactor( ExamReactor reactor, Class<?> testClass, Object testClassInstance )
        throws IOException, ExamConfigurationException
    {
        TestProbeBuilder probe = m_system.createProbe(  );
        probe = overwriteWithUserDefinition( testClass, testClassInstance, probe );

        //probe.setAnchor( testClass );
        for( FrameworkMethod s : super.getChildren() ) {
            // record the method -> adress matching
            TestAddress address = delegateTest( testClassInstance, probe, s );
            if( address == null ) {
                address = probe.addTest( testClass, s.getMethod().getName() );
            }
            m_map.put( address, s );
        }
        reactor.addProbe( probe.build() );
    }

    private TestAddress delegateTest( Object testClassInstance, TestProbeBuilder probe, FrameworkMethod s )
    {
        try {
            Class<?>[] types = s.getMethod().getParameterTypes();
            if( types.length == 1 && types[ 0 ].isAssignableFrom( TestProbeBuilder.class ) ) {
                // do some backtracking:
                return (TestAddress) s.getMethod().invoke( testClassInstance, probe );

            }
            else {
                return null;
            }
        } catch( Exception e ) {
            throw new TestContainerException( "Problem delegating to test.", e );
        }
    }

    private StagedExamReactorFactory getFactory( Class<?> testClass )
        throws InstantiationException, IllegalAccessException
    {
        ExamReactorStrategy strategy = (ExamReactorStrategy) testClass.getAnnotation( ExamReactorStrategy.class );

        StagedExamReactorFactory fact;
        if( strategy != null ) {
            fact = strategy.value()[ 0 ].newInstance();
        }
        else {
            // default:
            fact = new AllConfinedStagedReactorFactory();
        }
        return fact;
    }

    private DefaultExamReactor getReactor( Class<?> testClass )
        throws InstantiationException, IllegalAccessException
    {
        return new DefaultExamReactor( m_system, getExamFactory( testClass ) );
    }

    private TestContainerFactory getExamFactory( Class<?> testClass )
        throws IllegalAccessException, InstantiationException
    {
        ExamFactory f = (ExamFactory) testClass.getAnnotation( ExamFactory.class );

        TestContainerFactory fact;
        if( f != null ) {
            fact = f.value().newInstance();
        }
        else {
            // default:
            fact = PaxExamRuntime.getTestContainerFactory();
        }
        return fact;
    }

    protected synchronized Statement methodInvoker( final FrameworkMethod method, final Object test )
    {
        return new Statement() {

            @Override
            public void evaluate()
                throws Throwable
            {
                TestAddress address = m__childs.get( method );
                TestAddress root = address.root();

                LOG.debug( "Invoke " + method.getName() + " @ " + address + " Arguments: " + root.arguments() );
                try {
                    m_reactor.invoke( address );
                } catch( Exception e ) {
                    Throwable t = ExceptionHelper.unwind( e );
                    throw t;
                }
            }
        };
    }

    @Override
    protected void validatePublicVoidNoArgMethods( Class<? extends Annotation> annotation, boolean isStatic, List<Throwable> errors )
    {

    }

    private TestProbeBuilder overwriteWithUserDefinition( Class<?> testClass, Object instance, TestProbeBuilder probe )
        throws ExamConfigurationException
    {
        Method[] methods = testClass.getMethods();
        for( Method m : methods ) {
            ProbeBuilder conf = m.getAnnotation( ProbeBuilder.class );
            if( conf != null ) {
                // consider as option, so prepare that one:
                LOG.debug( "User defined probe hook found: " + m.getName() );
                TestProbeBuilder probeBuilder;
                try {
                    probeBuilder = (TestProbeBuilder) m.invoke( instance, probe );
                } catch( Exception e ) {
                    throw new ExamConfigurationException( "Invoking custom probe hook " + m.getName() + " failed", e );
                }
                if( probeBuilder != null ) {
                    return probe;
                }
                else {
                    throw new ExamConfigurationException( "Invoking custom probe hook " + m.getName() + " succeeded but returned null" );
                }

            }
        }
        LOG.debug( "No User defined probe hook found" );
        return probe;
    }
}
