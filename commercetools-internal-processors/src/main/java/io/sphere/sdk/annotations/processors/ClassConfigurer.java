package io.sphere.sdk.annotations.processors;

import io.sphere.sdk.annotations.FactoryMethod;
import io.sphere.sdk.annotations.ResourceDraftValue;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.joining;

/**
 * high level configuration util to generate classes based on an existing class or interface
 */
final class ClassConfigurer {
    public static final Predicate<Element> BEAN_GETTER_PREDICATE = e -> ElementKind.METHOD.equals(e.getKind()) && e.getSimpleName().toString().matches("^get[A-Z].*");

    public static SourceHolder ofSource(final TypeElement typeElement) {
        return new SourceHolder(typeElement);
    }

    public static class SourceHolder {
        private final TypeElement typeElement;

        private SourceHolder(final TypeElement typeElement) {
            this.typeElement = typeElement;
        }

        public PackageHolder samePackageFromSource() {
            return packageFrom(typeElement);
        }

        public PackageHolder packageFrom(final TypeElement typeElement) {
            final String packageName = packageName(typeElement);
            return new PackageHolder(packageName, typeElement);
        }
    }

    public static class PackageHolder {
        private final String packageName;
        private final TypeElement typeElement;

        private PackageHolder(final String packageName, final TypeElement typeElement) {
            this.packageName = packageName;
            this.typeElement = typeElement;
        }

        public ImportHolder imports() {
            return new ImportHolder(typeElement, this);
        }
    }

    public static class ImportHolder {
        private final TypeElement typeElement;
        private final List<String> imports = new LinkedList<>();
        private final PackageHolder packageHolder;

        public ImportHolder(final TypeElement typeElement, final PackageHolder packageHolder) {
            this.typeElement = typeElement;
            this.packageHolder = packageHolder;
        }

        public ImportHolder defaultImports() {
            return addImport("javax.annotation.Nullable");
        }

        public ImportHolder addImport(final String s) {
            imports.add(s);
            return this;
        }

        public JavadocHolder classJavadoc(final String s) {
            return new JavadocHolder(typeElement, this, s);
        }
    }

    public static class JavadocHolder {
        private final TypeElement typeElement;
        private final ImportHolder importHolder;
        private final String javadoc;

        public JavadocHolder(final TypeElement typeElement, final ImportHolder importHolder, final String javadoc) {
            this.typeElement = typeElement;
            this.importHolder = importHolder;
            this.javadoc = javadoc;
        }

        public ClassModifierHolder modifiers(final String ... modifiers) {
            return modifiers(asList(modifiers));
        }

        public ClassModifierHolder modifiers(final List<String> modifiers) {
            return new ClassModifierHolder(modifiers, this);
        }
    }

    public static class ClassModifierHolder {
        private final List<String> modifiers;
        private final JavadocHolder holder;

        private ClassModifierHolder(final List<String> modifiers, final JavadocHolder holder) {
            this.modifiers = modifiers;
            this.holder = holder;
        }

        public ClassTypeHolder classType() {
            return new ClassTypeHolder(ClassType.CLASS, this, holder.typeElement);
        }
    }

    public static class ClassTypeHolder {
        private final ClassType classType;
        private final ClassModifierHolder classModifierHolder;
        private final TypeElement typeElement;

        private ClassTypeHolder(final ClassType classType, final ClassModifierHolder classModifierHolder, final TypeElement typeElement) {
            this.classType = classType;
            this.classModifierHolder = classModifierHolder;
            this.typeElement = typeElement;
        }

        public ClassNameHolder className(final UnaryOperator<String> op) {
            return new ClassNameHolder(op.apply(this.classModifierHolder.holder.importHolder.packageHolder.typeElement.getSimpleName().toString()), this, typeElement);
        }
    }

    public static class ClassNameHolder {
        private final TypeElement typeElement;
        private final ClassModelBuilder builder;

