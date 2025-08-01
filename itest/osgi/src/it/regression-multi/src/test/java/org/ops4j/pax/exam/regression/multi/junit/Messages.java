/*
 * Copyright (C) 2025 OPS4J
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ops4j.pax.exam.regression.multi.junit;

import org.h2.mvstore.MVMap;
import org.h2.mvstore.MVStore;
import org.ops4j.pax.exam.util.PathUtils;

public class Messages {

    private static final String FILE_NAME = String.format("%s/target/messages", PathUtils.getBaseDir());

    private static final String MAP_NAME = "messages";

    public static void clearMessages() {
        try (MVStore store = MVStore.open(FILE_NAME)) {
            store.openMap(MAP_NAME).clear();
        }
    }

    public static void addMessage(final String message) {
        try (MVStore store = MVStore.open(FILE_NAME)) {
            final MVMap<Integer, String> map = store.openMap(MAP_NAME);
            map.put(map.size(), message);
        }
    }

    public static String getMessage(final int key) {
        try (MVStore store = MVStore.open(FILE_NAME)) {
            final MVMap<Integer, String> map = store.openMap(MAP_NAME);
            return map.get(key);
        }
    }

    public static int countMessages() {
        try (MVStore store = MVStore.open(FILE_NAME)) {
            return store.openMap(MAP_NAME).size();
        }
    }

}
