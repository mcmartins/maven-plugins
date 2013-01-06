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
 * Decode Password utility plugin allows to insert encoded information within the project without having it hardcoded
 * (e.g. single profiles.xml containing all the profiles - Production, Development, Developer1, Testing). The encoded
 * information will only be processed if the right master password is provided (see <a
 * href="http://maven.apache.org/guides/mini/guide-encryption.html">maven encryption guide</a>).
 * <p/>
 * Basically all information defined within the project as properties (e.g. within the profiles.xml, properties tag,
 * etc) surrounded by curly brackets <b>{ }</b>  will be decoded using the Settings Security master password, <b>if
 * valid only</b>.
 * <p/>
 * Settings Security file is loaded by the following order: <ul> <li>1st System Property (passed as argument for the
 * MAVEN_OPTS e.g. -Dsettings.security=/private/settings-security.xml);</li> <li>2nd Plugin Configuration
 * securitySettingsPath (User defined on the plugin configuration);</li> <li>3rd User Home .m2 directory;</li> <li>4th
 * Maven Home .m2 directory;</li> </ul>
 * <p/>
 * The first that occurs is the one used. If none is found the plugin will throw a {@link RuntimeException}. The name of
 * the file <b>shall</b>  be <b>settings-security.xml</b> unless you specify it.
 * <p/>
 * The plugin works using the Maven Password Encryption/Decryption system. In order to use this plugin ensure you have
 * generated a master password and encoded your project passwords using it. Please refer to the Maven documentation at:
 * <a href="http://maven.apache.org/guides/mini/guide-encryption.html">encryption guide</a>.
 * <p/>
 * <p/>
 * Usage:
 * <pre>
 * {@code
 * ...
 *  <build>
 *      ...
 *     <plugins>
 *         ...
 *         <plugin>
 *              <groupId>com.mcmartins.maven</groupId>
 *              <artifactId>decode-password-plugin</artifactId>
 *              <version>0.0.1</version>
 *              <executions>
 *                  <execution>
 *                    <id>decode-passwords</id>
 *                    <phase>initialize</phase>
 *                    <goals>
 *                      <goal>process</goal>
 *                    </goals>
 *                  </execution>
 *              </executions>
 *              <configuration>
 *                  <!-- Optional configurations -->
 *                  <!-- Absolute Path to the settings-security file. Could contain the file name. If this
 * configuration is used with a file name, the securitySettingsFileName configuration will be ignored. -->
 *                  <securitySettingsPath>/path/to/security-settings.xml</securitySettingsPath>
 *                  <!-- The settings-security file name. If none is provided the default is used -
 * settings-security.xml -->
 *                  <securitySettingsFileName>my-security-settings.xml</securitySettingsFileName>
 *              </configuration>
 *          </plugin>
 *          ...
 *     </plugins>
 *     ...
 *  </build>
 * ...
 *  <pluginRepositories>
 *      ...
 *      <pluginRepository>
 *          <id>mcmartins-maven-repository</id>
 *          <url>http://mcmartins-maven-repository.googlecode.com/svn/maven/plugins/releases</url>
 *          <snapshots>
 *              <enabled>false</enabled>
 *          </snapshots>
 *      </pluginRepository>
 *      ...
 *  </pluginRepositories>
 * ...
 * }
 *
 *
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
        // if is the first time, load security settings file and process the project properties
        if (!alreadyProcessed) {
            try {
                logInfo("Starting to process project properties...");
                final SettingsSecurity settingsSecurity = this.getSettingsSecurity();
                final String plainTextMasterPassword = CipherUtils.decode(settingsSecurity.getMaster(),
                        DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION);
                final Properties projectProperties = this.getProject().getProperties();
                final Enumeration keys = projectProperties.keys();
                while (keys.hasMoreElements()) {
                    final String key = (String) keys.nextElement();
                    final String value = (String) projectProperties.get(key);
                    // if property matches the pattern {.*}, try to decode it
                    if (value.matches(PASSWORD_PLACEHOLDER)) {
                        logInfo(MessageFormat.format("Processing property with key [{0}]", key));
                        try {
                            decodedProperties.setProperty(key, CipherUtils.decode(value, plainTextMasterPassword));
                        } catch (final PlexusCipherException e) {
                            // warn about errors when decrypting passwords, probably the master password is not the key for this one - skip
                            logWarn("Error decoding password. It seams you cannot decrypt this one with your master password, skipping...");
                        }
                    }
                }
                logInfo("Finished processing project properties.");
                alreadyProcessed = true;
            } catch (final PlexusCipherException e) {
                logError("Error decoding master password.", e);
                throw new RuntimeException(MessageFormat.format("Failed to decode Master Password from {0}",
                        SETTINGS_SECURITY_FILE), e);
            } catch (final SecDispatcherException e) {
                logError(MessageFormat.format("Error loading file {0}", SETTINGS_SECURITY_FILE), e);
                throw new RuntimeException(MessageFormat.format("Failed to load file {0}", SETTINGS_SECURITY_FILE), e);
            }
        }
        logInfo("Merging properties...");
        this.getProject().getProperties().putAll(decodedProperties);
    }
}
