package io.github.sinri.keel.helper;


import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashSet;
import java.util.Set;

import static io.github.sinri.keel.facade.KeelInstance.Keel;

/**
 * @since 2.6
 */
public class KeelReflectionHelper {
    private static final KeelReflectionHelper instance = new KeelReflectionHelper();

    private KeelReflectionHelper() {

    }

    static KeelReflectionHelper getInstance() {
        return instance;
    }

    /**
     * @param <T> class of target annotation
     * @return target annotation
     * @since 1.13
     */
    @Nullable
    public <T extends Annotation> T getAnnotationOfMethod(@Nonnull Method method, @Nonnull Class<T> classOfAnnotation, @Nullable T defaultAnnotation) {
        T annotation = method.getAnnotation(classOfAnnotation);
        if (annotation == null) {
            return defaultAnnotation;
        }
        return annotation;
    }

    /**
     * @since 2.6
     */
    @Nullable
    public <T extends Annotation> T getAnnotationOfMethod(@Nonnull Method method, @Nonnull Class<T> classOfAnnotation) {
        return getAnnotationOfMethod(method, classOfAnnotation, null);
    }

    /**
     * @return Returns this element's annotation for the specified type if such an annotation is present, else null.
     * @throws NullPointerException – if the given annotation class is null
     *                              Note that any annotation returned by this method is a declaration annotation.
     * @since 2.8
     */
    @Nullable
    public <T extends Annotation> T getAnnotationOfClass(@Nonnull Class<?> anyClass, @Nonnull Class<T> classOfAnnotation) {
        return anyClass.getAnnotation(classOfAnnotation);
    }

    /**
     * @since 3.1.8
     * For the repeatable annotations.
     */
    @Nonnull
    public <T extends Annotation> T[] getAnnotationsOfClass(@Nonnull Class<?> anyClass, @Nonnull Class<T> classOfAnnotation) {
        return anyClass.getAnnotationsByType(classOfAnnotation);
    }

    /**
     * @param packageName In this package
     * @param baseClass   seek any class implementations of this class
     * @param <R>         the target base class to seek its implementations
     * @return the sought classes in a set
     * @since 3.0.6
     */
    public <R> Set<Class<? extends R>> seekClassDescendantsInPackage(@Nonnull String packageName, @Nonnull Class<R> baseClass) {
//        Reflections reflections = new Reflections(packageName);
//        return reflections.getSubTypesOf(baseClass);

        if (Keel.fileHelper().isRunningFromJAR()) {
            return seekClassDescendantsInPackageForJar(packageName, baseClass);
        } else {
            return seekClassDescendantsInPackageForFileSystem(packageName, baseClass);
        }
    }

    /**
     * @since 3.2.11
     */
    protected <R> Set<Class<? extends R>> seekClassDescendantsInPackageForFileSystem(@Nonnull String packageName, @Nonnull Class<R> baseClass) {
        String packagePath = packageName.replace('.', '/');
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Set<Class<? extends R>> descendantClasses = new HashSet<>();
        try {
            // Assuming classes are in a directory on the file system (e.g., not in a JAR)
            URL resource = classLoader.getResource(packagePath);
            if (resource != null) {
                URI uri = resource.toURI();
                Path startPath = Paths.get(uri);

                Files.walkFileTree(startPath, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (file.toString().endsWith(".class")) {
                            String className = file.toString().replace(".class", "").replace("/", ".");
                            className = className.substring(className.indexOf(packageName));

                            try {
                                Class<? extends R> clazz = (Class<? extends R>) classLoader.loadClass(className);
                                if (
                                        baseClass.isAssignableFrom(clazz)
                                    //&& !baseClass.equals(clazz)
                                ) {
                                    descendantClasses.add(clazz);
                                }
                            } catch (ClassNotFoundException e) {
                                Keel.getLogger().exception(e);
                            }
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            }
        } catch (Exception e) {
            Keel.getLogger().exception(e);
        }
        return descendantClasses;
    }

    /**
     * @since 3.2.11
     */
    protected <R> Set<Class<? extends R>> seekClassDescendantsInPackageForJar(@Nonnull String packageName, @Nonnull Class<R> baseClass) {
        Set<Class<? extends R>> descendantClasses = new HashSet<>();
        Set<String> strings = Keel.fileHelper().seekPackageClassFilesInJar(packageName);
        for (String s : strings) {
            try {
                Class<?> aClass = Class.forName(s);
                if (baseClass.isAssignableFrom(aClass)) {
                    descendantClasses.add((Class<? extends R>) aClass);
                }
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return descendantClasses;
    }

    /**
     * @return Whether the given `baseClass` is the base of the given `implementClass`.
     * @since 3.0.10
     */
    public boolean isClassAssignable(@Nonnull Class<?> baseClass, @Nonnull Class<?> implementClass) {
        return baseClass.isAssignableFrom(implementClass);
    }
}
