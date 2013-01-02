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

import java.io.File;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.sonatype.plexus.components.cipher.DefaultPlexusCipher;
import org.sonatype.plexus.components.cipher.PlexusCipherException;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;
import org.sonatype.plexus.components.sec.dispatcher.SecUtil;
import org.sonatype.plexus.components.sec.dispatcher.model.SettingsSecurity;

/**
 * Decryption utility plugin for maven resources.
 *
 * Usage:
 *
 * <pre>
 *     {@code
 *     ...
 *    <plugin>
 *      <groupId>com.mcmartins.maven.hints</groupId>
 *      <artifactId>decode-password-plugin</artifactId>
 *      <version>0.0.1</version>
 *      <executions>
 *          <execution>
 *            <id>decode</id>
 *            <phase>initialize</phase>
 *            <goals>
 *              <goal>decode-password</goal>
 *            </goals>
 *          </execution>
 *      </executions>
 *    </plugin>
 *     ...
 *     }
 * </pre>
 *
 * @author Manuel Martins
 * @goal decode-password
 */
public class PasswordDecodeMojo extends AbstractMojo {

    private static final String PATH_FORMAT = "{0}/{1}/{2}";
    private static final String MAVEN_HOME = "env.M2_HOME";
    private static final String MAVEN_USER_HOME = "user.home";
    private static final String M2 = ".m2";
    private static final String SETTINGS_SECURITY_FILE = "settings-security.xml";
    private static final String PASSWORD_PLACEHOLDER = "\\{.*\\}";

    private static boolean processed = false;
    private static Properties decodedProperties = new Properties();

    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * @parameter default-value="${session}"
     * @required
     * @readonly
     */
    private MavenSession session;

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // if is the first time, load security settings file and get master password
        if (!processed) {
            try {
                logInfo("PasswordDecodeMavenResourcesFiltering - Starting to decode project properties...");
                // decode master password
                final SettingsSecurity settingsSecurity = this.getSettingSecurity();
                final String plainTextMasterPassword =
                        decode(settingsSecurity.getMaster(), DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION);
                final Properties projectProperties = this.project.getProperties();
                final Enumeration keys = projectProperties.keys();
                while (keys.hasMoreElements()) {
                    final String key = (String) keys.nextElement();
                    final String value = (String) projectProperties.get(key);
                    // if property matches the pattern {.*}, decode
                    if (value.matches(PASSWORD_PLACEHOLDER)) {
                        logInfo(MessageFormat
                                .format("PasswordDecodeMavenResourcesFiltering - Processing property with key: {0}", key));
                        decodedProperties.setProperty(key, decode(value, plainTextMasterPassword));
                    }
                }
                logInfo("PasswordDecodeMavenResourcesFiltering - Finished decoding project properties.");
                this.project.getProperties().putAll(decodedProperties);
                processed = true;
            } catch (final PlexusCipherException e) {
                logError("PasswordDecodeMavenResourcesFiltering - Error decoding password.", e);
                System.exit(1);
            } catch (final SecDispatcherException e) {
                logError(MessageFormat.format("PasswordDecodeMavenResourcesFiltering - Error loading file: {0}.",
                        SETTINGS_SECURITY_FILE), e);
                System.exit(1);
            }
        } else {
            logInfo("PasswordDecodeMavenResourcesFiltering - Merging properties...");
            this.project.getProperties().putAll(decodedProperties);
        }
    }

    private SettingsSecurity getSettingSecurity() throws PlexusCipherException, SecDispatcherException {
        File securitySettings;
        // get execution properties
        final Properties executionProperties = this.session.getExecutionProperties();
        // try to load security-settings.xml from the user home path
        String path = MessageFormat.format(PATH_FORMAT, executionProperties.getProperty(MAVEN_USER_HOME), M2,
                SETTINGS_SECURITY_FILE);
        logInfo(MessageFormat.format("PasswordDecodeMavenResourcesFiltering - Loading {0} file from: {1}...",
                SETTINGS_SECURITY_FILE, path));
        securitySettings = new File(path);
        // if the file does not exists in this path
        if (!securitySettings.exists()) {
            // try to load security-settings.xml from the maven home path
            path = MessageFormat.format(PATH_FORMAT,
                    executionProperties.getProperty(MAVEN_HOME), M2, SETTINGS_SECURITY_FILE);
            securitySettings = new File(path);
            logInfo(MessageFormat.format("PasswordDecodeMavenResourcesFiltering - Loading {0} file from: {1}...",
                    SETTINGS_SECURITY_FILE, path));
        }
        // if file cannot be found we cannot proceed
        if (!securitySettings.exists()) {
            logInfo(MessageFormat.format("PasswordDecodeMavenResourcesFiltering - Failed to load file: {0}",
                    SETTINGS_SECURITY_FILE));
            System.exit(1);
        }
        // load SettingsSecurity Object
        return SecUtil.read(securitySettings.getAbsolutePath(), true);
    }

    /**
     * Decode a password using the {@link org.sonatype.plexus.components.cipher.DefaultPlexusCipher}.
     *
     * @param encoded the encoded password.
     * @param key the key used to encode the password.
     * @return the password in plain text.
     * @throws PlexusCipherException if any exception occurs.
     */
    private static String decode(final String encoded, final String key) throws PlexusCipherException {
        return new DefaultPlexusCipher().decryptDecorated(encoded, key);
    }

    /**
     * Log info.
     *
     * @param message the message to log.
     */
    private void logInfo(final String message) {
        if (this.session.getContainer().getLogger().isInfoEnabled()) {
            this.session.getContainer().getLogger().info(message);
        }
    }

    /**
     * Log error.
     *
     * @param message the message.
     * @param e the exception.
     */
    private void logError(final String message, final Exception e) {
        if (this.session.getContainer().getLogger().isErrorEnabled()) {
            this.session.getContainer().getLogger().error(message, e);
        }
    }
}
