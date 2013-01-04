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

import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.sonatype.plexus.components.cipher.PlexusCipherException;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;
import org.sonatype.plexus.components.sec.dispatcher.model.SettingsSecurity;

/**
 * Decryption utility plugin for maven resources.
 * <p/>
 * Usage:
 * <p/>
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
 */
@Mojo(name = "process", defaultPhase = LifecyclePhase.INITIALIZE)
public class DecodePasswordMavenPluginMojo extends AbstractMavenPluginMojo {

    private static final String PASSWORD_PLACEHOLDER = "\\{.*\\}";

    private static boolean alreadyProcessed = false;
    private static Properties decodedProperties = new Properties();

    /**
     * {@inheritDoc}
     */
    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        // if is the first time, load security settings file and get master password
        if (!alreadyProcessed) {
            try {
                logInfo("Starting to process project properties...");
                // decode master password
                final SettingsSecurity settingsSecurity = this.getSettingSecurity();
                final String plainTextMasterPassword = CipherUtils.decode(settingsSecurity.getMaster(),
                        DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION);
                final Properties projectProperties = this.getProject().getProperties();
                final Enumeration keys = projectProperties.keys();
                while (keys.hasMoreElements()) {
                    final String key = (String) keys.nextElement();
                    final String value = (String) projectProperties.get(key);
                    // if property matches the pattern {.*}, decode
                    if (value.matches(PASSWORD_PLACEHOLDER)) {
                        logInfo(MessageFormat.format("Processing property with key: {0}", key));
                        // warn about errors decrypting passwords, probably the master password is not the key for this one
                        try {
                            decodedProperties.setProperty(key, CipherUtils.decode(value, plainTextMasterPassword));
                        } catch (final PlexusCipherException e) {
                            logWarn("Error decoding password. It seams you cannot decrypt this one...");
                        }
                    }
                }
                logInfo("Finished process project properties.");
                logInfo("Merging properties...");
                this.getProject().getProperties().putAll(decodedProperties);
                alreadyProcessed = true;
            } catch (final PlexusCipherException e) {
                logError("Error decoding master password.", e);
            } catch (final SecDispatcherException e) {
                logError(MessageFormat.format("Error loading file: {0}.",
                        SETTINGS_SECURITY_FILE), e);
                throw new RuntimeException(MessageFormat.format("Failed to load file: {0}", SETTINGS_SECURITY_FILE), e);
            }
        } else {
            logInfo("Merging properties...");
            this.getProject().getProperties().putAll(decodedProperties);
        }
    }
}
