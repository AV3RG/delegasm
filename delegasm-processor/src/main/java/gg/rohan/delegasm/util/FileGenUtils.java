/*
This file is part of Delegasm, licensed under the MIT License.

Copyright (c) 2025 Rohan Goyal

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package gg.rohan.delegasm.util;

import com.squareup.javapoet.*;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for generating Java source files using JavaPoet.
 * This class handles the generation of delegation classes that forward method calls
 * to inner composed types.
 */
public class FileGenUtils {

    private FileGenUtils() {
        // Private constructor to prevent instantiation
        throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Generates a Java source file containing an abstract class that delegates to inner composed types.
     *
     * @param processingEnv The annotation processing environment
     * @param packageName The package name for the generated class
     * @param className The name of the generated class
     * @param innerTypes List of pairs containing the inner types and their field names
     */
    public static void generateJavaFile(
            ProcessingEnvironment processingEnv,
            String packageName,
            String className,
            List<Pair<DeclaredType, String>> innerTypes
    ) {
        // Map inner types to their abstract methods
        Map<Pair<DeclaredType, String>, Set<ExecutableElement>> methodMap = createMethodMap(processingEnv, innerTypes);

        // Build the class specification
        TypeSpec classSpec = buildClassSpec(className, innerTypes, methodMap, processingEnv);

        // Generate and write the file
        writeFile(processingEnv, packageName, classSpec);
    }

    private static Map<Pair<DeclaredType, String>, Set<ExecutableElement>> createMethodMap(
            ProcessingEnvironment processingEnv,
            List<Pair<DeclaredType, String>> innerTypes
    ) {
        return innerTypes.stream()
                .collect(Collectors.toMap(
                        pair -> pair,
                        pair -> getAbstractMethods(processingEnv, pair.getFirst())
                ));
    }

    private static Set<ExecutableElement> getAbstractMethods(
            ProcessingEnvironment processingEnv,
            DeclaredType type
    ) {
        return processingEnv.getElementUtils()
                .getAllMembers((TypeElement) type.asElement())
                .stream()
                .filter(member -> member instanceof ExecutableElement)
                .map(member -> (ExecutableElement) member)
                .filter(member -> member.getModifiers().contains(Modifier.ABSTRACT) ||
                               member.getModifiers().contains(Modifier.DEFAULT))
                .collect(Collectors.toSet());
    }

    private static TypeSpec buildClassSpec(
            String className,
            List<Pair<DeclaredType, String>> innerTypes,
            Map<Pair<DeclaredType, String>, Set<ExecutableElement>> methodMap,
            ProcessingEnvironment processingEnv
    ) {
        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.ABSTRACT)
                .addJavadoc("Simple Delegation class for " + className + "\n");

        // Add type variables and interfaces
        addTypeVariablesAndInterfaces(classBuilder, innerTypes);

        // Add fields and constructor
        addFieldsAndConstructor(classBuilder, innerTypes);

        // Add delegating methods
        addDelegatingMethods(classBuilder, methodMap, processingEnv);

        return classBuilder.build();
    }

    private static void addTypeVariablesAndInterfaces(
            TypeSpec.Builder classBuilder,
            List<Pair<DeclaredType, String>> innerTypes
    ) {
        innerTypes.forEach(pair -> {
            List<TypeVariableName> typeVariables = ((TypeElement) pair.getFirst().asElement())
                    .getTypeParameters()
                    .stream()
                    .map(TypeVariableName::get)
                    .collect(Collectors.toList());
            classBuilder.addTypeVariables(typeVariables);
            classBuilder.addSuperinterface(ParameterizedTypeName.get(pair.getFirst().asElement().asType()));
        });
    }

    private static void addFieldsAndConstructor(
            TypeSpec.Builder classBuilder,
            List<Pair<DeclaredType, String>> innerTypes
    ) {
        // Add fields
        innerTypes.forEach(pair -> {
            FieldSpec field = FieldSpec.builder(
                    TypeName.get(pair.getFirst()),
                    pair.getSecond(),
                    Modifier.FINAL,
                    Modifier.PRIVATE
            ).build();
            classBuilder.addField(field);
        });

        // Add constructor
        MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder();
        innerTypes.forEach(pair -> {
            constructorBuilder
                    .addParameter(TypeName.get(pair.getFirst()), pair.getSecond(), Modifier.FINAL)
                    .addCode(CodeBlock.builder()
                            .addStatement("this.$L = $L", pair.getSecond(), pair.getSecond())
                            .build());
        });
        classBuilder.addMethod(constructorBuilder.build());
    }

    private static void addDelegatingMethods(
            TypeSpec.Builder classBuilder,
            Map<Pair<DeclaredType, String>, Set<ExecutableElement>> methodMap,
            ProcessingEnvironment processingEnv
    ) {
        methodMap.forEach((descriptor, methods) -> {
            Set<MethodSpec> methodSpecs = methods.stream()
                    .map(method -> createDelegatingMethod(method, descriptor, processingEnv))
                    .collect(Collectors.toSet());
            classBuilder.addMethods(methodSpecs);
        });
    }

    private static MethodSpec createDelegatingMethod(
            ExecutableElement method,
            Pair<DeclaredType, String> descriptor,
            ProcessingEnvironment processingEnv
    ) {
        String parameters = method.getParameters().stream()
                .map(param -> param.getSimpleName().toString())
                .collect(Collectors.joining(","));

        String returnPrefix = method.getReturnType().getKind() == TypeKind.VOID ? "" : "return ";

        return MethodSpec.overriding(method, descriptor.getFirst(), processingEnv.getTypeUtils())
                .addCode(CodeBlock.builder()
                        .addStatement(returnPrefix + descriptor.getSecond() + "." +
                                method.getSimpleName() + "(" + parameters + ")")
                        .build())
                .build();
    }

    private static void writeFile(
            ProcessingEnvironment processingEnv,
            String packageName,
            TypeSpec classSpec
    ) {
        try {
            JavaFile.builder(packageName, classSpec)
                    .build()
                    .writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write generated file", e);
        }
    }
}
