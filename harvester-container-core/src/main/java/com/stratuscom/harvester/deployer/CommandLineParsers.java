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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Utility class to encapsulate any command line parsers that we use,
 * along with their configurations.
 * 
 */
public class CommandLineParsers {
    public static final String WITH="with";

    static CommandLine parseCommandLineAppRunnerLine(String[] args) throws ParseException {
        Options options = new Options();

        Option courses = Option.builder(WITH)
                .argName(WITH)
                .hasArg()
                .valueSeparator(',')
                .desc("comma-separated list of applications to run with the main app")
                .build();
        options.addOption(courses);
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = null;
        commandLine = parser.parse(options, args);
        return commandLine;
    }

    
}
