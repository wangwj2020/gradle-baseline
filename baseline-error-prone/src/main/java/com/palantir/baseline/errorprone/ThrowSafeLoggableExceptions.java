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
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.NewClassTree;
import com.sun.source.tree.ThrowTree;
import java.util.List;
import java.util.regex.Pattern;

@AutoService(BugChecker.class)
@BugPattern(
        name = "ThrowSafeLoggableExceptions",
        link = "https://github.com/palantir/gradle-baseline#baseline-error-prone-checks",
        linkType = BugPattern.LinkType.CUSTOM,
        providesFix = BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION,
        severity = BugPattern.SeverityLevel.SUGGESTION,
        summary = "SafeLoggable exceptions should be thrown for a pleasant debugging experience.")
public final class ThrowSafeLoggableExceptions extends BugChecker
        implements BugChecker.MethodInvocationTreeMatcher, BugChecker.ThrowTreeMatcher {

    private static final Matcher<ExpressionTree> commonValidators = Matchers.anyOf(
            MethodMatchers.staticMethod()
                    .onClass("com.google.common.base.Preconditions")
                    .withNameMatching(Pattern.compile("checkArgument|checkState|checkNotNull")),
            MethodMatchers.staticMethod().onClass("java.util.Objects").named("requireNonNull"),
            MethodMatchers.staticMethod()
                    .onClass("org.apache.commons.lang3.Validate")
                    .withNameMatching(Pattern.compile("isTrue|notNull|validState")));

    private static final Matcher<ExpressionTree> safeLoggable =
            Matchers.isSubtypeOf("com.palantir.logsafe.SafeLoggable");

    private static final Matcher<ExpressionTree> throwable = Matchers.isSubtypeOf(Throwable.class);

    @Override
    public Description matchThrow(ThrowTree tree, VisitorState state) {
        // Only match "throw new <type>" to avoid creating noise while rethrowing existing exceptions.
        ExpressionTree expression = ASTHelpers.stripParentheses(tree.getExpression());
        if (expression instanceof NewClassTree && !safeLoggable.matches(expression, state)) {
            NewClassTree newClassTree = (NewClassTree) expression;
            List<? extends ExpressionTree> arguments = newClassTree.getArguments();
            boolean isCauseOrDefaultConstructor =
                    arguments.isEmpty() || (arguments.size() == 1 && throwable.matches(arguments.get(0), state));
            if (!isCauseOrDefaultConstructor) {
                return describeMatch(expression);
            }
        }
        return Description.NO_MATCH;
    }

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        // Roughly equivalent to PreferSafeLoggingPreconditions, except this check does not allow
        if (commonValidators.matches(tree, state)) {
            return buildDescription(tree)
                    .setMessage(
                            "This call can be replaced with an equivalent one from com.palantir.logsafe.Preconditions")
                    .build();
        }
        return Description.NO_MATCH;
    }
}
