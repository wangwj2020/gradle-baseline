/*
 * (c) Copyright 2018 Palantir Technologies Inc. All rights reserved.
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
import com.google.errorprone.bugpatterns.AbstractReturnValueIgnored;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.fixes.SuggestedFixes;
import com.google.errorprone.matchers.ChildMultiMatcher;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MemberReferenceTree;
import com.sun.source.tree.MethodInvocationTree;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

@AutoService(BugChecker.class)
@BugPattern(
        name = "ExecutorSubmitRunnableFutureIgnored",
        link = "https://github.com/palantir/gradle-baseline#baseline-error-prone-checks",
        linkType = BugPattern.LinkType.CUSTOM,
        providesFix = BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION,
        severity = BugPattern.SeverityLevel.ERROR,
        summary = "Uncaught exceptions from ExecutorService.submit are not logged by the uncaught exception handler "
                + "because it is assumed that the returned future is used to watch for failures.\n"
                + "When the returned future is ignored, using ExecutorService.execute is preferred because "
                + "failures are recorded.")
public final class ExecutorSubmitRunnableFutureIgnored extends AbstractReturnValueIgnored {

    private static final Matcher<ExpressionTree> SUBMIT_RUNNABLE = MethodMatchers.instanceMethod()
            .onDescendantOf(ExecutorService.class.getName())
            .named("submit")
            .withParameters(Runnable.class.getName());

    private static final Matcher<ExpressionTree> SUBMIT_AMBIGUOUS_CALLABLE = Matchers.methodInvocation(
            MethodMatchers.instanceMethod()
                    .onDescendantOf(ExecutorService.class.getName())
                    .named("submit")
                    .withParameters(Callable.class.getName()),
            // Only one parameter, so any of the current MatchTypes will do
            ChildMultiMatcher.MatchType.ALL,
            (Matcher<ExpressionTree>) (tree, state) -> {
                // Expression lambdas don't have an explicit return statement, and can function as runnables
                if (tree instanceof LambdaExpressionTree) {
                    LambdaExpressionTree lambdaExpressionTree = (LambdaExpressionTree) tree;
                    return lambdaExpressionTree.getBodyKind() == LambdaExpressionTree.BodyKind.EXPRESSION;
                }
                // Method references are ambiguous as well, e.g. executor.submit(this::function)
                if (tree instanceof MemberReferenceTree) {
                    MemberReferenceTree memberReferenceTree = (MemberReferenceTree) tree;
                    return memberReferenceTree.getMode() == MemberReferenceTree.ReferenceMode.INVOKE;
                }
                return false;
            });

    private static final Matcher<ExpressionTree> MATCHER = Matchers.anyOf(SUBMIT_RUNNABLE, SUBMIT_AMBIGUOUS_CALLABLE);

    @Override
    public Matcher<? super ExpressionTree> specializedMatcher() {
        return MATCHER;
    }

    // Override matchMethodInvocation from AbstractReturnValueIgnored to apply our suggested fix.
    @Override
    public Description matchMethodInvocation(MethodInvocationTree methodInvocationTree, VisitorState state) {
        Description description = super.matchMethodInvocation(methodInvocationTree, state);
        if (Description.NO_MATCH.equals(description)) {
            return description;
        }
        return buildDescription(methodInvocationTree)
                .addFix(SuggestedFixes.renameMethodInvocation(methodInvocationTree, "execute", state))
                .build();
    }
}
