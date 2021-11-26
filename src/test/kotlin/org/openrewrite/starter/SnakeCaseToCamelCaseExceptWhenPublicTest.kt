package org.openrewrite.starter

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

class SnakeCaseToCamelCaseExceptWhenPublicTest: JavaRecipeTest {
    override val parser: JavaParser
        get() = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .build()

    override val recipe: Recipe
        get() = SnakeCaseToCamelCaseExceptWhenPublic()

    @Test
    fun replacePrivateFunctionName() = assertChanged(
        before = """
            class TestClass {
                public String foo_foo() {
                    return bar_bar();                
                }
                private String bar_bar() {
                    return "bar_bar";
                }
            }
        """,
        after = """
            class TestClass {
                public String foo_foo() {
                    return barBar();                
                }
                private String barBar() {
                    return "bar_bar";
                }
            }
        """
    )
}