        private ClassNameHolder(final String name, final ClassTypeHolder classTypeHolder, final TypeElement typeElement) {
            this.typeElement = typeElement;
            this.builder = ClassModelBuilder.of(name, classTypeHolder.classType);
            this.builder.packageName(classTypeHolder.classModifierHolder.holder.importHolder.packageHolder.packageName);
            classTypeHolder.classModifierHolder.holder.importHolder.imports.forEach(imp -> builder.addImport(imp));
            classTypeHolder.classModifierHolder.modifiers.forEach(modifier -> builder.addModifiers(modifier));
            builder.setJavadoc(classTypeHolder.classModifierHolder.holder.javadoc);
        }

        public BaseClassHolder extending(final String baseClassName) {
            return new BaseClassHolder(baseClassName, builder, typeElement);
        }

        public BaseClassHolder extending(final Class<?> baseClass) {
            return extending(baseClass.getName());
        }
    }

    public static class BaseClassHolder {
        private final ClassModelBuilder builder;
        private final TypeElement typeElement;

        private BaseClassHolder(final String baseClassName, final ClassModelBuilder builder, final TypeElement typeElement) {
            this.builder = builder;
            this.typeElement = typeElement;
            builder.setBaseClassName(baseClassName);
        }

        public InterfacesHolder implementingBasedOnSourceName(final Function<String, String> op) {
            return implementing(op.apply(typeElement.getSimpleName().toString()));
        }

        public InterfacesHolder implementing(final String ... interfaces) {
            return new InterfacesHolder(asList(interfaces), builder, typeElement);
        }

        public InterfacesHolder implementing(final TypeElement typeElement) {
            return implementing(typeElement.getSimpleName().toString());
        }
    }

    public static class InterfacesHolder {
        private final ClassModelBuilder builder;
        private final TypeElement typeElement;

        private InterfacesHolder(final List<String> interfaces, final ClassModelBuilder builder, final TypeElement typeElement) {
            this.builder = builder;
            this.typeElement = typeElement;
            builder.interfaces(interfaces);
        }

        public FieldsHolder fields() {
            return new FieldsHolder(builder, typeElement);
        }
    }

    public static class FieldsHolder {
        private final ClassModelBuilder builder;
        private final TypeElement typeElement;

        private FieldsHolder(final ClassModelBuilder builder, final TypeElement typeElement) {
            this.builder = builder;
            this.typeElement = typeElement;
        }

        public FieldsHolder fields(final List<FieldModel> fields) {
            fields.forEach(builder::addField);
            return this;
        }

        public FieldsHolder fieldsFromInterfaceBeanGetters(final boolean finalFields) {
            final Stream<? extends Element> elementStream = elementStreamIncludingInterfaces();
            final List<FieldModel> fields = elementStream
                    .filter(BEAN_GETTER_PREDICATE)
                    .map(DistinctElementWrapper::new)
                    .distinct()
                    .map(wrapper -> wrapper.element)
                    .map(typeElement -> createField(typeElement, finalFields))
                    .collect(Collectors.toList());
            return fields(fields);
        }

        private Stream<? extends Element> elementStreamIncludingInterfaces() {
            final Stream<? extends Element> interfacesMethodsStream = typeElement.getInterfaces().stream()
                    .filter(i -> i instanceof DeclaredType)
                    .map(i -> (DeclaredType) i)
                    .flatMap(i -> i.asElement().getEnclosedElements().stream());
            return Stream.concat(typeElement.getEnclosedElements().stream(), interfacesMethodsStream);
        }

        private static class DistinctElementWrapper {
            private final Element element;

            private DistinctElementWrapper(final Element element) {
                this.element = element;
            }

            @Override
            public boolean equals(final Object o) {
                if (this == o) return true;
                if (!(o instanceof DistinctElementWrapper)) return false;
                final DistinctElementWrapper that = (DistinctElementWrapper) o;
                return Objects.equals(element.getSimpleName(), that.element.getSimpleName());
            }

            @Override
            public int hashCode() {
                return Objects.hash(element.getSimpleName());
            }
        }

        public ConstructorsHolder constructors() {
            return new ConstructorsHolder(builder, typeElement);
        }
    }

    public static class ConstructorsHolder {
        private final ClassModelBuilder builder;
        private final TypeElement typeElement;

