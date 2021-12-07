package nl.kevinvandervlist.rewrite;

import org.openrewrite.ExecutionContext;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.NameCaseConvention;
import org.openrewrite.java.ChangeMethodName;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.J;

public class SnakeCaseToCamelCaseExceptWhenPublic extends Recipe {
    @Override
    public String getDisplayName() {
        return "Rename snake case method names to camel case except when they are public";
    }

    @Override
    public String getDescription() {
        return "Apply snake case to camel case transformation unless the method is public and therefore can have unknown consumers";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {
            @Override
            public J visitMethodDeclaration(J.MethodDeclaration method, ExecutionContext executionContext) {
                boolean isNotPublic = ! method.hasModifier(J.Modifier.Type.Public);
                boolean isSnakeCase = NameCaseConvention.matches(NameCaseConvention.LOWER_UNDERSCORE, method.getSimpleName());
                if(isNotPublic && isSnakeCase) {
                    String cc = NameCaseConvention.format(NameCaseConvention.LOWER_CAMEL, method.getSimpleName());
                    doNext(new ChangeMethodName(MethodMatcher.methodPattern(method), cc, true));
                }
                return super.visitMethodDeclaration(method, executionContext);
            }
        };
    }
}
