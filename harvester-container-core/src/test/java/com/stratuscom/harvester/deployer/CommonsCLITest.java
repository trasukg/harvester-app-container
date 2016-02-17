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
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author trasukg
 */
public class CommonsCLITest {

    /**
     * An exploratory test to confirm the expected behaviour of the Apache Commons
     * CLI parser.
     * 
     * @throws Exception 
     */
    @Test
    public void testOptions() throws Exception {
        Options options = new Options();

        Option courses = Option.builder("with")
                .argName("with")
                .hasArgs()
                .valueSeparator(',')
                .desc("comma-separated list of applications to run with the main app")
                .build();
        options.addOption(courses);
        CommandLineParser parser = new DefaultParser();
        CommandLine commandLine = null;
        String[] args={
          "-with", "reggie,test-module,A"  
        };
        commandLine = parser.parse(options, args);
        assertEquals(3,commandLine.getOptionValues("with").length);
        
    }
    
    /**
     * Test the implementation of our CommandLineParsers class.
     * @throws Exception 
     */
    @Test
    public void testCommandLineParser() throws Exception {
        String[] args={
          "-with", "reggie,test-module,A"  
        };
        CommandLine commandLine = CommandLineParsers.parseCommandLineAppRunnerLine(args);
        assertEquals(1,commandLine.getOptionValues(CommandLineParsers.WITH).length);
    }
    
    /**
     * Test the implementation of our CommandLineParsers class.
     * @throws Exception 
     */
    @Test
    public void testAppArgs() throws Exception {
        String[] args={
          "-with", "reggie,test-module,A", "a", "b"
        };
        CommandLine commandLine = CommandLineParsers.parseCommandLineAppRunnerLine(args);
        assertEquals(1,commandLine.getOptionValues(CommandLineParsers.WITH).length);
        assertEquals("reggie,test-module,A", commandLine.getOptionValue(CommandLineParsers.WITH));
        assertEquals(2, commandLine.getArgs().length);
        assertEquals("a", commandLine.getArgs()[0]);
        assertEquals("b", commandLine.getArgs()[1]);
        
        
    }
    
}
