/*
 * Copyright 2008 Toni Menzel.
 * Copyright 2008 Alin Dreghiciu.
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
package org.ops4j.pax.exam.container.def.options;

import static org.ops4j.lang.NullArgumentException.*;

/**
 * {@link RepositoryOption} implementations.
 * This is the implementation that bridges nice api with actual string representation in pax runner.
 *
 * @author Toni Menzel (tonit)
 * @author Alin Dreghiciu (adreghiciu@gmail.com)
 * @since 0.3.0, December 19, 2008
 */
public class RepositoryOptionImpl implements RepositoryOption
{

    /**
     * Repository url (cannot be null or empty).
     */
    private final String m_repositoryUrl;
    /**
     * Marks repository as allowing snapshots.
     */
    private boolean m_allowSnapshots;
    /**
     * MArks repository as allowing releases.
     */
    private boolean m_allowReleases;

    /**
     * Constructor.
     *
     * @param repositoryUrl repository url (cannot be null or empty)
     *
     * @throws IllegalArgumentException - If repository url is null or empty
     */
    public RepositoryOptionImpl( final String repositoryUrl )
    {
        validateNotEmpty( repositoryUrl, "Repository URL" );

        m_repositoryUrl = repositoryUrl;
        m_allowSnapshots = false;
        m_allowReleases = true;
    }

    /**
     * {@inheritDoc}
     */
    public RepositoryOptionImpl allowSnapshots()
    {
        m_allowSnapshots = true;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    public RepositoryOptionImpl disableReleases()
    {
        m_allowReleases = false;
        return this;
    }

    /**
     * Returns the full repository url.
     *
     * @return the full repository as given plus eventual snapshot/release tags (cannot be null or empty)
     *
     * @throws IllegalStateException - if both snapshots and releases are not allowed
     */
    public String getRepository()
    {
        if( !m_allowReleases && !m_allowSnapshots )
        {
            throw new IllegalStateException( "Does not make sense to disallow both releases and snapshots." );
        }
        final StringBuilder repositoryUrl = new StringBuilder();
        repositoryUrl.append( m_repositoryUrl );
        if( m_allowSnapshots )
        {
            repositoryUrl.append( "@snapshots" );
        }
        if( !m_allowReleases )
        {
            repositoryUrl.append( "@noreleases" );
        }
        return repositoryUrl.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        final StringBuilder sb = new StringBuilder();
        sb.append( "RepositoryOptionImpl" );
        sb.append( "{url=" ).append( getRepository() );
        sb.append( '}' );
        return sb.toString();
    }
}
