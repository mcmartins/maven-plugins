/*
 * Copyright (c) Manuel Martins, All Rights Reserved.
 * (www.bet4trade.com)
 *
 * This software is the proprietary information of BetForTrade.
 * Use is subject to license terms.
 *
 */
package com.mcmartins.maven.plugins;

import java.io.File;
import java.text.MessageFormat;
import java.util.Properties;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.StringUtils;
import org.sonatype.plexus.components.sec.dispatcher.SecDispatcherException;
import org.sonatype.plexus.components.sec.dispatcher.SecUtil;
import org.sonatype.plexus.components.sec.dispatcher.model.SettingsSecurity;

/**
 * <description>.
 *
 * @author Manuel Martins
 */
public abstract class AbstractMavenPluginMojo extends AbstractMojo {

    protected static final String PATH_FORMAT = "{0}/{1}/{2}";
    protected static final String MAVEN_HOME = "env.M2_HOME";
    protected static final String MAVEN_USER_HOME = "user.home";
    protected static final String M2 = ".m2";
    protected static final String SETTINGS_SECURITY_FILE = "settings-security.xml";

    @Parameter
    private String securitySettingsPath;

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
    protected SettingsSecurity getSettingSecurity() throws SecDispatcherException {
        // try to load file from path specified by user, if specified
        File securitySettings = this.getFile(StringUtils.isNotBlank(this.securitySettingsPath)
                ? MessageFormat.format("{0}/{1}", this.securitySettingsPath, SETTINGS_SECURITY_FILE) : null);
        // if path specified by user is invalid or not provided, try to load from default locations: user.home/.m2/ | M2_HOME/.m2/
        if (securitySettings == null) {
            // get execution properties
            final Properties executionProperties = this.getSession().getExecutionProperties();
            // try to load security-settings.xml from the user home path
            this.securitySettingsPath =
                    MessageFormat.format(PATH_FORMAT, executionProperties.getProperty(MAVEN_USER_HOME),
                            M2, SETTINGS_SECURITY_FILE);
            securitySettings = this.getFile(this.securitySettingsPath);
            // if the file does not exists in this path, try to load security-settings.xml from the maven home path
            if (securitySettings == null) {
                this.securitySettingsPath =
                        MessageFormat.format(PATH_FORMAT, executionProperties.getProperty(MAVEN_HOME),
                                M2, SETTINGS_SECURITY_FILE);
                securitySettings = this.getFile(this.securitySettingsPath);
            }
        }
        // if file cannot be found we cannot proceed
        if (securitySettings == null) {
            throw new RuntimeException(MessageFormat.format("Failed to load file: {0}", SETTINGS_SECURITY_FILE));
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
    private File getFile(final String path) {
        File file = null;
        if (StringUtils.isNotBlank(path)) {
            logInfo(MessageFormat.format("Loading {0} file from: {1}...", SETTINGS_SECURITY_FILE, path));
            file = new File(path);
        }
        return file;
    }
}
