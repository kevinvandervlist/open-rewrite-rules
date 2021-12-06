package org.openrewrite.starter;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.ListUtils;
import org.openrewrite.internal.NameCaseConvention;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.Arrays;
import java.util.Objects;

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
            private final MethodMatcher EQ = new MethodMatcher("java.lang.String equals(String)");
            private final MethodMatcher EQANY = new MethodMatcher("java.lang.String equals(..)");
            private final MethodMatcher EQ_IGNORE_CASE = new MethodMatcher("java.lang.String equalsIgnoreCase(java.lang.String)");
            private final MethodMatcher LOWER_CASE = new MethodMatcher("java.lang.String toLowerCase()");
            private final MethodMatcher UPPER_CASE = new MethodMatcher("java.lang.String toUpperCase()");

            private final JavaTemplate newEqualsIgnoreCase = JavaTemplate
                    .builder(this::getCursor, "#{any(java.lang.String)}.equalsIgnoreCase(#{any(java.lang.String)})")
                    .build();

            @Override
            public J visitMethodInvocation(J.MethodInvocation method, ExecutionContext executionContext) {
                // "Abc".toUpperCase().equals("ABC");
//                if(EQ.matches(method)) {
//                    String x = method.print(getCursor());
//                    System.out.println(x);
//                }
//                if(EQANY.matches(method)) {
//                    System.out.println(isToLowerOrUpper(method));
//                    String x = method.print(getCursor());
//                    System.out.println(x);
//                }

                // "abc".equals("ABC".toLowerCase());
                // Why does EQ matcher not work?
                //if(EQ.matches(method)) {
                if(method.getName().getSimpleName().equals("equals") && method.getArguments().size() == 1 && Objects.equals(method.getArguments().get(0).getType(), JavaType.buildType("java.lang.String"))) {
                    J head = method.getArguments().get(0);
                    if(head instanceof J.MethodInvocation &&
                            isToLowerOrUpper((J.MethodInvocation) head)) {
                        J.MethodInvocation h = (J.MethodInvocation) head;
                        //J.MethodInvocation updated = newEqualsIgnoreCase.withTemplate(method.withTemplate(newEqualsIgnoreCase, method.getCoordinates().replaceMethod(), method.getSelect(), h.getSelect()));
                        //System.out.println(updated);
                        //return updated;
                        //System.out.println(h.getSelect());
                    }
                }
//                System.out.println(method.getSimpleName() + "; " + isToLowerOrUpper(method));
//                // ((J.MethodInvocation) method.getSelect()).getName()
//                if(EQ.matches(method)) {
//                    System.out.println(method.getName());
//                }
                return super.visitMethodInvocation(method, executionContext);
            }

            private boolean isToLowerOrUpper(J.MethodInvocation m) {
                return LOWER_CASE.matches(m) || UPPER_CASE.matches(m);
            }
        };
    }
}
