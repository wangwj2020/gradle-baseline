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
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.util.Warner;
import java.util.List;

@AutoService(BugChecker.class)
@BugPattern(
        name = "JacksonConvertValueGenericMigration",
        link = "https://github.com/palantir/gradle-baseline#baseline-error-prone-checks",
        linkType = BugPattern.LinkType.CUSTOM,
        severity = BugPattern.SeverityLevel.SUGGESTION,
        summary = "Recent jackson releases require the convertValue TypeReference generic type to match the result.")
public final class JacksonConvertValueGenericMigration
        extends BugChecker implements BugChecker.MethodInvocationTreeMatcher {

    private static final String TYPE_REFERENCE =  "com.fasterxml.jackson.core.type.TypeReference";
    private static final Matcher<ExpressionTree> MATCHER = MethodMatchers.instanceMethod()
            .onDescendantOf("com.fasterxml.jackson.databind.ObjectMapper")
            .named("convertValue")
            .withParameters(Object.class.getName(), TYPE_REFERENCE);

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        if (!MATCHER.matches(tree, state)) {
            return Description.NO_MATCH;
        }
        Type returnType = ASTHelpers.getResultType(tree);
        if (returnType == null) {
            return Description.NO_MATCH;
        }
        ExpressionTree typeReferenceArgument = tree.getArguments().get(1);
        Type typeReferenceArgumentType = ASTHelpers.getResultType(typeReferenceArgument);
        Symbol typeReferenceSymbol = state.getSymbolFromString(TYPE_REFERENCE);
        if (typeReferenceSymbol == null) {
            return Description.NO_MATCH;
        }
        Type typeReferenceType = state.getTypes().asSuper(typeReferenceArgumentType, typeReferenceSymbol);
        if (typeReferenceType == null) {
            return Description.NO_MATCH;
        }
        List<Type> typeArguments = typeReferenceType.getTypeArguments();
        if (typeArguments.size() != 1) {
            return Description.NO_MATCH;
        }
        Type expectedType = typeArguments.get(0);
        if (state.getTypes().isSameType(expectedType, returnType)) {
            return Description.NO_MATCH;
        }
        SuggestedFix.Builder fix = SuggestedFix.builder();
        String qualifiedTypeReference = SuggestedFixes.qualifyType(state, fix, TYPE_REFERENCE);
        String qualifiedReturnType = SuggestedFixes.qualifyType(state, fix, returnType);
        Type expectedTypeReferenceType = state.getType(typeReferenceType, false, ImmutableList.of(returnType));
        WarnChecker warner = new WarnChecker();
        if (expectedTypeReferenceType != null
                && state.getTypes().isCastable(typeReferenceArgumentType, expectedTypeReferenceType, warner)) {
            fix.prefixWith(typeReferenceArgument, '(' + qualifiedTypeReference + "<" + qualifiedReturnType + ">) ");
            if (warner.isWarned()) {
                SuggestedFixes.addSuppressWarnings(fix, state, "unchecked");
            }
        } else {
            fix.replace(typeReferenceArgument, "new " + qualifiedTypeReference + "<" + qualifiedReturnType + ">() {}");
        }
        return buildDescription(typeReferenceArgument)
                .addFix(fix.build())
                .build();
    }

    private static final class WarnChecker extends Warner {

        boolean isWarned() {
            return warned;
        }
    }
}
