/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Based on https://github.com/openrewrite/rewrite/blob/main/rewrite-java/src/main/java/org/openrewrite/java/cleanup/ReplaceDuplicateStringLiterals.java
 */
package org.openrewrite.starter;

import org.jetbrains.annotations.NotNull;
import org.openrewrite.*;
import org.openrewrite.internal.StringUtils;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.ChangeFieldName;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.JavaTemplate;
import org.openrewrite.java.JavaVisitor;
import org.openrewrite.java.search.UsesType;
import org.openrewrite.java.tree.Flag;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.util.*;

public class AvoidDuplicateLiterals extends Recipe {
    @Override
    public String getDisplayName() {
        return "Avoid duplicate literals in a class.";
    }

    @Override
    public String getDescription() {
        return "https://pmd.github.io/pmd-6.8.0/pmd_rules_java_errorprone.html#avoidduplicateliterals.";
    }

    @Nullable
    @Override
    protected TreeVisitor<?, ExecutionContext> getSingleSourceApplicableTest() {
        return new UsesType<>("java.lang.String");
    }

    @Override
    protected JavaVisitor<ExecutionContext> getVisitor() {
        return new JavaVisitor<ExecutionContext>() {

            @Override
            public J visitClassDeclaration(J.ClassDeclaration classDecl, ExecutionContext executionContext) {
                assert(classDecl.getType() != null);
                // I think the assertion should always hold. Otherwise, we have to use the code below.
                // if (classDecl.getType() == null) {
                //     return classDecl;
                // }

                Map<String, Set<J.Literal>> duplicateLiteralsMap = FindDuplicateStringLiterals.find(classDecl);
                if (duplicateLiteralsMap.isEmpty()) {
                    return classDecl;
                }

                Set<String> variableNames = FindVariableNames.find(classDecl);
                Map<String, String> fieldValueToFieldName = FindExistingPrivateStaticFinalFields.find(classDecl);

                String classFqn = classDecl.getType().getFullyQualifiedName();
                for (String valueOfLiteral : duplicateLiteralsMap.keySet()) {
                    String variableName;
                    if (fieldValueToFieldName.containsKey(valueOfLiteral)) {
                        String classFieldName = fieldValueToFieldName.get(valueOfLiteral);
                        variableName = getNameWithoutShadow(classFieldName, variableNames);
                        if (StringUtils.isBlank(variableName)) {
                            continue;
                        }
                        if (!classFieldName.equals(variableName)) {
                            doAfterVisit(new ChangeFieldName<>(classFqn, classFieldName, variableName));
                        }
                    } else {
                        variableName = getNameWithoutShadow(transformToVariableName(valueOfLiteral), variableNames);
                        if (StringUtils.isBlank(variableName)) {
                            continue;
                        }
                        J.Literal replaceLiteral = ((J.Literal) duplicateLiteralsMap.get(valueOfLiteral).toArray()[0]).withId(Tree.randomId());
                        String insertStatement = "private static final String " + variableName + " = #{any(String)}";
                        classDecl = classDecl.withBody(
                                classDecl.getBody().withTemplate(
                                        JavaTemplate.builder(this::getCursor, insertStatement).build(),
                                        classDecl.getBody().getCoordinates().firstStatement(), replaceLiteral));
                    }
                    variableNames.add(variableName);
                    doAfterVisit(new ReplaceStringLiterals(classDecl, variableName, duplicateLiteralsMap.get(valueOfLiteral)));
                }
                return classDecl;
            }

            /**
             * Generate a variable name that does not create a name space conflict.
             * @param name variable name to replace duplicate literals with.
             * @param variableNames variable names that exist in the compilation unit.
             * @return unique variable name.
             */
            private String getNameWithoutShadow(String name, Set<String> variableNames) {
                String transformedName = transformToVariableName(name);
                String newName = transformedName;
                int append = 0;
                while (variableNames.contains(newName)) {
                    append++;
                    newName = transformedName + "_" + append;
                }
                return newName;
            }

            /**
             * Convert a `String` value to a variable name with naming convention of all caps delimited by `_`.
             * Special characters are filtered out to meet regex convention: ^[A-Z][A-Z0-9]*(_[A-Z0-9]+)*$
             */
            private String transformToVariableName(String valueOfLiteral) {
                boolean prevIsLower = false;
                boolean prevIsCharacter = false;
                StringBuilder newName = new StringBuilder();
                for (int i = 0; i < valueOfLiteral.length(); i++) {
                    char c = valueOfLiteral.charAt(i);
                    if (i > 0 && newName.lastIndexOf("_") != newName.length() - 1 &&
                            (Character.isUpperCase(c) && prevIsLower || !prevIsCharacter)) {
                        newName.append("_");
                    }
                    prevIsCharacter = Character.isLetterOrDigit(c);
                    if (!prevIsCharacter) {
                        continue;
                    }
                    if (newName.length() == 0 && Character.isDigit(c)) {
                        newName.append("A_");
                    }
                    newName.append(Character.toUpperCase(c));
                    prevIsLower = Character.isLowerCase(c);
                }
                return newName.toString();
            }
        };
    }

