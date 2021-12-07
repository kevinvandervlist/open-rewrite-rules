package nl.kevinvandervlist.rewrite

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

class NoEmptyCatchBlockTest: JavaRecipeTest {
    override val parser: JavaParser
        get() = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .build()

    override val recipe: Recipe
        get() = NoEmptyCatchBlock()

    @Test
    fun addStackTraceToEmptyCatch() = assertChanged(
        before = """
            class TestClass {
                public void foo() {
                    try {
                        System.out.println("Foo");
                    } catch (RuntimeException e) {
                    }
                    try {
                        System.out.println("Bar");
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                }
            }""",
        after = """
            class TestClass {
                public void foo() {
                    try {
                        System.out.println("Foo");
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                    try {
                        System.out.println("Bar");
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    }
                }
            }"""
    )

    @Test
    fun addStackTraceToEmptyMultiCatch() = assertChanged(
        before = """
            class TestClass {
                public void foo() {
                    try {
                        System.out.println("Foo");
                    } catch (RuntimeException e) {
                    } catch (IllegalStateException e) {
                    }
                    try {
                        System.out.println("Bar");
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    } catch (IllegalStateException e) {
                    }
                }
            }""",
        after = """
            class TestClass {
                public void foo() {
                    try {
                        System.out.println("Foo");
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                    try {
                        System.out.println("Bar");
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                    } catch (IllegalStateException e) {
                        e.printStackTrace();
                    }
                }
            }"""
    )
}
