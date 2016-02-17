/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stratuscom.harvester.deployer;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileType;
import com.stratuscom.harvester.ConfigurationException;
import com.stratuscom.harvester.Context;
import com.stratuscom.harvester.FileUtility;
import com.stratuscom.harvester.Init;
import com.stratuscom.harvester.Injected;
import com.stratuscom.harvester.InjectionStyle;
import com.stratuscom.harvester.MBeanRegistrar;
import com.stratuscom.harvester.MessageNames;
import com.stratuscom.harvester.Name;
import com.stratuscom.harvester.Utils;
import java.io.File;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.vfs2.FileSystemException;

/**
 *
 * A runner task that looks at the command line to determine the name of an
 * application to run from a deployment folder. Generally used to run "client"
 * apps where the name of the app is supplied on the command line, for instance
 * the browser app or the admin client app.
 */
public class CommandLineAppRunner {

    private static final Logger log
            = Logger.getLogger(CommandLineAppRunner.class.getName(), MessageNames.BUNDLE_NAME);

    @Injected
    ResourceBundle messages;

    @Injected
    public String[] commandLineArguments = null;

    @Injected(style = InjectionStyle.BY_TYPE)
    Context context = null;

    private String deployDirectory = com.stratuscom.harvester.Strings.DEFAULT_DEPLOY_DIRECTORY;

    @Injected(style = InjectionStyle.BY_TYPE)
    private FileUtility fileUtility = null;

    @Injected(style = InjectionStyle.BY_TYPE)
    private StarterServiceDeployer deployer;

    @Injected(style = InjectionStyle.BY_TYPE)
    private MBeanRegistrar mbeanRegistrar;

    @Name
    private String myName = null;

    private List<ApplicationEnvironment> applicationEnvironments
            = new ArrayList<ApplicationEnvironment>();

    public String getDeployDirectory() {
        return deployDirectory;
    }

    public void setDeployDirectory(String deployDirectory) {
        this.deployDirectory = deployDirectory;
    }

    FileObject deploymentDirectoryFile = null;

    private String clientAppName = null;

    public String getClientAppName() {
        return clientAppName;
    }

    /**
     * Set the client app that should be loaded. If not provided, client app
     * name is taken from the first parameter.
     *
     * @param clientApp
     */
    public void setClientAppName(String clientApp) {
        this.clientAppName = clientApp;
    }

    @Init
    public void init() {
        try {
            tryInitialize();
        } catch (Throwable ex) {
            log.log(Level.SEVERE, MessageNames.STARTUP_DEPLOYER_FAILED_INIT,
                    ex);
            throw new ConfigurationException(ex,
                    MessageNames.STARTUP_DEPLOYER_FAILED_INIT);
        }
    }

