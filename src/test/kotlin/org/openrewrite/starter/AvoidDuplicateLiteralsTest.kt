package org.openrewrite.starter

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.java.JavaParser
import org.openrewrite.java.JavaRecipeTest

class AvoidDuplicateLiteralsTest: JavaRecipeTest {
    override val parser: JavaParser
        get() = JavaParser.fromJavaVersion()
            .logCompilationWarningsAndErrors(true)
            .classpath("slf4j")
            .build()

    override val recipe: Recipe
        get() = AvoidDuplicateLiterals()

    @Test
    fun replacePrivateFunctionName() = assertChanged(
        before = """
            class Test {
                public static boolean f(String p) {
                    if("foo".equals(p)) {
                        return true;
                    } else if(!"fooBar".equals(p)) {
                        return (p + "fooBar" + "foo").equals(p);
                    }
                }
            }
        """,
        after = """
            class Test {
                private static final String FOO = "foo";
                private static final String FOO_BAR = "fooBar";
                public static boolean f(String p) {
                    if(FOO.equals(p)) {
                        return true;
                    } else if(!FOO_BAR.equals(p)) {
                        return (p + FOO_BAR + FOO).equals(p);
                    }
                }
            }
        """
    )

    @Test
    fun acceptHeaderTestExample() = assertChanged(
        before = """
            import java.util.Arrays;
            
            public class TestUtil {
                private static void info(String m, String... args) {
                    System.out.println(m + args);
                }
                private static String get(String a, String b) {
                    return a + b;
                }
                public static String detIP(String req) {
                    String cip = get(req, "X-CLIENT-IP");
                    info("{} {}", "X-CLIENT-IP", cip);
                    if(cip.isEmpty()) {
                        info("no cip from {}, trying {}", "X-CLIENT-IP", "X-FORWARDED-FOR");
                        cip = get(req, "X-FORWARDED-FOR");
                        info("{} {}", "X-FORWARDED-FOR", cip);
                    }
                    info("cip: {}", cip);
                    return cip;
                }
                public static String getXFW(String req) {
                    String cip = "";
                    if(req != null) {
                        String[] ipa = req.split(",");
                        info("{}: {}", "X-FORWARDED-FOR", Arrays.toString(ipa));
                        if(ipa.length > 1) {
                            cip = ipa[0];
                        }
                    }
                    return cip;
                }
            }
        """,
        after = """
            import java.util.Arrays;
            
            public class TestUtil {
                private static final String X_CLIENT_IP = "X-CLIENT-IP";
                private static final String X_FORWARDED_FOR = "X-FORWARDED-FOR";
                private static void info(String m, String... args) {
                    System.out.println(m + args);
                }
                private static String get(String a, String b) {
                    return a + b;
                }
                public static String detIP(String req) {
                    String cip = get(req, X_CLIENT_IP);
                    info("{} {}", X_CLIENT_IP, cip);
                    if(cip.isEmpty()) {
                        info("no cip from {}, trying {}", X_CLIENT_IP, X_FORWARDED_FOR);
                        cip = get(req, X_FORWARDED_FOR);
                        info("{} {}", X_FORWARDED_FOR, cip);
                    }
                    info("cip: {}", cip);
                    return cip;
                }
                public static String getXFW(String req) {
                    String cip = "";
                    if(req != null) {
                        String[] ipa = req.split(",");
                        info("{}: {}", X_FORWARDED_FOR, Arrays.toString(ipa));
                        if(ipa.length > 1) {
                            cip = ipa[0];
                        }
                    }
                    return cip;
                }
            }
        """
    )

    /**
     * This test is disabled because the current recipe does not take the pre-existing 'FOO'
     * property of the super class into account
     */
    @Test
    @Disabled
    fun shadowingTestSuper() = assertChanged(
        before = """
            class Super {
                private static final String FOO = "super";
            }
            class Test extends Super {
                public static boolean f(String p) {
                    if("foo".equals(p)) {
                        return true;
                    } else if(!"fooBar".equals(p)) {
                        return (p + "fooBar" + "foo" + FOO).equals(p);
                    }
                }
            }
        """,
        after = """
            class Super {
                private static final String FOO = "super";
            }
            class Test extends Super {
                private static final String _FOO = "foo";
                private static final String FOO_BAR = "fooBar";
                public static boolean f(String p) {
                    if(FOO.equals(p)) {
                        return true;
                    } else if(!FOO_BAR.equals(p)) {
                        return (p + FOO_BAR + _FOO + FOO).equals(p);
                    }
                }
            }
        """
    )

    /**
     * This test is disabled because the current recipe does not take the pre-existing 'FOO' property into account
     */
    @Test
    @Disabled
    fun shadowingTest() = assertChanged(
        before = """
            class Test {
                private static final String FOO = "predef";
                public static boolean f(String p) {
                    if("foo".equals(p)) {
                        return true;
                    } else if(!"fooBar".equals(p)) {
                        return (p + "fooBar" + "foo" + FOO).equals(p);
                    }
                }
            }
        """,
        after = """
            class Test {
                private static final String FOO = "predef";
                private static final String _FOO = "foo";
                private static final String FOO_BAR = "fooBar";
                public static boolean f(String p) {
                    if(FOO.equals(p)) {
                        return true;
                    } else if(!FOO_BAR.equals(p)) {
                        return (p + FOO_BAR + _FOO + FOO).equals(p);
                    }
                }
            }
        """
    )
}
