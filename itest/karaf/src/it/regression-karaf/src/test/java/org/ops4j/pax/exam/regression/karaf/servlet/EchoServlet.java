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

package org.ops4j.pax.exam.regression.karaf.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;

@Component(
    service = {
        Servlet.class
    },
    property = {
        HttpWhiteboardConstants.HTTP_WHITEBOARD_SERVLET_PATTERN + "=/test/services"
    }
)
public class EchoServlet extends HttpServlet {

    // now we just send a echo response back
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException,
        IOException {
        copy(request.getInputStream(), response.getOutputStream(), 4096);
    }

    public static int copy(final InputStream input, final OutputStream output, final int bufferSize) throws IOException {
        int avail = input.available();
        if (avail > 262144) {
            avail = 262144;
        }
        final byte[] buffer;
        if (avail > bufferSize) {
            buffer = new byte[avail];
        } else {
            buffer = new byte[bufferSize];
        }

        int n = input.read(buffer);
        int total = 0;
        while (-1 != n) {
            output.write(buffer, 0, n);
            total += n;
            n = input.read(buffer);
        }
        output.flush();
        return total;
    }
}
