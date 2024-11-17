/*
 * Copyright (C) 2010 Toni Menzel
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
package org.ops4j.pax.exam.container.remote;

import java.util.Objects;

import static org.ops4j.pax.exam.OptionUtils.filter;

import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.container.remote.options.RBCLookupTimeoutOption;
import org.ops4j.pax.exam.container.remote.options.RBCPortOption;

/**
 * Minimal parser for the rbcremote fragment.
 */
public class Parser {

    private String host;

    private Integer port;

    private long timeout;

    public Parser(Option[] options) {
        extractArguments(filter(RBCPortOption.class, options));
        extractArguments(filter(RBCLookupTimeoutOption.class, options));
        Objects.requireNonNull(host, "Host must not be be null.");
        Objects.requireNonNull(port, "Port must not be be null.");

    }

    private void extractArguments(RBCLookupTimeoutOption[] options) {
        for (RBCLookupTimeoutOption op : options) {
            timeout = op.getTimeout();
        }
    }

    private void extractArguments(RBCPortOption[] rbcPortOptions) {
        for (RBCPortOption op : rbcPortOptions) {
            host = op.getHost();
            port = op.getPort();
        }
    }

    public String getHost() {
        return host;
    }

    public Integer getRMIPort() {
        return port;
    }

    public long getRMILookupTimpout() {
        return timeout;
    }

    public Integer getPort() {
        return port;
    }
}
