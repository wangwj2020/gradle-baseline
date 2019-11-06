package com.palantir.baseline.errorprone;

import com.google.auto.service.AutoService;
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Type;
import java.util.Collection;
import java.util.Map;
import javax.annotation.Nullable;

@AutoService(BugChecker.class)
@BugPattern(
        name = "UnexpectedType",
        link = "https://github.com/palantir/gradle-baseline#baseline-error-prone-checks",
        linkType = BugPattern.LinkType.CUSTOM,
        severity = BugPattern.SeverityLevel.WARNING,
        summary = "Likely programming error due to using the wrong type in a method that accepts Object.")
public final class UnexpectedType extends BugChecker implements BugChecker.MethodInvocationTreeMatcher {

    private static final String MAP = Map.class.getName();
    private static final String COLLECTION = Collection.class.getName();

    private static final Matcher<ExpressionTree> mapGet = MethodMatchers.instanceMethod()
            .onDescendantOf(Map.class.getName())
            .named("get")
            .withParameters(Object.class.getName());

    private static final Matcher<ExpressionTree> mapContainsKey = MethodMatchers.instanceMethod()
            .onDescendantOf(MAP)
            .named("containsKey")
            .withParameters(Object.class.getName());

    private static final Matcher<ExpressionTree> collectionContains = MethodMatchers.instanceMethod()
            .onDescendantOf(COLLECTION)
            .named("contains")
            .withParameters(Object.class.getName());

    @Override
    public Description matchMethodInvocation(MethodInvocationTree tree, VisitorState state) {
        if (mapGet.matches(tree, state) || mapContainsKey.matches(tree, state)) {
            Type mapType = getTargetTypeAsSuper(tree, MAP, state);
            if (mapType == null) {
                return Description.NO_MATCH;
            }
            if (mapType.getTypeArguments().size() != 2) {
                return Description.NO_MATCH;
            }
            Type keyType = mapType.getTypeArguments().get(0);
            ExpressionTree keyExpression = tree.getArguments().get(0);
            if (ASTHelpers.isCastable(ASTHelpers.getResultType(keyExpression), keyType, state)) {
                return Description.NO_MATCH;
            }
            return describeMatch(tree);
        } else if (collectionContains.matches(tree, state)) {
            Type collectionType = getTargetTypeAsSuper(tree, COLLECTION, state);
            if (collectionType == null) {
                return Description.NO_MATCH;
            }
            if (collectionType.getTypeArguments().size() != 1) {
                return Description.NO_MATCH;
            }
            Type elementType = collectionType.getTypeArguments().get(0);
            ExpressionTree containsExpression = tree.getArguments().get(0);
            if (ASTHelpers.isCastable(ASTHelpers.getResultType(containsExpression), elementType, state)) {
                return Description.NO_MATCH;
            }
            return describeMatch(tree);
        }
        return Description.NO_MATCH;
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
}
