/*
 * File: AbstractRemoteApplicationBuilder.java
 *
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * The contents of this file are subject to the terms and conditions of 
 * the Common Development and Distribution License 1.0 (the "License").
 *
 * You may not use this file except in compliance with the License.
 *
 * You can obtain a copy of the License by consulting the LICENSE.txt file
 * distributed with this file, or by consulting https://oss.oracle.com/licenses/CDDL
 *
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file LICENSE.txt.
 *
 * MODIFICATIONS:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 */

package com.oracle.tools.runtime.remote;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

import com.oracle.tools.runtime.AbstractApplicationBuilder;
import com.oracle.tools.runtime.Application;
import com.oracle.tools.runtime.ApplicationConsole;
import com.oracle.tools.runtime.ApplicationSchema;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Properties;

import java.util.concurrent.TimeUnit;

/**
 * An {@link AbstractRemoteApplicationBuilder} is a base implementation of an {@link RemoteApplicationBuilder}
 * (over SSH).
 *
 * @param <A>  the type of the {@link Application}s the {@link RemoteApplicationBuilder} will realize
 * @param <S>  the type of the {@link ApplicationSchema} for the {@link Application}s
 * @param <E>  the type of the {@link RemoteApplicationEnvironment} used by the {@link RemoteApplicationBuilder}
 * @param <B>  the type of the {@link RemoteApplicationBuilder}
 * <p>
 * Copyright (c) 2014. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Brian Oliver
 */
