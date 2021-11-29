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
    fun noUnnecessaryCaseChangetoUpperLowerThenEquals() = assertChanged(
        before = """
            class Test {
                public static boolean f(String p) {
                    "Abc".toUpperCase().equals("ABC");
                }
            }
        """,
        after = """
            class Test {
                public static boolean f(String p) {
                    "Abc".equalsIgnoreCase("ABC");
                }
            }
        """
    )

    @Test
    fun noUnnecessaryCaseChangeInEquals() = assertChanged(
        before = """
            class Test {
                public static boolean f(String p) {
                    "abc".equals("ABC".toLowerCase());
                }
            }
        """,
        after = """
            class Test {
                public static boolean f(String p) {
                    "abc".equalsIgnoreCase("ABC");
                }
            }
        """
    )
    // TODO: String x = foo.toLowerCase(); x.equals("bar");
}
