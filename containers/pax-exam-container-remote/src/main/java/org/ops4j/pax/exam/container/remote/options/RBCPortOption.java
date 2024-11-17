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
package org.ops4j.pax.exam.container.remote.options;

import java.util.Objects;

import org.ops4j.pax.exam.Option;

/**
 * @author Toni Menzel
 * @since Jan 25, 2010
 */
public class RBCPortOption implements Option {

    private Integer port;
    private String host;

    public RBCPortOption(String host, Integer port) {
        Objects.requireNonNull(host, "Host must not be be null.");
        Objects.requireNonNull(port, "Port must not be be null.");

        this.host = host;
        this.port = port;
    }

    public String getHost() {
        return host;
    }

    public Integer getPort() {
        return port;
    }
}
