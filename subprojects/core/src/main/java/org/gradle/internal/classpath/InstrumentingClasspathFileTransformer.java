/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.classpath;

import org.gradle.api.UncheckedIOException;
import org.gradle.cache.FileLock;
import org.gradle.cache.FileLockManager;
import org.gradle.internal.classanalysis.AsmConstants;
import org.gradle.internal.classpath.transforms.ClassTransform;
import org.gradle.internal.classpath.transforms.ClasspathElementTransformFactory;
import org.gradle.internal.classpath.types.GradleCoreInstrumentingTypeRegistry;
import org.gradle.internal.classpath.types.InstrumentingTypeRegistry;
import org.gradle.internal.file.FileType;
import org.gradle.internal.hash.HashCode;
import org.gradle.internal.hash.Hasher;
import org.gradle.internal.hash.Hashing;
import org.gradle.internal.snapshot.FileSystemLocationSnapshot;

import java.io.File;
import java.io.IOException;

import static java.lang.String.format;
import static org.gradle.cache.internal.filelock.DefaultLockOptions.mode;

public class InstrumentingClasspathFileTransformer implements ClasspathFileTransformer {
    private static final int CACHE_FORMAT = 6;

    private final FileLockManager fileLockManager;
    private final ClasspathFileHasher fileHasher;
    private final ClasspathElementTransformFactory classpathElementTransformFactory;
    private final ClassTransform transform;

    public InstrumentingClasspathFileTransformer(
        FileLockManager fileLockManager,
        ClasspathFileHasher classpathFileHasher,
        ClasspathElementTransformFactory classpathElementTransformFactory,
        ClassTransform transform,
        GradleCoreInstrumentingTypeRegistry gradleCoreInstrumentingTypeRegistry
    ) {
        this.fileLockManager = fileLockManager;

        this.fileHasher = createFileHasherWithConfig(
            configHashFor(classpathElementTransformFactory, transform, gradleCoreInstrumentingTypeRegistry),
            classpathFileHasher);
        this.classpathElementTransformFactory = classpathElementTransformFactory;
        this.transform = transform;
    }

    private static HashCode configHashFor(ClasspathElementTransformFactory classpathElementTransformFactory, ClassTransform transform, GradleCoreInstrumentingTypeRegistry gradleCoreInstrumentingTypeRegistry) {
        Hasher hasher = Hashing.defaultFunction().newHasher();
        hasher.putInt(CACHE_FORMAT);
        hasher.putInt(AsmConstants.MAX_SUPPORTED_JAVA_VERSION);
        gradleCoreInstrumentingTypeRegistry.getInstrumentedTypesHash().ifPresent(hasher::putHash);
        gradleCoreInstrumentingTypeRegistry.getUpgradedPropertiesHash().ifPresent(hasher::putHash);
        classpathElementTransformFactory.applyConfigurationTo(hasher);
        transform.applyConfigurationTo(hasher);
        return hasher.hash();
    }

    private static ClasspathFileHasher createFileHasherWithConfig(HashCode configHash, ClasspathFileHasher fileHasher) {
        return sourceSnapshot -> {
            Hasher hasher = Hashing.newHasher();
            hasher.putHash(configHash);
            hasher.putHash(fileHasher.hashOf(sourceSnapshot));
            return hasher.hash();
        };
    }

    @Override
    public File transform(File source, FileSystemLocationSnapshot sourceSnapshot, File cacheDir, InstrumentingTypeRegistry typeRegistry) {
        String destDirName = hashOf(sourceSnapshot);
        File destDir = new File(cacheDir, destDirName);
        String destFileName = sourceSnapshot.getType() == FileType.Directory ? source.getName() + ".jar" : source.getName();
        File receipt = new File(destDir, destFileName + ".receipt");
        File transformed = new File(destDir, destFileName);

        // Avoid file locking overhead by checking for the receipt first.
        if (receipt.isFile()) {
            return transformed;
        }

        final File lockFile = new File(destDir, destFileName + ".lock");
        final FileLock fileLock = exclusiveLockFor(lockFile);
        try {
            if (receipt.isFile()) {
                // Lock was acquired after a concurrent writer had already finished.
                return transformed;
            }
            transform(source, transformed, typeRegistry);
            try {
                receipt.createNewFile();
            } catch (IOException e) {
                throw new UncheckedIOException(
                    format("Failed to create receipt for instrumented classpath file '%s/%s'.", destDirName, destFileName),
                    e
                );
            }
            return transformed;
        } finally {
            fileLock.close();
        }
    }

    @Override
    public ClasspathFileHasher getFileHasher() {
        return fileHasher;
    }

    private FileLock exclusiveLockFor(File file) {
        return fileLockManager.lock(
            file,
            mode(FileLockManager.LockMode.Exclusive).useCrossVersionImplementation(),
            "instrumented jar cache"
        );
    }

    private String hashOf(FileSystemLocationSnapshot sourceSnapshot) {
        return fileHasher.hashOf(sourceSnapshot).toString();
    }

    private void transform(File source, File dest, InstrumentingTypeRegistry typeRegistry) {
        classpathElementTransformFactory.createTransformer(source, this.transform, typeRegistry).transform(dest);
    }
}
