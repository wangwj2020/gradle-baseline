/*
 * (c) Copyright 2019 Palantir Technologies Inc. All rights reserved.
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

import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.Test;

public final class PreferExpressionLambdaTest {

    @Test
    public void testAutoFix() {
        BugCheckerRefactoringTestHelper.newInstance(new PreferExpressionLambda(), getClass())
                .addInputLines(
                        "Test.java",
                        "import " + ImmutableList.class.getName() + ';',
                        "import " + List.class.getName() + ';',
                        "import " + Optional.class.getName() + ';',
                        "class Test {",
                        "  public List<Object> foo(Optional<List<Object>> optional) {",
                        "    return optional.orElseGet(() -> { return ImmutableList.of(); });",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import " + ImmutableList.class.getName() + ';',
                        "import " + List.class.getName() + ';',
                        "import " + Optional.class.getName() + ';',
                        "class Test {",
                        "  public List<Object> foo(Optional<List<Object>> optional) {",
                        "    return optional.orElseGet(() -> ImmutableList.of());",
                        "  }",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    public void testAutoFix_args() {
        BugCheckerRefactoringTestHelper.newInstance(new PreferExpressionLambda(), getClass())
                .addInputLines(
                        "Test.java",
                        "import " + Map.class.getName() + ';',
                        "import " + Consumer.class.getName() + ';',
                        "class Test {",
                        "  public void foo(Map<String, Integer> map, Consumer<String> consumer) {",
                        "    map.forEach((key, value) -> { consumer.accept(key); });",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import " + Map.class.getName() + ';',
                        "import " + Consumer.class.getName() + ';',
                        "class Test {",
                        "  public void foo(Map<String, Integer> map, Consumer<String> consumer) {",
                        "    map.forEach((key, value) -> consumer.accept(key));",
                        "  }",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    public void testAutoFix_reference() {
        BugCheckerRefactoringTestHelper.newInstance(new PreferExpressionLambda(), getClass())
                .addInputLines(
                        "Test.java",
                        "import " + Optional.class.getName() + ';',
                        "class Test {",
                        "  public String foo(Optional<String> opt) {",
                        "    String value = \"value\";",
                        "    return opt.orElseGet(() -> { return value; });",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import " + Optional.class.getName() + ';',
                        "class Test {",
                        "  public String foo(Optional<String> opt) {",
                        "    String value = \"value\";",
                        "    return opt.orElseGet(() -> value);",
                        "  }",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    public void testAutoFix_referenceClass() {
        BugCheckerRefactoringTestHelper.newInstance(new PreferExpressionLambda(), getClass())
                .addInputLines(
                        "Test.java",
                        "import " + Optional.class.getName() + ';',
                        "class Test {",
                        "  private static final String value = \"value\";",
                        "  public String foo(Optional<String> opt) {",
                        "    return opt.orElseGet(() -> { return value; });",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import " + Optional.class.getName() + ';',
                        "class Test {",
                        "  private static final String value = \"value\";",
                        "  public String foo(Optional<String> opt) {",
                        "    return opt.orElseGet(() -> value);",
                        "  }",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }

    @Test
    public void testAutoFix_binaryBlock() {
        BugCheckerRefactoringTestHelper.newInstance(new PreferExpressionLambda(), getClass())
                .addInputLines(
                        "Test.java",
                        "import " + Optional.class.getName() + ';',
                        "class Test {",
                        "  public String foo(Optional<String> opt) {",
                        "    String value = \"value\";",
                        "    return opt.orElseGet(() -> { return \"val: \" + value; });",
                        "  }",
                        "}")
                .addOutputLines(
                        "Test.java",
                        "import " + Optional.class.getName() + ';',
                        "class Test {",
                        "  public String foo(Optional<String> opt) {",
                        "    String value = \"value\";",
                        "    return opt.orElseGet(() -> \"val: \" + value);",
                        "  }",
                        "}")
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }
}