    /**
     * If clientAppName has been set, then we're running a predetermined app as
     * a result of the entry in Config.xml. In that case, we're not going to
     * bother with any '-with' options.
     *
     * @throws IOException
     * @throws ParseException
     * @throws org.apache.commons.cli.ParseException
     */
    private void tryInitialize() throws IOException, ParseException, org.apache.commons.cli.ParseException {
        log.log(Level.FINE, MessageNames.STARTER_SERVICE_DEPLOYER_STARTING, myName);

        /*
         Establish the deployment directory.
         */
        deploymentDirectoryFile = fileUtility.getProfileDirectory().resolveFile(deployDirectory);
        if (deploymentDirectoryFile == null
                || deploymentDirectoryFile.getType() != FileType.FOLDER) {
            log.log(Level.WARNING, MessageNames.NO_DEPLOYMENT_DIRECTORY,
                    new Object[]{deployDirectory, fileUtility.getProfileDirectory()});
        }
        /*
         * Find the name of the client we need to deploy.  
         */
        /* First argument was the profile name.  Second argument is the name of 
         * the client app to run.  All the rest are parameters to the client
         * app.
         */
        if (clientAppName == null && commandLineArguments.length < 2) {
            System.out.println(messages.getString(MessageNames.CLIENT_APP_USAGE));
            System.exit(1);
        }
        String[] clientAppArgs = new String[0];
        String additionalApps = Strings.EMPTY;
        if (clientAppName == null) {
            String[] argsWithoutProfile = new String[commandLineArguments.length - 1];
            System.arraycopy(commandLineArguments, 1, argsWithoutProfile, 0,
                    argsWithoutProfile.length);
            CommandLine cl = CommandLineParsers.parseCommandLineAppRunnerLine(argsWithoutProfile);
            // At this point, any remaining args after -with are in getArgs()
            // The first of those is the app name.
            clientAppName = cl.getArgs()[0];
            clientAppArgs = new String[cl.getArgs().length - 1];
            System.arraycopy(cl.getArgs(), 1, clientAppArgs, 0, clientAppArgs.length);
            if (cl.hasOption(CommandLineParsers.WITH)) {
                additionalApps = cl.getOptionValue(CommandLineParsers.WITH);
            }
        } else {
            clientAppArgs = new String[commandLineArguments.length - 1];
            System.arraycopy(commandLineArguments, 1, clientAppArgs, 0,
                    clientAppArgs.length);
        }
        if (!Strings.EMPTY.equals(additionalApps)) {
            startAdditionalApps(additionalApps);
        }

        /*
         See if the clientAppName happens to be a 'jar' name and refers to a 
         jar file.  If so, that's the service archive.
         */
        FileObject serviceArchive = null;
        if (isAppArchive(clientAppName)) {
            serviceArchive = fileUtility.resolveFile(clientAppName);
        } else {
            serviceArchive = findServiceArchiveForName(clientAppName);
        }

        if (serviceArchive == null) {
            System.err.println(MessageFormat.format(messages.getString(MessageNames.NO_SUCH_CLIENT_APP), clientAppName));
            System.exit(1);
        }
        // Deploy the service
        deployServiceArchive(serviceArchive, clientAppArgs);
        // Run the main method with the remaining command line parameters.
    }

    private FileObject findServiceArchiveForName(String appName) throws FileSystemException {
        // Locate the service archive that has the client's name.
        // First get all the jar files.
        List<FileObject> serviceArchives
                = Utils.findChildrenWithSuffix(deploymentDirectoryFile,
                        com.stratuscom.harvester.Strings.JAR);
        //Then find the one that starts with the client name
        for (FileObject fo : serviceArchives) {
            if (fo.getName().getBaseName().startsWith(appName + com.stratuscom.harvester.Strings.DASH)) {
                return fo;
            }
        }
        return null;
    }

    boolean isAppArchive(String appName) {
        return appName.endsWith(com.stratuscom.harvester.Strings.JAR)
                && (new File(appName)).isFile()
                && (new File(appName)).canRead();
    }

    /**
     * Start the apps indicated in the command line's '-with' argument.
     *
     * @param appList
     */
    private void startAdditionalApps(String appList) throws FileSystemException {
        //Split on comma
        String[] additionalApps = appList.split(Strings.COMMA);
        for (String app : additionalApps) {
            log.log(Level.INFO, MessageNames.STARTING_WITH_SERVICE, app);
            FileObject serviceArchive = findServiceArchiveForName(app);
            if (serviceArchive==null) {
                System.err.println(MessageFormat.format(messages.getString(MessageNames.NO_SUCH_CLIENT_APP), clientAppName));
                System.exit(1);
            }
            deployServiceArchive(serviceArchive, new String[0]);
        }
    }

    private void deployServiceArchive(FileObject archiveFile, String[] commandLineArgs) {
        try {
            /* Try the archive in all the deployers to see if someone can 
             * handle it. For now there's only one.
             */

            /*
             * Create the ApplicationEnvironment for the archive.
             */
            ServiceLifeCycle deployedApp = deployer.deployServiceArchive(myName, archiveFile);

            deployedApp.startWithArgs(commandLineArgs);
        } catch (Throwable t) {
            log.log(Level.WARNING, MessageNames.FAILED_DEPLOY_SERVICE, archiveFile.toString());
            log.log(Level.WARNING, MessageNames.EXCEPTION_THROWN, Utils.stackTrace(t));
        }
    }
}
