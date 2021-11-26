package org.openrewrite.starter;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.NameCaseConvention;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Statement;

import java.util.Arrays;
import java.util.List;

public class NoEmptyCatchBlock extends Recipe {
    @Override
    public String getDisplayName() {
        return "Do not have empty catch blocks";
    }

    @Override
    public String getDescription() {
        return "Empty catch blocks make no sense";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            private final JavaTemplate newPrintStackTrace = JavaTemplate
                    .builder(this::getCursor, "#{any(java.lang.Exception)}.printStackTrace()")
                    .build();
            @Override
            public J visitCatch(J.Try.Catch _catch, ExecutionContext executionContext) {
                J.Block body = _catch.getBody();
                if(body.getStatements().isEmpty()) {
                    // This is still a bit nasty, can probably be done better. 
                    J.Identifier var = _catch
                            .getParameter()
                            .getPadding()
                            .getTree()
                            .getElement()
                            .getVariables()
                            .get(0)
                            .getName();

                    J.Block updatedBody = body.withTemplate(newPrintStackTrace, body.getCoordinates().firstStatement(), var);
                    System.out.println(updatedBody);
                    return super.visitCatch(_catch.withBody(updatedBody), executionContext);
                } else {
                    return super.visitCatch(_catch, executionContext);
                }
            }
        };
    }
}
