package com.palantir.baseline.errorprone;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.MethodTree;

@AutoService(BugChecker.class)
@BugPattern(
        name = "ConjureMigrator",
        link = "https://github.com/palantir/gradle-baseline#baseline-error-prone-checks",
        linkType = BugPattern.LinkType.CUSTOM,
        severity = BugPattern.SeverityLevel.WARNING,
        summary = "test")
public final class ConjureMigrator extends BugChecker implements BugChecker.MethodTreeMatcher {

    // TODO: dynamic parameters based on compiler flags
    private static final Matcher<MethodTree> matcher = Matchers.allOf(
            Matchers.methodIsNamed("endpointName"),
            Matchers.enclosingClass(Matchers.isSubtypeOf("com.palantir.foo.bar.UndertowBazzInterface")))

    @Override
    public Description matchMethod(MethodTree tree, VisitorState state) {
        if (!matcher.matches(tree, state)) {
            return Description.NO_MATCH;
        }
        return buildDescription(tree)
                .addFix(SuggestedFix.replace(tree,
                        buildLegacyStub(tree, state) + "\n" + state.getSourceForNode(tree)))
                .build();
    }

    private static String buildLegacyStub(MethodTree _method, VisitorState _state) {
        return ""; //todo
    }
}
