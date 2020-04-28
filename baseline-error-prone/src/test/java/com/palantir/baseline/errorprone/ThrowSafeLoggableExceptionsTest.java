/*
 * (c) Copyright 2020 Palantir Technologies Inc. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.palantir.baseline.errorprone;

import com.google.errorprone.CompilationTestHelper;
import org.junit.jupiter.api.Test;

class ThrowSafeLoggableExceptionsTest {

    @Test
    public void testRuntimeException_noArgsPasses() {
        helper().addSourceLines(
                        "Test.java", "class Test {", "  void f() {", "    throw new RuntimeException();", "  }", "}")
                .doTest();
    }

    @Test
    public void testRuntimeException_causePasses() {
        helper().addSourceLines(
                        "Test.java",
                        "class Test {",
                        "  void f(Throwable cause) {",
                        "    throw new RuntimeException(cause);",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    public void testRuntimeException_stringFails() {
        helper().addSourceLines(
                        "Test.java",
                        "class Test {",
                        "  void f() {",
                        "// BUG: Diagnostic contains: SafeLoggable",
                        "    throw new RuntimeException(\"foo\");",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    public void testRuntimeException_stringAndCauseFails() {
        helper().addSourceLines(
                        "Test.java",
                        "class Test {",
                        "  void f(Throwable cause) {",
                        "// BUG: Diagnostic contains: SafeLoggable",
                        "    throw new RuntimeException(\"foo\", cause);",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    public void testUnsafePreconditions() {
        helper().addSourceLines(
                        "Test.java",
                        "class Test {",
                        "  void f() {",
                        "// BUG: Diagnostic contains: com.palantir.logsafe.Preconditions",
                        "    com.google.common.base.Preconditions.checkArgument(true, \"a %s\", \"b\");",
                        "// BUG: Diagnostic contains: com.palantir.logsafe.Preconditions",
                        "    com.google.common.base.Preconditions.checkState(true, \"a %s\", \"b\");",
                        "// BUG: Diagnostic contains: com.palantir.logsafe.Preconditions",
                        "    com.google.common.base.Preconditions.checkNotNull(new Object(), \"a %s\", \"b\");",
                        "// BUG: Diagnostic contains: com.palantir.logsafe.Preconditions",
                        "org.apache.commons.lang3.Validate.isTrue(true, \"a\");",
                        "// BUG: Diagnostic contains: com.palantir.logsafe.Preconditions",
                        "org.apache.commons.lang3.Validate.validState(true, \"a\");",
                        "// BUG: Diagnostic contains: com.palantir.logsafe.Preconditions",
                        "org.apache.commons.lang3.Validate.notNull(new Object(), \"a %s\", \"b\");",
                        "// BUG: Diagnostic contains: com.palantir.logsafe.Preconditions",
                        "java.util.Objects.requireNonNull(new Object());",
                        "// BUG: Diagnostic contains: com.palantir.logsafe.Preconditions",
                        "java.util.Objects.requireNonNull(new Object(), \"msg\");",
                        "  }",
                        "}")
                .doTest();
    }

    private CompilationTestHelper helper() {
        return CompilationTestHelper.newInstance(ThrowSafeLoggableExceptions.class, getClass());
    }
}
