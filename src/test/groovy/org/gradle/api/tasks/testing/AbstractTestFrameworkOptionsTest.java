/*
 * Copyright 2009 the original author or authors.
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

package org.gradle.api.tasks.testing;

import org.gradle.util.JUnit4GroovyMockery;
import org.gradle.api.testing.TestFramework;
import org.jmock.lib.legacy.ClassImposteriser;

/**
 * @author Tom Eyckmans
 */
public class AbstractTestFrameworkOptionsTest<T extends TestFramework> {
    protected JUnit4GroovyMockery context = new JUnit4GroovyMockery();

    protected T testFrameworkMock;

    protected void setUp(Class<T> testFrameworkClass) throws Exception
    {
        context.setImposteriser(ClassImposteriser.INSTANCE);

        testFrameworkMock = context.mock(testFrameworkClass);
    }
}
