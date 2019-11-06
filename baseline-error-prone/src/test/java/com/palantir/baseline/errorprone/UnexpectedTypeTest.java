package com.palantir.baseline.errorprone;

import com.google.errorprone.CompilationTestHelper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

@Execution(ExecutionMode.CONCURRENT)
class UnexpectedTypeTest {

    @Test
    void testUnexpectedType_map() {
        helper().addSourceLines(
                "Test.java",
                "import java.util.Map;",
                "class Test {",
                "   String f0(Map<Integer, String> map, String key) {",
                "       // BUG: Diagnostic contains: wrong type",
                "       return map.get(key);",
                "   }",
                "   boolean f1(Map<Integer, String> map, String key) {",
                "       // BUG: Diagnostic contains: wrong type",
                "       return map.containsKey(key);",
                "   }",
                "   String f2(CustomMap map, String key) {",
                "       // BUG: Diagnostic contains: wrong type",
                "       return map.get(key);",
                "   }",
                "   boolean f3(CustomMap map, String key) {",
                "       // BUG: Diagnostic contains: wrong type",
                "       return map.containsKey(key);",
                "   }",
                "   interface CustomMap extends Map<Integer, String> {}",
                "}"
        ).doTest();
    }

    @Test
    void testUnexpectedType_collection() {
        helper().addSourceLines(
                "Test.java",
                "import java.util.Collection;",
                "import java.util.List;",
                "import com.google.common.collect.ImmutableMap;",
                "class Test {",
                "   boolean f0(Collection<Integer> in, String key) {",
                "       // BUG: Diagnostic contains: wrong type",
                "       return in.contains(key);",
                "   }",
                "   boolean f1(List<Integer> in, String key) {",
                "       // BUG: Diagnostic contains: wrong type",
                "       return in.contains(key);",
                "   }",
                "   boolean f2(Custom in, Integer key) {",
                "       // BUG: Diagnostic contains: wrong type",
                "       return in.contains(key);",
                "   }",
                "   boolean f3(ImmutableMap<Integer, String> in, String key) {",
                "       // BUG: Diagnostic contains: wrong type",
                "       return in.keySet().contains(key);",
                "   }",
                "   interface Custom extends List<CharSequence> {}",
                "}"
        ).doTest();
    }

    @Test
    void testCollectionContains_edges() {
        helper().addSourceLines(
                "Test.java",
                "import java.util.Collection;",
                "class Test {",
                "   boolean f(Collection<Integer> in) {",
                "     return in.contains(null) || in.contains(3);",
                "   }",
                "}"
        ).doTest();
    }

    @Test
    void testAmbiguousMapKey_allowed() {
        helper().addSourceLines(
                "Test.java",
                "import java.util.Map;",
                "class Test {",
                "   String f0(Map<String, String> map, Object key) {",
                // This is suspicious, but isn't impossible
                "       return map.get(key);",
                "   }",
                // Not recommended, but should not fail
                "   Object f1(Map map, String key) {",
                "       return map.get(key);",
                "   }",
                "}"
        ).doTest();
    }

    private CompilationTestHelper helper() {
        return CompilationTestHelper.newInstance(UnexpectedType.class, getClass());
    }
}
