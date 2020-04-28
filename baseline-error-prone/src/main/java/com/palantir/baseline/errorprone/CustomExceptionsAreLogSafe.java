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
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;

@AutoService(BugChecker.class)
@BugPattern(
        name = "CustomExceptionsAreLogSafe",
        link = "https://github.com/palantir/gradle-baseline#baseline-error-prone-checks",
        linkType = BugPattern.LinkType.CUSTOM,
        providesFix = BugPattern.ProvidesFix.REQUIRES_HUMAN_ATTENTION,
        severity = BugPattern.SeverityLevel.SUGGESTION,
        summary = "Custom exceptions should implement SafeLoggable")
public final class CustomExceptionsAreLogSafe extends BugChecker implements BugChecker.ClassTreeMatcher {
    private static final String SAFE_LOGGABLE_STRING = "com.palantir.logsafe.SafeLoggable";

    private static final Matcher<ClassTree> matcher = Matchers.allOf(
            Matchers.isSubtypeOf(Throwable.class), Matchers.not(Matchers.isSubtypeOf(SAFE_LOGGABLE_STRING)));

    @Override
    public Description matchClass(ClassTree tree, VisitorState state) {
        if (!matcher.matches(tree, state)) {
            return Description.NO_MATCH;
        }

        // If the default no-arg constructor is used, there's no additional data beyond class name
        // and stack trace that can be marked safe.
        if (ASTHelpers.getConstructors(tree).stream().allMatch(ASTHelpers::isGeneratedConstructor)) {
            return Description.NO_MATCH;
        }

        return describeMatch(tree);
    }
}
