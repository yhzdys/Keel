package io.github.sinri.keel.helper;

import io.vertx.core.Future;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.CodeSource;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static io.github.sinri.keel.facade.KeelInstance.Keel;

/**
 * @since 2.6
 */
public class KeelFileHelper {
    private static final KeelFileHelper instance = new KeelFileHelper();

    private KeelFileHelper() {

    }

    static KeelFileHelper getInstance() {
        return instance;
    }

    public @Nonnull byte[] readFileAsByteArray(@Nonnull String filePath, boolean seekInsideJarWhenNotFound) throws IOException {
        try {
            return Files.readAllBytes(Path.of(filePath));
        } catch (IOException e) {
            if (seekInsideJarWhenNotFound) {
                try (
                        InputStream resourceAsStream = KeelFileHelper.class.getClassLoader().getResourceAsStream(filePath)
                ) {
                    if (resourceAsStream == null) {
                        // not found resource
                        throw new IOException("file also not in jar", e);
                    }
                    return resourceAsStream.readAllBytes();
                }

//                URL resource = KeelOptions.class.getClassLoader().getResource(filePath);
//                if (resource == null) {
//                    throw new IOException("Embedded one is not found after not found in FS: " + filePath, e);
//                }
//                String file = resource.getFile();
//                return Files.readAllBytes(Path.of(file));
            } else {
                throw e;
            }
        }
    }

    /**
     * @param filePath path string of the target file, or directory
     * @return the URL of target file; if not there, null return.
     */
    @Nullable
    public URL getUrlOfFileInJar(@Nonnull String filePath) {
        return KeelFileHelper.class.getClassLoader().getResource(filePath);
    }

    /**
     * Seek in JAR, under the root (exclusive)
     *
     * @param root ends with '/'
     * @return list of JarEntry
     */
    @Nonnull
    public List<JarEntry> traversalInJar(@Nonnull String root) {
        List<JarEntry> jarEntryList = new ArrayList<>();
        try {
            // should root ends with '/'?
            URL url = KeelFileHelper.class.getClassLoader().getResource(root);
            if (url == null) {
                throw new RuntimeException("Resource is not found");
            }
            if (!url.toString().contains("!/")) {
                throw new RuntimeException("Resource is not in JAR");
            }
            String jarPath = url.toString().substring(0, url.toString().indexOf("!/") + 2);

            URL jarURL = new URL(jarPath);
            JarURLConnection jarCon = (JarURLConnection) jarURL.openConnection();
            JarFile jarFile = jarCon.getJarFile();
            Enumeration<JarEntry> jarEntries = jarFile.entries();
            var baseJarEntry = jarFile.getJarEntry(root);
            var pathOfBaseJarEntry = Path.of(baseJarEntry.getName());

            while (jarEntries.hasMoreElements()) {
                JarEntry entry = jarEntries.nextElement();

                Path entryPath = Path.of(entry.getName());
                if (entryPath.getParent() == null) {
                    continue;
                }
                if (entryPath.getParent().compareTo(pathOfBaseJarEntry) == 0) {
                    jarEntryList.add(entry);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return jarEntryList;
    }

    /**
     * @return absolute created Temp File path
     * @since 3.0.0
     */
    public Future<String> crateTempFile(@Nullable String prefix, @Nullable String suffix) {
        return Keel.getVertx().fileSystem().createTempFile(prefix, suffix);
    }

    /**
     * @since 3.2.11
     * Check if this process is running with JAR file.
     */
    public boolean isRunningFromJAR() {
        CodeSource src = this.getClass().getProtectionDomain().getCodeSource();
        if (src == null) {
            throw new RuntimeException();
        }
        URL location = src.getLocation();
        if (location == null) {
            throw new RuntimeException();
        }
        // System.out.println("src.getLocation: "+location.toString());
        return location.toString().endsWith(".jar");

//        ZipInputStream zip = new ZipInputStream(jar.openStream());
//        while (true) {
//            ZipEntry e = zip.getNextEntry();
//            if (e == null)
//                break;
//            String name = e.getName();
//            if (name.startsWith("path/to/your/dir/")) {
//                /* Do something with this entry. */
//            }
//        }
    }

    /**
     * The in-class classes, i.e. subclasses, would be neglected.
     *
     * @since 3.2.11
     */
    public Set<String> seekPackageClassFilesInJar(@Nonnull String packageName) {
        Set<String> classes = new HashSet<>();
        // Get the current class's class loader
        ClassLoader classLoader = this.getClass().getClassLoader();

        // Get the URL of the JAR file containing the current class
        URL jarUrl = classLoader.getResource(getClass().getName().replace('.', '/') + ".class");

        if (jarUrl != null && jarUrl.getProtocol().equals("jar")) {
            // Extract the JAR file path
            String jarPath = jarUrl.getPath().substring(5, jarUrl.getPath().indexOf("!"));

            // Open the JAR file
            try (JarFile jarFile = new JarFile(jarPath)) {
                // Iterate through the entries of the JAR file
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    String entryName = entry.getName();

                    // Check if the entry is a class
                    if (entryName.endsWith(".class")) {
                        // Convert the entry name to a fully qualified class name
                        String className = entryName.replace('/', '.').replace('\\', '.').replace(".class", "");
                        //System.out.println(className);
                        if (className.startsWith(packageName + ".") && !className.contains("$")) {
                            classes.add(className);
                        }
                    }
                }
            } catch (IOException e) {
                Keel.getLogger().exception(e);
            }
        }

        return classes;
    }
}