    private static class FindDuplicateStringLiterals extends JavaIsoVisitor<Map<String, Set<J.Literal>>> {

        /**
         * Find duplicate `String` literals repeated 2 or more times.
         *
         * @param inClass subtree to search in.
         * @return `Map` of `String` literal values to the `J.Literal` AST elements.
         */
        public static Map<String, Set<J.Literal>> find(J.ClassDeclaration inClass) {
            Map<String, Set<J.Literal>> literalsMap = new HashMap<>();
            new FindDuplicateStringLiterals().visit(inClass, literalsMap);
            literalsMap.entrySet().removeIf(entry -> entry.getValue().size() == 1);
            return literalsMap;
        }

        private static boolean isStringLiteral(J.Literal literal) {
            return JavaType.Primitive.String.equals(literal.getType()) && literal.getValue() instanceof String;
        }

        public static boolean getParentBetterName(Object is) {
            return is instanceof J.ClassDeclaration ||
                is instanceof J.Annotation ||
                is instanceof J.VariableDeclarations ||
                is instanceof J.NewClass ||
                is instanceof J.MethodInvocation;
        }

        public static boolean isStringLiteralUsage(Cursor parent) {
            return parentIsVarDeclAndFinalAndNotPrivateStatic(parent) ||
                    parent.getValue() instanceof J.NewClass ||
                    parent.getValue() instanceof J.MethodInvocation;
        }

        private static boolean parentIsVarDeclAndFinalAndNotPrivateStatic(Cursor parent) {
            return parent.getValue() instanceof J.VariableDeclarations &&
                    ((J.VariableDeclarations) parent.getValue()).hasModifier(J.Modifier.Type.Final) &&
                    !(((J.VariableDeclarations) parent.getValue()).hasModifier(J.Modifier.Type.Private) && ((J.VariableDeclarations) parent.getValue()).hasModifier(J.Modifier.Type.Static));
        }

        @Override
        public J.Literal visitLiteral(J.Literal literal, Map<String, Set<J.Literal>> literalsMap) {
            if(isStringLiteral(literal)) {
                Cursor parent = getCursor().dropParentUntil(FindDuplicateStringLiterals::getParentBetterName);

                if (isStringLiteralUsage(parent)) {
                    literalsMap.computeIfAbsent(((String) literal.getValue()), k -> new HashSet<>());
                    literalsMap.get((String) literal.getValue()).add(literal);
                }
            }
            return literal;
        }
    }

    private static boolean isPrivateStaticFinalVariable(J.VariableDeclarations declaration) {
        return declaration.hasModifier(J.Modifier.Type.Private) &&
                declaration.hasModifier(J.Modifier.Type.Static) &&
                declaration.hasModifier(J.Modifier.Type.Final);
    }

