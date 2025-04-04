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
package gg.rohan.delegasm;

import gg.rohan.delegasm.util.FileGenUtils;
import gg.rohan.delegasm.util.NullUtils;
import gg.rohan.delegasm.util.Pair;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor8;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Annotation processor for the @Delegasm annotation.
 * This processor generates delegation code for classes annotated with @Delegasm,
 * creating abstract classes that delegate to inner composed types.
 */
public class DelegasmProcessor extends AbstractProcessor {

    /**
     * Default constructor for the DelegasmProcessor.
     * This constructor is used by the annotation processing framework.
     */
    public DelegasmProcessor() {
        super();
    }

    private ProcessingEnvironment processingEnv;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        this.processingEnv = processingEnv;
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (this.processingEnv == null) {
            throw new IllegalStateException("Processing environment is null. This should not happen.");
        }

        // Get all classes annotated with @Delegasm
        Set<? extends Element> annotatedClasses = getAnnotatedClasses(roundEnv);

        // Process each annotated class
        annotatedClasses.stream()
                .map(this::extractAnnotationData)
                .map(this::validateAndExtractDelegationTypes)
                .forEach(this::generateDelegationClass);

        return false;
    }

    private Set<? extends Element> getAnnotatedClasses(RoundEnvironment roundEnv) {
        return roundEnv.getElementsAnnotatedWith(Delegasm.class)
                .stream()
                .filter(element -> element.getKind() == ElementKind.CLASS)
                .collect(Collectors.toSet());
    }

    private Pair<Element, List<Element>> extractAnnotationData(Element element) {
        AnnotationMirror annotationMirror = findDelegasmAnnotation(element);
        if (annotationMirror == null) {
            throw new IllegalStateException("Could not find Delegasm annotation on class " + element.getSimpleName());
        }

        Map<String, AnnotationValue> annotationValues = extractAnnotationValues(annotationMirror);
        List<Element> delegationElements = extractDelegationElements(annotationValues);
        
        return Pair.of(element, delegationElements);
    }

    private AnnotationMirror findDelegasmAnnotation(Element element) {
        return element.getAnnotationMirrors().stream()
                .filter(mirror -> {
                    Name annotationName = ((QualifiedNameable) mirror.getAnnotationType().asElement()).getQualifiedName();
                    return annotationName.contentEquals(Delegasm.class.getCanonicalName());
                })
                .findFirst()
                .orElse(null);
    }

    private Map<String, AnnotationValue> extractAnnotationValues(AnnotationMirror annotationMirror) {
        Map<String, AnnotationValue> values = new HashMap<>();
        annotationMirror.getElementValues().forEach((key, value) -> 
            values.put(key.getSimpleName().toString(), value));
        return values;
    }

    private List<Element> extractDelegationElements(Map<String, AnnotationValue> values) {
        AnnotationValue valueField = values.get("value");
        AnnotationValue multiField = values.get("multi");

        if (!NullUtils.xor(valueField, multiField)) {
            throw new IllegalStateException("Exactly one of value or multi must be present on Delegasm annotation");
        }

        List<Element> elements = new ArrayList<>();
        if (valueField != null) {
            Element element = extractSingleElement(valueField);
            if (element != null) {
                elements.add(element);
            }
        }
        if (multiField != null) {
            List<Element> multiElements = extractMultiElements(multiField);
            if (multiElements != null) {
                elements.addAll(multiElements);
            }
        }
        return elements;
    }

    private Element extractSingleElement(AnnotationValue value) {
        return value.accept(new SimpleAnnotationValueVisitor8<Element, Void>() {
            @Override
            public Element visitType(TypeMirror t, Void unused) {
                return t.getKind() == TypeKind.VOID ? null : processingEnv.getTypeUtils().asElement(t);
            }
        }, null);
    }

    private List<Element> extractMultiElements(AnnotationValue value) {
        return value.accept(new SimpleAnnotationValueVisitor8<List<Element>, Void>() {
            @Override
            public List<Element> visitType(TypeMirror t, Void unused) {
                return t.getKind() == TypeKind.VOID ? null : 
                    Collections.singletonList(processingEnv.getTypeUtils().asElement(t));
            }

            @Override
            public List<Element> visitArray(List<? extends AnnotationValue> values, Void unused) {
                return values.stream()
                        .flatMap(v -> v.accept(this, null).stream())
                        .collect(Collectors.toList());
            }
        }, null);
    }

    private Pair<TypeElement, List<DeclaredType>> validateAndExtractDelegationTypes(
            Pair<Element, List<Element>> data) {
        TypeElement element = (TypeElement) data.getFirst();
        List<Element> delegations = data.getSecond();

        List<DeclaredType> types = element.getInterfaces().stream()
                .flatMap(declaredType -> Stream.concat(
                        Stream.of(declaredType),
                        processingEnv.getTypeUtils().directSupertypes(declaredType).stream()
                                .filter(typeMirror -> typeMirror.getKind().equals(TypeKind.DECLARED))))
                .map(typeMirror -> (DeclaredType) typeMirror)
                .filter(declaredType -> delegations.contains(declaredType.asElement()))
                .collect(Collectors.toList());

        if (types.size() != delegations.size()) {
            throw new IllegalStateException(
                    "Mismatch between interfaces found and delegation targets specified. " +
                    "Please ensure your type implements all delegation targets.");
        }

        return Pair.of(element, types);
    }

    private void generateDelegationClass(Pair<TypeElement, List<DeclaredType>> data) {
        TypeElement element = data.getFirst();
        List<DeclaredType> types = data.getSecond();
        
        List<Pair<DeclaredType, String>> namedTypes = new ArrayList<>();
        for (int i = 0; i < types.size(); i++) {
            namedTypes.add(Pair.of(types.get(i), "delegasmic" + i));
        }

        FileGenUtils.generateJavaFile(
                this.processingEnv,
                element.getQualifiedName().toString().replace("." + element.getSimpleName(), ""),
                "Delegasm_" + element.getSimpleName().toString(),
                namedTypes
        );
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(Delegasm.class.getCanonicalName());
    }
}
