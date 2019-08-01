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

import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Attribute.Compound;
import com.sun.tools.javac.code.Symbol;

import javax.lang.model.element.AnnotationValue;
import javax.lang.model.util.SimpleAnnotationValueVisitor8;
import java.util.List;
import java.util.Optional;

@AutoService(BugChecker.class)
@BugPattern(
        name = "JUnit5SuiteUsage",
        severity = BugPattern.SeverityLevel.ERROR,
        summary = "Using JUnit5 tests in JUnit4 suites results in the tests silently not executing")
public final class JUnit5SuiteUsage extends BugChecker implements BugChecker.ClassTreeMatcher {
    private static final String JUNIT4_SUITE_CLASSES = "org.junit.runners.Suite.SuiteClasses";
    private static final Matcher<Tree> USES_SUITE_CLASSES = Matchers.hasAnnotation(JUNIT4_SUITE_CLASSES);

    @Override
    public Description matchClass(ClassTree tree, VisitorState state) {
        if (!USES_SUITE_CLASSES.matches(tree, state)) {
            return Description.NO_MATCH;
        }

        AnnotationTree annotation = findAnnotation(tree, state, JUNIT4_SUITE_CLASSES);
        Optional<Attribute> expected = getSymbolAttribute(
                ASTHelpers.getSymbol(tree), state, JUNIT4_SUITE_CLASSES, "value");
        if (!expected.isPresent()) {
            return Description.NO_MATCH;
        }

        Attribute expectedException = expected.get();
        List<Class<?>> classes = expectedException.accept(new SimpleAnnotationValueVisitor8<List<Class<?>>, Void>() {
            @Override
            public List<Class<?>> visitArray(List<? extends AnnotationValue> vals, Void aVoid) {
                return vals.stream()
                        .filter(val -> val instanceof Attribute.Class)
                        .map(val -> {
                            try {
                                Attribute.Class val1 = (Attribute.Class) val;
                                return Class.forName(val1.getValue().toString());
                            } catch (ClassNotFoundException e) {
                                throw new RuntimeException(e);
                            }
                        })
                        .collect(ImmutableList.toImmutableList());
            }
        }, null);

        return Description.NO_MATCH;
    }

    private static AnnotationTree findAnnotation(ClassTree tree, VisitorState state, String annotationName) {
        AnnotationTree annotationNode = null;
        for (AnnotationTree annotation : tree.getModifiers().getAnnotations()) {
            Symbol sym = ASTHelpers.getSymbol(annotation);
            if (sym.equals(state.getSymbolFromString(annotationName))) {
                annotationNode = annotation;
            }
        }
        return annotationNode;
    }

    private static Optional<Attribute> getSymbolAttribute(Symbol sym, VisitorState state, String annotation, String name) {
        Compound attribute = sym.attribute(state.getSymbolFromString(annotation));
        if (attribute == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(attribute.member(state.getName(name)));
    }
}