        private ConstructorsHolder(final ClassModelBuilder builder, final TypeElement typeElement) {
            this.builder = builder;
            this.typeElement = typeElement;
        }

        public ConstructorsHolder constructorForAllFields() {
            final MethodModel c = new MethodModel();
            //no modifiers since it should be package scope
            final List<MethodParameterModel> parameters = parametersForInstanceFields(builder);
            c.setParameters(parameters);
            c.setName(builder.getName());
            c.setAnnotations(singletonList(createJsonCreatorAnnotation()));
            c.setBody(Templates.render("fieldAssignments", singletonMap("assignments", parameters)));
            builder.addImport("com.fasterxml.jackson.annotation.JsonCreator");
            builder.addConstructor(c);
            return new ConstructorsHolder(builder, typeElement);
        }

        public MethodsHolder methods() {
            return new MethodsHolder(builder, typeElement);
        }
    }

    public static class MethodsHolder {
        private final ClassModelBuilder builder;
        private final TypeElement typeElement;

        private MethodsHolder(final ClassModelBuilder builder, final TypeElement typeElement) {
            this.builder = builder;
            this.typeElement = typeElement;
        }

        public MethodsHolder factoryMethodsAccordingToAnnotations() {
            final ResourceDraftValue annotation = typeElement.getAnnotation(ResourceDraftValue.class);
            if (annotation != null) {
                for (final FactoryMethod factoryMethod : annotation.factoryMethods()) {
                    addFactoryMethod(builder, factoryMethod);
                }
            }
            return this;
        }

        public MethodsHolder builderMethods() {
            addBuilderMethods(builder, typeElement);
            return this;
        }

        public MethodsHolder buildMethod() {
            addBuildMethod(builder, typeElement);
            return this;
        }

        public ClassModel build() {
            return builder.build();
        }

        public MethodsHolder gettersForFields() {
            final List<FieldModel> fieldModels = instanceFieldsSorted(builder);
            fieldModels.forEach(field -> builder.addMethod(createGetter(field)));
            return this;
        }

        public MethodsHolder withers() {
            addDslMethods(builder, typeElement);
            addNewBuilderMethod(builder, typeElement);
            return this;
        }

        public MethodsHolder factoryMethodFromInterfaceInstance(final boolean finalFields) {
            final MethodModel m = new MethodModel();
            m.setModifiers(asList("public", "static"));
            m.setName("of");
            m.setReturnType(builder.getName());
            final MethodParameterModel p = new MethodParameterModel();
            p.setModifiers(singletonList("final"));
            p.setType(typeElement.getSimpleName().toString());
            p.setName("template");
            m.setParameters(singletonList(p));
            final String dsd = typeElement.getEnclosedElements()
                    .stream()
                    .filter(BEAN_GETTER_PREDICATE)
                    .map(element -> {
                        final FieldModel field = createField(element, finalFields);
                        return ImmutablePair.of(field.getName(), element.getSimpleName().toString());
                    })
                    .sorted(Comparator.comparing(ImmutablePair::getLeft))
                    .map(pair -> format("template.%s()", pair.getRight()))
                    .collect(joining(", "));
            final String body = "return new " + builder.getName() + "(" + dsd + ");";
            m.setBody(body);
            builder.addMethod(m);
            return this;
        }

        public <A extends Annotation> MethodsHolder additionalContents(final Class<A> a, final Function<A, String[]> op) {
            final A annotation = typeElement.getAnnotation(a);
            if (annotation != null) {
                final String[] thingsToAdd = op.apply(annotation);
                final String s = Arrays.stream(thingsToAdd).collect(joining("\n"));
                builder.setAdditions(s);
            }
            return this;
        }
    }

    private static MethodModel createGetter(final FieldModel field) {
        final String methodName = "get" + StringUtils.capitalize(field.getName());
        final MethodModel method = new MethodModel();
        method.setName(methodName);
        method.setReturnType(field.getType());
        method.setBody("return " + field.getName() + ";");
        method.addModifiers("public");

        field.getAnnotations()
                .stream()
                .filter(a -> "Nullable".equals(a.getName()))
                .findFirst()
                .ifPresent(a -> method.setAnnotations(singletonList(createNullableAnnotation())));

        final List<AnnotationModel> list = new LinkedList<>(method.getAnnotations());
        final AnnotationModel a = new AnnotationModel();
        a.setName("Override");
        list.add(a);
        method.setAnnotations(list);
        return method;
    }

