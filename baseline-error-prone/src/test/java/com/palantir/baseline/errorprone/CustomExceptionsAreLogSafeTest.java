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

class CustomExceptionsAreLogSafeTest {

    @Test
    public void testCustomException() {
        helper().addSourceLines(
                        "Test.java",
                        "// BUG: Diagnostic contains: should implement SafeLoggable",
                        "public final class Test extends RuntimeException {",
                        "  public Test(String message) {",
                        "    super(message);",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    public void testNegativeCustomException() {
        helper().addSourceLines(
                        "Test.java",
                        "import com.palantir.logsafe.Arg;",
                        "import com.palantir.logsafe.SafeLoggable;",
                        "import java.util.List;",
                        "public final class Test extends RuntimeException implements SafeLoggable {",
                        "  @Override public String getLogMessage() {",
                        "    return \"foo\";",
                        "  }",
                        "  @Override public List<Arg<?>> getArgs() {",
                        "    return null;",
                        "  }",
                        "}")
                .doTest();
    }

    @Test
    public void testCustomException_defaultConstructor() {
        helper().addSourceLines("Test.java", "public final class Test extends RuntimeException {", "}")
                .doTest();
    }

    private CompilationTestHelper helper() {
        return CompilationTestHelper.newInstance(CustomExceptionsAreLogSafe.class, getClass());
    }
}
