/*
 * Copyright (c) Manuel Martins, All Rights Reserved.
 * (www.bet4trade.com)
 *
 * This software is the proprietary information of BetForTrade.
 * Use is subject to license terms.
 *
 */
package com.mcmartins.maven.plugins;

import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;

/**
 * <description>.
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
