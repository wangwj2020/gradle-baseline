package com.palantir.baseline.refaster.jdk11;

import com.palantir.baseline.refaster.RefasterTestHelper;
import org.junit.jupiter.api.Test;

class JdkImmutableListTest {

    @Test
    public void guavaToJdk() {
        RefasterTestHelper
                .forRefactoring(JdkImmutableList.class)
                .withInputLines(
                        "Test",
                        "import java.util.List;",
                        "import com.google.common.collect.ImmutableList;",
                        "public class Test {",
                        "  List<String> list = ImmutableList.of(\"value1\", \"value2\");",
                        "}")
                .hasOutputLines(
                        "import java.util.List;",
                        "public class Test {",
                        "  List<String> list = List.of(\"value1\", \"value2\");",
                        "}");
    }
}