public abstract class AbstractRemoteApplicationBuilder<A extends Application<A>, S extends ApplicationSchema<A, S>,
                                                       E extends RemoteApplicationEnvironment,
                                                       B extends AbstractRemoteApplicationBuilder<A, S, E, B>>
    extends AbstractApplicationBuilder<A, S> implements RemoteApplicationBuilder<A, S>
{
    /**
     * The {@link JSch} framework.
     */
    protected JSch jsch;

    /**
     * The name of the remote host for the SSH-based session.
     */
    protected String hostName;

    /**
     * The port of the remote host to connect for the SSH-based session.
     */
    protected int port;

    /**
     * The {@link Authentication} to use for the SSH-based session.
     */
    protected Authentication authentication;

    /**
     * The user name to use for the SSH-based session.
     */
    protected String userName;

    /**
     * The {@link File#separator} for the remote SSH-based session.
     * (by default we assume the remote server is the same as this server)
     */
    protected char remoteFileSeparatorChar;

    /**
     * The {@link File#pathSeparator} for the remote SSH-based session.
     * (by default we assume the remote server is the same as this server)
     */
    protected char remotePathSeparatorChar;

    /**
     * Is strict host name checking enforced?
     * <p>
     * WARNING: By setting to false this may lower system security.
     */
    protected boolean strictHostChecking;

    /**
     * The {@link DeploymentArtifact}s that the builder must deploy
     * prior to the application being launched.
     */
    protected ArrayList<DeploymentArtifact> deploymentArtifacts;

    /**
     * The {@link File} representing the location of the temporary directory
     * on the remote server.
     */
    protected File remoteTemporaryDirectoryFile;


    /**
     * Constructs an {@link AbstractRemoteApplicationBuilder} (using the default port).
     *
     * @param hostName        the remote host name
     * @param userName        the user name on the remote host
     * @param authentication  the {@link Authentication} for connecting to the host
     */
    public AbstractRemoteApplicationBuilder(String         hostName,
                                            String         userName,
                                            Authentication authentication)
    {
        this(hostName, DEFAULT_PORT, userName, authentication);
    }


    /**
     * Constructs an {@link AbstractRemoteApplicationBuilder}.
     *
     * @param hostName        the remote host name
     * @param port            the remote port
     * @param userName        the user name on the remote host
     * @param authentication  the {@link Authentication} for connecting to the host
     */
    public AbstractRemoteApplicationBuilder(String         hostName,
                                            int            port,
                                            String         userName,
                                            Authentication authentication)
    {
        super();

        this.hostName       = hostName;
        this.port           = port;
        this.userName       = userName;
        this.authentication = authentication;

        // establish the default remote server properties
        this.remoteTemporaryDirectoryFile = new File(File.separatorChar + "tmp");
        this.remoteFileSeparatorChar      = File.separatorChar;
        this.remotePathSeparatorChar      = File.pathSeparatorChar;
        this.strictHostChecking           = true;

        // by default the builder doesn't deploy any artifacts
        deploymentArtifacts = new ArrayList<DeploymentArtifact>();

        // establish the JSch framework for the builder
        this.jsch = new JSch();

        // allow the authentication to configure the framework
        if (authentication instanceof JSchBasedAuthentication)
        {
            ((JSchBasedAuthentication) authentication).configureFramework(jsch);
        }
    }


    /**
     * Sets the file.separator character to be used with the remote SSH-session.
     * This defaults to the file.separator of the current platform.
     *
     * @param fileSeparator  the file.separator
     *
     * @return this {@link RemoteApplicationBuilder} to permit fluent method calls
     */
    public B setRemoteFileSeparator(char fileSeparator)
    {
        this.remoteFileSeparatorChar = fileSeparator;

        return (B) this;
    }


    /**
     * Sets the location of the remote temporary directory.
     * By default this is /tmp.
     *
     * @param directory  the temporary directory
     *
     * @return this {@link RemoteApplicationBuilder} to permit fluent method calls
     */
    public B setRemoteTemporaryDirectory(File directory)
    {
        this.remoteTemporaryDirectoryFile = directory;

        return (B) this;
    }


    /**
     * Sets whether strict host file checking is required (true by default).
     * By setting to false security will be lowered.
     *
     * @param strictHostChecking  true to use strict host checking
     *
     * @return this {@link RemoteApplicationBuilder} to permit fluent method calls
     */
    public B setStrictHostChecking(boolean strictHostChecking)
    {
        this.strictHostChecking = strictHostChecking;

        return (B) this;
    }


    /**
     * Adds a {@link DeploymentArtifact} that the builder must deploy for each
     * application realized, regardless of the application schema.
     *
     * @param deploymentArtifact  the {@link DeploymentArtifact}
     */
    public B addDeploymentArtifact(DeploymentArtifact deploymentArtifact)
    {
        if (deploymentArtifact != null)
        {
            deploymentArtifacts.add(deploymentArtifact);
        }

        return (B) this;
    }


    /**
     * Creates a remote-platform specific filename, given a fileName
     * represented in a format for this platform.
     *
     * @param fileName  the file name to convert
     *
     * @return the file as it would be represented by the remote platform
     */
    protected String asRemotePlatformFileName(String fileName)
    {
        return fileName == null ? null : fileName.replace(File.separatorChar, remoteFileSeparatorChar);
    }


    /**
     * Creates a sanitized and lower-case version of a file name by replacing
     * consecutive non-numerical, non-alphabetical, non ".", non "-" and non "~"
     * characters of a string with "-".
     *
     * @param fileName  the un-sanitized file name
     *
     * @return  the sanitized file name
     */
    protected String asSanitizedFileName(String fileName)
    {
        if (fileName == null)
        {
            return null;
        }
        else
        {
            StringBuilder builder = new StringBuilder(fileName.length());
            String valid = "1234567890abcdefghijklmnopqrstuvwxyz.~" + File.separatorChar + remoteFileSeparatorChar;
            char          last    = '\0';

            fileName = fileName.toLowerCase();

            for (char c : fileName.toCharArray())
            {
                if (valid.indexOf(c) < 0)
                {
                    last = '-';
                }
                else
                {
                    if (last == '-' && builder.length() > 0)
                    {
                        builder.append(last);
                    }

                    last = c;
                    builder.append(c);
                }
            }

            return builder.toString().toLowerCase();
        }
    }


    /**
     * Obtains the {@link RemoteApplicationBuilder} specific {@link RemoteApplicationEnvironment}
     * to be used for configuring and realizing a remote {@link Application}.
     *
     * @param schema  the {@link ApplicationSchema} defining the application
     *
     * @return the {@link RemoteApplicationEnvironment}
     */
    abstract protected E getRemoteApplicationEnvironment(S schema) throws IOException;


    /**
     * Creates the {@link Application} representing the underlying
     * {@link RemoteApplicationProcess}.
     *
     * @param schema                the {@link ApplicationSchema} used to define the application
     * @param environment           the {@link RemoteApplicationEnvironment} for the application
     * @param applicationName       the name of the application
     * @param process               the {@link RemoteApplicationProcess}
     * @param console               the {@link ApplicationConsole} to use to capture the
     *                              {@link RemoteApplicationProcess} input and output
     *
     * @return the {@link Application}
     */
    protected abstract A createApplication(S                        schema,
                                           E                        environment,
                                           String                   applicationName,
                                           RemoteApplicationProcess process,
                                           ApplicationConsole       console);


    @Override
    public A realize(S                  schema,
                     String             applicationName,
                     ApplicationConsole console) throws IOException
    {
        Session session = null;

        // obtain the builder-specific remote application environment based on the schema
        E environment = getRemoteApplicationEnvironment(schema);

        try
        {
            // create the remote session
            session = jsch.getSession(userName, hostName, port);

            // the session should not cause the JVM not to exit
            session.setDaemonThread(true);

            // determine the timeout (based on the schema)
            int timeout = (int) schema.getDefaultTimeoutUnits().convert(schema.getDefaultTimeout(),
                                                                        TimeUnit.MILLISECONDS);

            // set the default session timeouts (in milliseconds)
            session.setTimeout(timeout);

            // allow the authentication to configure the session
            if (authentication instanceof JSchBasedAuthentication)
            {
                ((JSchBasedAuthentication) authentication).configureSession(session);
            }

            // configure the session channel properties
            Properties config = new Properties();

            config.put("StrictHostKeyChecking", strictHostChecking ? "yes" : "no");
            session.setConfig(config);

            // connect the session
            session.connect();

            // -------- deploy remote application artifacts (using sftp) --------

            // assume the remote directory is the working directory
            File remoteDirectoryFile = schema.getWorkingDirectory();
            String remoteDirectory = remoteDirectoryFile == null
                                     ? null : asRemotePlatformFileName(remoteDirectoryFile.toString());

            // create a list of DeploymentArtifacts to deploy based on those specified by the builder
            ArrayList<DeploymentArtifact> artifactsToDeploy = new ArrayList<DeploymentArtifact>(deploymentArtifacts);

            // add any custom artifacts for the schema
            for (DeploymentArtifact deploymentArtifact : environment.getRemoteDeploymentArtifacts())
            {
                artifactsToDeploy.add(deploymentArtifact);
            }

            if (artifactsToDeploy.size() > 0)
            {
                ChannelSftp sftpChannel = null;

                try
                {
                    // open an sftp channel that we can use to copy over the artifacts
                    sftpChannel = (ChannelSftp) session.openChannel("sftp");
                    sftpChannel.connect(timeout);

                    // create a temporary working folder (if there's no working folder set)
                    if (remoteDirectoryFile == null)
                    {
                        // create deployment directory as the working directory
                        // (as applicationName-YYYYmmdd-HHMMSS-LLL)

                        String   sanitizedApplicationName = asSanitizedFileName(applicationName);
                        Calendar now                      = Calendar.getInstance();
                        String temporaryDirectoryName = String.format("%1$s-%2$tY%2$tm%2$td-%2$tH%2$tM%2$tS-%2$tL",
                                                                      sanitizedApplicationName,
                                                                      now);

                        remoteDirectoryFile = new File(remoteTemporaryDirectoryFile, temporaryDirectoryName);
                        remoteDirectory     = asRemotePlatformFileName(remoteDirectoryFile.toString());

                        // create the remote directory
                        sftpChannel.mkdir(remoteDirectory);
                    }

                    // copy deployment artifacts into the remote server
                    for (DeploymentArtifact artifactToDeploy : artifactsToDeploy)
                    {
                        // acquire the source file to deploy
                        File sourceFile = artifactToDeploy.getSourceFile();

                        // change to the desired remote directory
                        File   destinationFile = artifactToDeploy.getDestinationFile();

                        String destinationFileName;

                        if (destinationFile == null)
                        {
                            sftpChannel.cd(remoteDirectory);
                            destinationFileName = sourceFile.getName();
                        }
                        else
                        {
                            String destinationFilePath = asRemotePlatformFileName(destinationFile.getParent());

                            if (destinationFilePath == null)
                            {
                                sftpChannel.cd(remoteDirectory);
                            }
                            else
                            {
                                sftpChannel.cd(asRemotePlatformFileName(destinationFile.getPath()));
                            }

                            destinationFileName = destinationFile.getName();
                        }

                        // copy the source artifact to the destination file
                        sftpChannel.put(new FileInputStream(sourceFile), destinationFileName);
                    }
                }
                catch (SftpException e)
                {
                    throw new IOException("Failed to deploy application", e);
                }
                finally
                {
                    if (sftpChannel != null)
                    {
                        sftpChannel.disconnect();
                    }
                }
            }

            // -------- execute remote application (using ssh) --------

            // open the channel (for a remote execution)
            ChannelExec execChannel = (ChannelExec) session.openChannel("exec");

            // set the remote environment variables
            Properties environmentVariables = environment.getRemoteEnvironmentVariables();

            for (String variableName : environmentVariables.stringPropertyNames())
            {
                execChannel.setEnv(variableName, environmentVariables.getProperty(variableName));
            }

            // determine the command to execute remotely
            String command = environment.getRemoteCommandToExecute();

            // the actual remote command must include changing to the remote directory
            String remoteCommand = String.format("cd %s ; %s", remoteDirectory, command);

            execChannel.setCommand(remoteCommand);

            // establish a RemoteApplicationProcess representing the remote application
            RemoteApplicationProcess process = new RemoteApplicationProcess(session, execChannel);

            // connect the channel
            execChannel.connect(timeout);

            // create the Application based on the RemoteApplicationProcess
            A application = createApplication(schema, environment, applicationName, process, console);

            // -------- raise application interceptor events --------

            raiseApplicationLifecycleEvent(application, Application.EventKind.REALIZED);

            return application;
        }
        catch (JSchException e)
        {
            if (session != null)
            {
                session.disconnect();
            }

            environment.close();

            throw new IOException("Failed to create remote application", e);
        }
    }
}