/*
 * Copyright 2017 Oliver Lietz.
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
package org.ops4j.pax.exam.karaf.container.internal;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

public abstract class KarafConfigurationFile {

    protected final File file;

    public KarafConfigurationFile(final File karafHome, final String location) {
        if (location.startsWith("/")) {
            file = new File(karafHome + location);
        }
        else {
            file = new File(karafHome + "/" + location);
        }
    }

    public boolean exists() {
        return file.exists();
    }

    public void replace(final File source) {
        try {
            FileUtils.copyFile(source, file);
        }
        catch (IOException e) {
            throw new IllegalStateException("Error occurred while replacing file " + file, e);
        }
    }

    public abstract void store() throws IOException;

    public abstract void load() throws IOException;

    public abstract void put(final String key, final Object value);

    public abstract void extend(final String key, final Object value);

    public abstract Object get(final String key);

}
