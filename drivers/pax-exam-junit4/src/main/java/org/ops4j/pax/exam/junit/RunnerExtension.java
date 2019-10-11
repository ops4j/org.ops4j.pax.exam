/*
 * Copyright 2016 Harald Wellmann.
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

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.FrameworkMethod;
import org.ops4j.pax.exam.TestFilter;

/**
 * @author hwellmann
 *
 */
public class RunnerExtension {

    public boolean shouldDelegateClass() {
        return false;
    }

    public boolean shouldDelegateMethod() {
        return false;
    }

    public void beforeClassBlock(RunNotifier notifier) {
    }

    public void afterClassBlock(RunNotifier notifier) {
    }

    public void delegateClassBlock(RunNotifier notifier) {
    }

    public void beforeMethodBlock(FrameworkMethod method) {
    }

    public void afterMethodBlock(FrameworkMethod method) {
    }

    public void delegateMethodBlock(FrameworkMethod method, RunNotifier notifier) {
    }

    public Object processTestInstance(Object test) {
        return test;
    }

    public void setFilter(TestFilter filter) {
    }

}