    private static void addDslMethods(final ClassModelBuilder builder, final TypeElement typeElement) {
        typeElement.getEnclosedElements()
                .stream()
                .filter(BEAN_GETTER_PREDICATE)
                .forEach(element -> addDslMethod(element, builder));
    }

    private static void addDslMethod(final Element element, final ClassModelBuilder builder) {
        final String fieldName = fieldNameFromGetter(element.toString());
        final MethodModel method = new MethodModel();
        final String name = witherNameFromGetter(element);
        method.setName(name);
        method.setReturnType(builder.getName());
        method.addModifiers("public");
        method.setParameters(singletonList(getBuilderMethodParameterModel(element, fieldName)));
        final HashMap<String, Object> values = new HashMap<>();
        values.put("fieldName", fieldName);
        method.setBody(Templates.render("witherMethodBody", values));
        builder.addMethod(method);
    }

    private static void addNewBuilderMethod(final ClassModelBuilder builder, final TypeElement typeElement) {
        final MethodModel method = new MethodModel();
        method.setModifiers(singletonList("private"));
        final String associatedBuilderName = ResourceDraftBuilderClassModelFactory.builderName(typeElement);
        builder.addImport(builder.build().getPackageName() + "." + associatedBuilderName);
        method.setReturnType(associatedBuilderName);
        method.setName("newBuilder");
        method.setBody("return new " + associatedBuilderName + "(" + fieldNamesSortedString(builder) + ");");
        builder.addMethod(method);
    }

    private static FieldModel createField(final Element element, final boolean finalFields) {
        final String methodName = element.getSimpleName().toString();
        final String fieldName = fieldNameFromGetter(methodName);
        final FieldModel field = new FieldModel();
        field.addModifiers("private");
        if (finalFields) {
            field.addModifiers("final");
        }
        field.setType(getType(element));
        field.setName(fieldName);
        element.getAnnotationMirrors().forEach(a -> {
            final String annotationName = a.getAnnotationType().asElement().getSimpleName().toString();
            if ("Nullable".equals(annotationName)) {
                field.setAnnotations(singletonList(createNullableAnnotation()));
            }
        });
        return field;
    }

    private static void addFactoryMethod(final ClassModelBuilder builder, final FactoryMethod factoryMethod) {
        final String name = factoryMethod.methodName();
        final MethodModel method = new MethodModel();
        method.setName(name);
        method.setModifiers(asList("public", "static"));
        method.setParameters(createParameterModels(builder, factoryMethod));
        method.setReturnType(builder.getName());
        final List<String> parameterNameList = asList(factoryMethod.parameterNames());
        final String constructorParameters = parametersForInstanceFields(builder).stream()
                .map(p -> parameterNameList.contains(p.getName()) ? p.getName() : null)
                .collect(joining(", "));
        method.setBody("return new " + builder.getName() + "(" + constructorParameters +");");
        builder.addMethod(method);
    }

    private static List<MethodParameterModel> createParameterModels(final ClassModelBuilder builder, final FactoryMethod factoryMethod) {
        return Arrays.stream(factoryMethod.parameterNames())
                .map(parameterName -> {
                    final FieldModel field = getField(builder, parameterName);
                    final MethodParameterModel m = new MethodParameterModel();
                    m.setName(parameterName);
                    m.setType(field.getType());
                    m.setModifiers(asList("final"));
                    return m;
                })
                .collect(Collectors.toList());
    }

    private static FieldModel getField(final ClassModelBuilder builder, final String fieldName) {
        return builder.build().getFields().stream()
                .filter(f -> f.getName().equals(fieldName))
                .findFirst().orElseThrow(() -> new RuntimeException("field " + fieldName + " not found in " + builder));
    }

