/*
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
package org.ops4j.pax.exam.container.eclipse.impl.repository;

import org.ops4j.pax.exam.container.eclipse.EclipseVersionedArtifact;

/**
 * Extension of versioned artefact that is classified by the repository
 * 
 * @author Christoph Läubrich
 *
 */
public interface EclipseClassifiedVersionedArtifact extends EclipseVersionedArtifact {

    public static final String CLASSIFIER_BUNDLE = "osgi.bundle";
    public static final String CLASSIFIER_FEATURE = "org.eclipse.update.feature";

    String getClassifier();
}
