/*
 * Copyright 2009 Toni Menzel.
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
package org.ops4j.pax.exam.raw.extender.intern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.ops4j.pax.swissbox.extender.BundleManifestScanner;
import org.ops4j.pax.swissbox.extender.BundleWatcher;
import org.ops4j.pax.swissbox.extender.ManifestEntry;
import org.ops4j.pax.swissbox.extender.RegexKeyManifestFilter;

/**
 * @author Toni Menzel
 * @since Dec 5, 2009
 */
public class Activator implements BundleActivator {

    private static final Log LOG = LogFactory.getLog( Activator.class );
    
    /**
     * Bundle watcher of web.xml.
     */
    private BundleWatcher<ManifestEntry> m_probeWatcher;

    /**
     * {@inheritDoc}
     */
    public void start( BundleContext bundleContext )
        throws Exception
    {
        BundleManifestScanner scanner = new BundleManifestScanner(
            new RegexKeyManifestFilter(
                "PaxExam-.*"
            )
        );
        m_probeWatcher = new BundleWatcher<ManifestEntry>(
            bundleContext,
            scanner,
            new TestBundleObserver()
        );
        m_probeWatcher.start();
        LOG.info("start probe watcher");
    }

    /**
     * {@inheritDoc}
     */
    public void stop( BundleContext bundleContext )
        throws Exception
    {
        m_probeWatcher.stop();
        LOG.info("stop probe watcher");
    }
}
