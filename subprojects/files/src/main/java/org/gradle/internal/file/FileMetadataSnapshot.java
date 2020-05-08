/*
 * Copyright 2017 the original author or authors.
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

package org.gradle.internal.file;

/**
 * An immutable snapshot of the metadata of a file.
 */
public interface FileMetadataSnapshot {
    FileType getType();

    /**
     * Note: always 0 for directories and missing files.
     */
    long getLastModified();

    /**
     * Note: always 0 for directories and missing files.
     */
    long getLength();

    /**
     * How the file with the metadata was accessed.
     */
    AccessType getAccessType();

    /**
     * How the file with the metadata was accessed.
     *
     * If we have a symlink situation like `symlink1` -> `symlink2` -> `target`,
     * then the metadata snapshot for `symlink1` will have an access type {@link AccessType#VIA_SYMLINK}
     * and the metadata of `target`, i.e. the accessor will resolve transitive symlinks.
     *
     * If the directory `symlinkedDir` -> `targetDir` is a symlink, then the
     * metadata snapshot of `symlinkedDir/fileInDir` will be {@link AccessType#DIRECT},
     * given the file `targetDir/fileInDir` exists. That means that {@link AccessType}
     * only gives information about the queried path, not about parents of the queried path.
     */
    enum AccessType {
        /**
         * The path pointed to the file directly.
         */
        DIRECT,

        /**
         * The path pointed to a symlink pointing to the file with the metadata.
         */
        VIA_SYMLINK;

        /**
         * Factory method for returning the access type based on a boolean.
         */
        public static AccessType viaSymlink(boolean viaSymlink) {
            return viaSymlink
                ? VIA_SYMLINK
                : DIRECT;
        }
    }
}
