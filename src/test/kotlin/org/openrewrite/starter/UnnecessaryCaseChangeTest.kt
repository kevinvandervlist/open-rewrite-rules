package org.openrewrite.starter

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

class UnnecessaryCaseChangeTest: JavaRecipeTest {
    override val parser: JavaParser
        get() = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .classpath("slf4j")
            .build()

    override val recipe: Recipe
        get() = UnnecessaryCaseChange()

    @Test
    fun noUnnecessaryCaseChanges() = assertChanged(
        before = """
            class Test {
                public static boolean f(String p) {
                    "Abc".toUpperCase().equals("ABC");
                    "abc".equals("ABC".toLowerCase());
                }
            }
        """,
        after = """
            class Test {
                public static boolean f(String p) {
                    "Abc".equalsIgnoreCase("ABC");
                    "abc".equalsIgnoreCase("ABC");
                }
            }
        """
    )
}
