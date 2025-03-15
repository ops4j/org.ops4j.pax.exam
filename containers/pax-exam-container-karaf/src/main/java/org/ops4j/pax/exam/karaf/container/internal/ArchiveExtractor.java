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
package org.ops4j.pax.exam.karaf.container.internal;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.io.FileUtils;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extract zip and tar.gz archives to a target folder
 */
public class ArchiveExtractor {

    private static final String ZIP_MIMETYPE = "application/zip";

    private static final String GZIP_MIMETYPE = "application/gzip";

    private static final Logger logger = LoggerFactory.getLogger(ArchiveExtractor.class);

    private ArchiveExtractor() {
    }

    /**
     * Extract archives to a target directory
     *
     * @param sourceURL url of the archive to extract
     * @param targetDir where to extract to
     * @throws IOException on I/O error
     */
    public static void extract(final URL sourceURL, final File targetDir) throws IOException {
        logger.info("extracting {} to {}", sourceURL, targetDir);

        final Tika tika = new Tika();
        final String mimeType = tika.detect(sourceURL);

        if (ZIP_MIMETYPE.equals(mimeType)) {
            extractZip(sourceURL, targetDir);
        } else if (GZIP_MIMETYPE.equals(mimeType)) { // could be less naive; if mime type application/gzip is detected, we assume a tar inside
            extractTarGz(sourceURL, targetDir);
        } else {
            throw new IllegalStateException(String.format("Unknown packaging (%s); only zip and tar.gz can be handled.", sourceURL));
        }
    }

    private static void extractTarGz(final URL sourceURL, final File targetFolder) throws IOException {
        try (InputStream buffer = new BufferedInputStream(sourceURL.openStream());
             GzipCompressorInputStream gzip = new GzipCompressorInputStream(buffer);
             TarArchiveInputStream archive = new TarArchiveInputStream(gzip)) {
            extract(archive, targetFolder);
        }
    }

    private static void extractZip(final URL sourceURL, final File targetFolder) throws IOException {
        try (InputStream buffer = new BufferedInputStream(sourceURL.openStream());
             ZipArchiveInputStream archive = new ZipArchiveInputStream(buffer)) {
            extract(archive, targetFolder);
        }
    }

    private static void extract(final ArchiveInputStream<? extends ArchiveEntry> is, final File targetDir) throws IOException {
        if (targetDir.exists()) {
            FileUtils.forceDelete(targetDir);
        }
        targetDir.mkdirs();
        ArchiveEntry entry = is.getNextEntry();
        while (entry != null) {
            logger.trace("processing archive entry {}", entry.getName());
            String name = entry.getName();
            name = name.substring(name.indexOf("/") + 1);
            final File file = new File(targetDir, name);
            if (entry.isDirectory()) {
                file.mkdirs();
            } else {
                file.getParentFile().mkdirs();
                final long number = Files.copy(is, file.toPath(), StandardCopyOption.REPLACE_EXISTING);
                logger.trace("copied {} bytes", number);
            }
            entry = is.getNextEntry();
        }
    }

}
