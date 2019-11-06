/*
 * (c) Copyright 2017 Palantir Technologies Inc. All rights reserved.
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
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import javax.annotation.Nullable;

@AutoService(BugChecker.class)
@BugPattern(
        name = "StrictCollectionIncompatibleType",
        link = "https://github.com/palantir/gradle-baseline#baseline-error-prone-checks",
        linkType = BugPattern.LinkType.CUSTOM,
        severity = BugPattern.SeverityLevel.WARNING,
        summary = "Likely programming error due to using incompatible types as " +
                "arguments for a collection method that accepts Object.")
public final class StrictCollectionIncompatibleType extends BugChecker implements BugChecker.MethodInvocationTreeMatcher {

    private static final Matcher<MethodInvocationTree> MATCHER = Matchers.anyOf(
            // Matcher arguments borrowed from error-prone CollectionIncompatibleType
            compatibleArgTypeMatcher("java.util.Collection", "contains(java.lang.Object)", 0, 0),
            compatibleArgTypeMatcher("java.util.Collection", "remove(java.lang.Object)", 0, 0),
            compatibleArgTypeMatcher("java.util.Deque", "removeFirstOccurrence(java.lang.Object)", 0, 0),
            compatibleArgTypeMatcher("java.util.Deque", "removeLastOccurrence(java.lang.Object)", 0, 0),
            compatibleArgTypeMatcher("java.util.Dictionary", "get(java.lang.Object)", 0, 0),
            compatibleArgTypeMatcher("java.util.Dictionary", "remove(java.lang.Object)", 0, 0),
            compatibleArgTypeMatcher("java.util.List", "indexOf(java.lang.Object)", 0, 0),
            compatibleArgTypeMatcher("java.util.List", "lastIndexOf(java.lang.Object)", 0, 0),
            compatibleArgTypeMatcher("java.util.Map", "containsKey(java.lang.Object)", 0, 0),
            compatibleArgTypeMatcher("java.util.Map", "containsValue(java.lang.Object)", 1, 0),
            compatibleArgTypeMatcher("java.util.Map", "get(java.lang.Object)", 0, 0),
            compatibleArgTypeMatcher("java.util.Map", "getOrDefault(java.lang.Object,V)", 0, 0),
            compatibleArgTypeMatcher("java.util.Map", "remove(java.lang.Object)", 0, 0),
            compatibleArgTypeMatcher("java.util.Stack", "search(java.lang.Object)", 0, 0),
            compatibleArgTypeMatcher("java.util.Vector", "indexOf(java.lang.Object,int)", 0, 0),
            compatibleArgTypeMatcher("java.util.Vector", "lastIndexOf(java.lang.Object,int)", 0, 0),
            compatibleArgTypeMatcher("java.util.Vector", "removeElement(java.lang.Object)", 0, 0));

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        if (MATCHER.matches(tree, state)) {
            return describeMatch(tree);
        }
        return Description.NO_MATCH;
    }

    @Nullable
    private static Type getBoxedResult(ExpressionTree expressionTree, VisitorState state) {
        Type rawType = ASTHelpers.getResultType(expressionTree);
        if (rawType == null) {
            return null;
        }
        return state.getTypes().boxedTypeOrType(rawType);
    }

    @Nullable
    private static Type getTargetTypeAsSuper(
            MethodInvocationTree tree,
            String superTarget,
            VisitorState state) {
        Type targetMapType = getTargetType(tree);
        if (targetMapType == null) {
            return null;
        }
        Symbol mapSymbol = state.getSymbolFromString(superTarget);
        if (mapSymbol == null) {
            return null;
        }
        return state.getTypes().asSuper(targetMapType, mapSymbol);
    }

    @Nullable
    private static Type getTargetType(MethodInvocationTree tree) {
        ExpressionTree methodSelect = tree.getMethodSelect();
        if (methodSelect instanceof MemberSelectTree) {
            MemberSelectTree memberSelectTree = (MemberSelectTree) methodSelect;
            return ASTHelpers.getResultType(memberSelectTree.getExpression());
        }
        return null;
    }

    private static Matcher<MethodInvocationTree> compatibleArgTypeMatcher(
            String baseType,
            String signature,
            int typeArgumentIndex,
            int argumentIndex) {
        return Matchers.allOf(
                MethodMatchers.instanceMethod()
                        .onDescendantOf(baseType)
                        .withSignature(signature),
                (Matcher<MethodInvocationTree>) (tree, state) -> {
                    if (tree.getArguments().size() <= argumentIndex) {
                        return false;
                    }
                    Type targetType = getTargetTypeAsSuper(tree, baseType, state);
                    if (targetType == null) {
                        return false;
                    }
                    if (targetType.getTypeArguments().size() <= typeArgumentIndex) {
                        return false;
                    }
                    Type typeArgumentType = targetType.getTypeArguments().get(typeArgumentIndex);
                    Type argumentType = getBoxedResult(tree.getArguments().get(argumentIndex), state);
                    if (argumentType == null) {
                        return false;
                    }
                    // Check erased types only to avoid more complex edge cases. This way we only warn when we
                    // have high confidence something isn't right.
                    // This tests that types are within the same (linear) inheritance hierarchy, but does not
                    // not accept types with a common ancestor.
                    return !ASTHelpers.isSubtype(argumentType, typeArgumentType, state)
                            // Check the reverse direction as well, this allows 'Object' to succeed for
                            // delegation, as well as most false positives without sacrificing many known
                            // failure cases.
                            && !ASTHelpers.isSubtype(typeArgumentType, argumentType, state);
                });
    }
}