    private static List<MethodParameterModel> parametersForInstanceFields(final ClassModelBuilder builder) {
        return builder.build().getFields().stream()
                .filter(f -> !f.getModifiers().contains("static"))
                .sorted(Comparator.comparing(f -> f.getName()))
                .map(f -> {
                    final MethodParameterModel p = new MethodParameterModel();
                    p.setModifiers(asList("final"));
                    p.setType(f.getType());
                    p.setName(f.getName());
                    return p;
                })
                .collect(Collectors.toList());
    }

    private static String witherNameFromGetter(final Element element) {
        return "with" + StringUtils.capitalize(fieldNameFromGetter(element.toString()));
    }

    private static void addBuilderMethod(final Element element, final ClassModelBuilder builder) {
        final String methodName = element.getSimpleName().toString();
        final String fieldName = fieldNameFromGetter(methodName);
        final MethodModel method = new MethodModel();
        method.setName(fieldName);
        method.setReturnType(builder.getName());
        method.addModifiers("public");
        method.setParameters(singletonList(getBuilderMethodParameterModel(element, fieldName)));
        final HashMap<String, Object> values = new HashMap<>();
        values.put("fieldName", fieldName);
        method.setBody(Templates.render("builderMethodBody", values));
        builder.addMethod(method);
    }

    private static void addBuilderMethods(final ClassModelBuilder builder, final TypeElement typeElement) {
        typeElement.getEnclosedElements()
                .stream()
                .filter(BEAN_GETTER_PREDICATE)
                .forEach(element -> addBuilderMethod(element, builder));
    }

    private static void addBuildMethod(final ClassModelBuilder builder, final TypeElement typeElement) {
        final MethodModel method = new MethodModel();
        method.addModifiers("public");
        final String dslName = ResourceDraftDslClassModelFactory.dslName(typeElement);
        method.setReturnType(dslName);
        method.setName("build");
        method.setBody("return new " + dslName + "(" + fieldNamesSortedString(builder) + ");");
        builder.addMethod(method);
    }

    private static String packageName(final TypeElement typeElement) {
        final PackageElement packageElement = (PackageElement) typeElement.getEnclosingElement();
        return packageElement.getQualifiedName().toString();
    }

    private static AnnotationModel createJsonCreatorAnnotation() {
        final AnnotationModel jsonCreator = new AnnotationModel();
        jsonCreator.setName("JsonCreator");
        return jsonCreator;
    }

    private static AnnotationModel createNullableAnnotation() {
        final AnnotationModel am = new AnnotationModel();
        am.setName("Nullable");
        return am;
    }

    private static String fieldNameFromGetter(final String methodName) {
        final String s1 = methodName.toString().replaceAll("^get", "");
        final String s = ("" + s1.charAt(0)).toLowerCase() + s1.substring(1);
        return StringUtils.substringBefore(s, "(");
    }

    private static String getType(final Element element) {
        final ReturnTypeElementVisitor visitor = new ReturnTypeElementVisitor();
        return element.accept(visitor, null);
    }

    private static MethodParameterModel getBuilderMethodParameterModel(final Element element, final String fieldName) {
        final MethodParameterModel parameter = new MethodParameterModel();
        parameter.setModifiers(singletonList("final"));
        parameter.setName(fieldName);
        final String type = getType(element);
        parameter.setType(type);
        element.getAnnotationMirrors().forEach(a -> {
            final String annotationName = a.getAnnotationType().asElement().getSimpleName().toString();
            if ("Nullable".equals(annotationName)) {
                parameter.setAnnotations(singletonList(createNullableAnnotation()));
            }
        });
        return parameter;
    }

    private static List<String> fieldNamesSorted(final ClassModelBuilder builder) {
        return instanceFieldsSorted(builder)
                .stream()
                .map(field -> field.getName())
                .collect(Collectors.toList());
    }

    private static List<FieldModel> instanceFieldsSorted(final ClassModelBuilder builder) {
        return builder.build().getFields().stream()
                .filter(f -> !f.getModifiers().contains("static"))
                .sorted(Comparator.comparing(f -> f.getName()))
                .collect(Collectors.toList());
    }

    private static String fieldNamesSortedString(final ClassModelBuilder builder) {
        return fieldNamesSorted(builder).stream().collect(joining(", "));
    }
}