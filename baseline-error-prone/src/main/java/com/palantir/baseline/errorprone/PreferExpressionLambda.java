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
import com.google.errorprone.BugPattern;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ReturnTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.SimpleTreeVisitor;
import java.util.List;
import java.util.Optional;

@AutoService(BugChecker.class)
@BugPattern(
        name = "PreferExpressionLambda",
        link = "https://github.com/palantir/gradle-baseline#baseline-error-prone-checks",
        linkType = BugPattern.LinkType.CUSTOM,
        severity = BugPattern.SeverityLevel.SUGGESTION,
        summary = "Lambda should be an expression, braces are unnecessary")
public final class PreferExpressionLambda extends BugChecker implements BugChecker.LambdaExpressionTreeMatcher {

    @Override
    public Description matchLambdaExpression(LambdaExpressionTree tree, VisitorState state) {
        LambdaExpressionTree.BodyKind bodyKind = tree.getBodyKind();
        if (bodyKind == LambdaExpressionTree.BodyKind.EXPRESSION) {
            return Description.NO_MATCH;
        }
        Tree body = tree.getBody();
        if (!(body instanceof BlockTree)) {
            return Description.NO_MATCH;
        }
        BlockTree block = (BlockTree) body;
        List<? extends StatementTree> statements = block.getStatements();
        if (statements.size() != 1) {
            return Description.NO_MATCH;
        }
        StatementTree statement = statements.get(0);
        Optional<Tree> replacement = statement.accept(Visitor.INSTANCE, null);
        if (!replacement.isPresent()) {
            return Description.NO_MATCH;
        }
        return buildDescription(tree.getBody())
                .addFix(SuggestedFix.builder()
                        .replace(tree.getBody(), state.getSourceForNode(replacement.get()))
                        .build())
                .build();
    }

    private static final class Visitor extends SimpleTreeVisitor<Optional<Tree>, Void> {
        private static final Visitor INSTANCE = new Visitor();

        Visitor() {
            super(Optional.empty());
        }

        @Override
        public Optional<Tree> visitBlock(BlockTree node, Void state) {
            List<? extends StatementTree> statements = node.getStatements();
            if (statements.size() == 1) {
                return statements.get(0).accept(this, state);
            }
            return Optional.empty();
        }

        @Override
        public Optional<Tree> visitExpressionStatement(ExpressionStatementTree node, Void state) {
            return node.getExpression().accept(this, state);
        }

        @Override
        public Optional<Tree> visitMethodInvocation(MethodInvocationTree node, Void state) {
            return Optional.of(node);
        }

        @Override
        public Optional<Tree> visitReturn(ReturnTree node, Void state) {
            return Optional.of(node.getExpression());
        }
    }
}
