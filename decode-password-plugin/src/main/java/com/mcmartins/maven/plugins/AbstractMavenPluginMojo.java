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
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.plexus.components.sec.dispatcher.DefaultSecDispatcher;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;
import org.sonatype.plexus.components.sec.dispatcher.SecUtil;
import org.sonatype.plexus.components.sec.dispatcher.model.SettingsSecurity;

/**
 * Abstract Mojo. Contains additional methods to deal with Maven Session, Maven Project and Maven Settings Security.
 * Will be part of a more abstract module in the future in order to be reused by other plugins.
 *
 * @author Manuel Martins
 */
public abstract class AbstractMavenPluginMojo extends AbstractMojo {

    protected static final String SETTINGS_SECURITY_FILE = "settings-security.xml";
    protected static final String MAVEN_HOME = "env.M2_HOME";
    protected static final String USER_HOME = "user.home";
    protected static final String M2 = ".m2";

    /**
     * Absolute Path to the settings-security file. Could contain the file name. If this configuration is used with a
     * file name, the securitySettingsFileName configuration will be ignored.
     */
    @Parameter
    private String securitySettingsPath;

    /**
     * The settings-security file name. If none is provided the default is used - settings-security.xml
     */
    @Parameter
    private String securitySettingsFileName;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    private MavenSession session;

    /**
     * Log info.
     *
     * @param message the message to log.
     */
    protected void logInfo(final String message) {
        if (this.getLog().isInfoEnabled()) {
            this.getLog().info(message);
        }
    }

    /**
     * Log error.
     *
     * @param message the message.
     * @param e the exception.
     */
    protected void logError(final String message, final Exception e) {
        if (this.getLog().isErrorEnabled()) {
            this.getLog().error(message, e);
        }
    }

    /**
     * Log warning.
     *
     * @param message the message.
     */
    protected void logWarn(final String message) {
        if (this.getLog().isWarnEnabled()) {
            this.getLog().warn(message);
        }
    }

    /**
     * Returns the {@link MavenSession} object
     *
     * @return mavenSession
     */
    protected MavenSession getSession() {
        return session;
    }

    /**
     * Returns the {@link MavenProject} object
     *
     * @return mavenProject
     */
    protected MavenProject getProject() {
        return project;
    }

    /**
     * Returns the {@link SettingsSecurity} object.
     *
     * @return settingsSecurity
     * @throws SecDispatcherException
     */
    protected SettingsSecurity getSettingsSecurity() throws SecDispatcherException {
        // load execution properties to lookup for settings-security paths
        final Properties executionProperties = this.getSession().getExecutionProperties();
        // try to load security-settings.xml from the system properties
        File securitySettings = this.getSettingsSecurityFile(executionProperties
                .getProperty(DefaultSecDispatcher.SYSTEM_PROPERTY_SEC_LOCATION));
        if (securitySettings == null) {
            // try to load file from path specified by user, if specified
            securitySettings = this.getSettingsSecurityFile(this.securitySettingsPath);
            if (securitySettings == null) {
                // try to load security-settings.xml from the user home path / .m2
                securitySettings = this.getSettingsSecurityFile(MessageFormat.format("{0}{1}{2}",
                        System.getProperty(USER_HOME), File.separator, M2));
                if (securitySettings == null) {
                    //try to load security-settings.xml from the maven home path / .m2
                    securitySettings = this.getSettingsSecurityFile(MessageFormat.format("{0}{1}{2}",
                            executionProperties.getProperty(MAVEN_HOME), File.separator, M2));
                }
            }
        }
        // if file cannot be found we cannot proceed
        if (securitySettings == null) {
            throw new RuntimeException(MessageFormat.format("Failed to load file {0}",
                    this.getSettingsSecurityFileName()));
        }
        // load SettingsSecurity Object
        return SecUtil.read(securitySettings.getAbsolutePath(), true);
    }

    /**
     * Returns the {@link File} for the specified path if exists, {@code null} otherwise.
     *
     * @param path the path for the file
     * @return file
     */
    private File getSettingsSecurityFile(final String path) {
        File file = null;
        if (StringUtils.isNotBlank(path)) {
            // if path contains the file name load the file, otherwise try to load the file by it default name
            if (path.endsWith(".xml")) {
                logInfo(MessageFormat.format("Loading {0}", path));
                file = new File(path);
            } else {
                logInfo(MessageFormat.format("Loading {0}{1}{2}", path,
                        File.separator, this.getSettingsSecurityFileName()));
                final File folder = new File(path);
                file = new File(folder, this.getSettingsSecurityFileName());
            }
            if (!file.exists()) {
                file = null;
            }
        }
        return file;
    }

    /**
     * Returns the settings security file name.
     *
     * @return settingsSecurity file name
     */
    protected String getSettingsSecurityFileName() {
        return StringUtils.isNotBlank(this.securitySettingsFileName) ? this.securitySettingsFileName
                : StringUtils.isNotBlank(this.securitySettingsPath) && this.securitySettingsPath.endsWith(".xml")
                ? getFileNameFromPath(this.securitySettingsPath) : SETTINGS_SECURITY_FILE;
    }

    /**
     * Tries to find the file name in a given path.
     *
     * @param path the path to search
     * @return fileName
     */
    private String getFileNameFromPath(final String path) {
        String fileName = null;
        if (StringUtils.isNotBlank(path)) {
            String[] paths = path.split(Pattern.quote("/"));
            if (paths.length == 1) {
                paths = path.split(Pattern.quote("\\"));
                if (paths.length > 1) {
                    fileName = paths[paths.length - 1];
                }
            } else {
                fileName = paths[paths.length - 1];
            }
        }
        return fileName;
    }
}
