/*
 *  Copyright 2013 Manuel Martins.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package com.mcmartins.maven.plugins;

import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

/**
 * Contains utilities methods to decrypt passwords using maven master password system.
 *
 * @author Manuel Martins
 */
public final class CipherUtils {

    private CipherUtils() {
    }

    /**
     * Decode a password using the {@link org.sonatype.plexus.components.cipher.DefaultPlexusCipher}.
     *
     * @param encoded the encoded password
     * @param key the key used to encode the password
     * @return the password in plain text
     * @throws org.sonatype.plexus.components.cipher.PlexusCipherException if any exception occurs
     */
    public static String decode(final String encoded, final String key) throws PlexusCipherException {
        return new DefaultPlexusCipher().decryptDecorated(encoded, key);
    }
}