    private static class FindVariableNames extends JavaIsoVisitor<Set<String>> {

        /**
         * Find all the variable names that exist in the provided subtree.
         *
         * @param inClass subtree to search in.
         * @return variable names that exist in the subtree.
         */
        public static Set<String> find(J.ClassDeclaration inClass) {
            Set<String> variableNames = new HashSet<>();
            new FindVariableNames().visit(inClass, variableNames);
            return variableNames;
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Set<String> variableNames) {
            Cursor parentScope = getCursor().dropParentUntil(is -> is instanceof J.ClassDeclaration || is instanceof J.MethodDeclaration);
            J.VariableDeclarations declaration = getCursor().firstEnclosing(J.VariableDeclarations.class);
            if (parentScope.getValue() instanceof J.MethodDeclaration ||
                    (parentScope.getValue() instanceof J.ClassDeclaration && declaration != null &&
                            // `private static final String`(s) are handled separately by `FindExistingPrivateStaticFinalFields`.
                            !(isPrivateStaticFinalVariable(declaration) && variable.getInitializer() instanceof J.Literal &&
                                    ((J.Literal) variable.getInitializer()).getValue() instanceof String))) {
                variableNames.add(variable.getSimpleName());
            }
            return variable;
        }
    }

    private static class FindExistingPrivateStaticFinalFields extends JavaIsoVisitor<Map<String, String>> {

        /**
         * Find existing `private static final String`(s) in a class.
         */
        public static Map<String, String> find(J j) {
            Map<String, String> fieldValueToFieldName = new LinkedHashMap<>();
            new FindExistingPrivateStaticFinalFields().visit(j, fieldValueToFieldName);
            return fieldValueToFieldName;
        }

        @Override
        public J.VariableDeclarations.NamedVariable visitVariable(J.VariableDeclarations.NamedVariable variable, Map<String, String> stringStringMap) {
            Cursor parentScope = getCursor().dropParentUntil(is -> is instanceof J.ClassDeclaration ||
                    // Prevent checks on most of the literals.
                    is instanceof J.MethodDeclaration);
            J.VariableDeclarations declaration = getCursor().firstEnclosing(J.VariableDeclarations.class);
            if (parentScope.getValue() instanceof J.ClassDeclaration &&
                    declaration != null && isPrivateStaticFinalVariable(declaration) &&
                    variable.getInitializer() instanceof J.Literal &&
                    ((J.Literal) variable.getInitializer()).getValue() instanceof String) {
                String value = (String) (((J.Literal) variable.getInitializer()).getValue());
                stringStringMap.putIfAbsent(value, variable.getSimpleName());
            }
            return variable;
        }
    }

    /**
     * ReplaceStringLiterals in a class with a reference to a `private static final String` with the provided variable name.
     */
    private static class ReplaceStringLiterals extends JavaVisitor<ExecutionContext> {
        private final J.ClassDeclaration isClass;
        private final String variableName;
        private final Set<J.Literal> literals;

        private ReplaceStringLiterals(J.ClassDeclaration isClass, String variableName, Set<J.Literal> literals) {
            this.isClass = isClass;
            this.variableName = variableName;
            this.literals = literals;
        }

        @Override
        public J visitLiteral(J.Literal literal, ExecutionContext executionContext) {
            if (literals.contains(literal)) {
                assert isClass.getType() != null;
                return asIdentifier(literal);
            }

            return literal;
        }

        @NotNull
        private J.Identifier asIdentifier(J.Literal literal) {
            return new J.Identifier(
                    Tree.randomId(),
                    literal.getPrefix(),
                    literal.getMarkers(),
                    variableName,
                    JavaType.Primitive.String,
                    new JavaType.Variable(
                            Flag.flagsToBitMap(new HashSet<>(Arrays.asList(Flag.Private, Flag.Static, Flag.Final))),
                            variableName,
                            isClass.getType(),
                            JavaType.Primitive.String,
                            Collections.emptyList()
                    )
            );
        }
    }
}
