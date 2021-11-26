package org.openrewrite.starter;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.NameCaseConvention;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

public class UnnecessaryCaseChange extends Recipe {
    @Override
    public String getDisplayName() {
        return "UnnecessaryCaseChange";
    }

    @Override
    public String getDescription() {
        return "Using equalsIgnoreCase() is faster than using toUpperCase/toLowerCase().equals()";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            private final MethodMatcher EQ = new MethodMatcher("java.lang.String equals(java.lang.String)");
            private final MethodMatcher EQ_IGNORE_CASE = new MethodMatcher("java.lang.String equalsIgnoreCase(java.lang.String)");
            private final MethodMatcher LOWER_CASE = new MethodMatcher("java.lang.String toLowerCase()");
            private final MethodMatcher UPPER_CASE = new MethodMatcher("java.lang.String toUpperCase()");
            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                System.out.println(method.getSimpleName() + "; " + isToLowerOrUpper(method));
                System.out.println(method.get + "; " + isToLowerOrUpper(method));
                if(EQ.matches(method)) {
                    System.out.println(method.getName());
                }
                return super.visitMethodInvocation(method, executionContext);
            }

            private boolean isToLowerOrUpper(J.MethodInvocation m) {
                return LOWER_CASE.matches(m) || UPPER_CASE.matches(m);
            }
        };
    }
}
