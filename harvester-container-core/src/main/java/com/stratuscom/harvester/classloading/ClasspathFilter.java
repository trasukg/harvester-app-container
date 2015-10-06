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
package com.stratuscom.harvester.classloading;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author trasukg
 */
public class ClasspathFilter {

    private String jarName = null;

    public void setJarName(String s) {
        jarName = s;
    }

    public String getJarName() {
        return jarName;
    }
    List<Acceptor> acceptors = new ArrayList<Acceptor>();

    public List<Acceptor> getAcceptors() {
        return acceptors;
    }

    /**
    Returns true if this filter accepts the given resource path.
    Resource path is in the form '/META-INF/ABC.xyz'
    @param resourcePath
    @return
     */
    public boolean acceptsResource(String resourcePath) {

        for (Acceptor a : getAcceptors()) {
            if (a.acceptsResource(resourcePath)) {
                return true;
            }
        }
        return false;
    }
    
    public String toString() {
        return "("+acceptorString()+")";
    }
    
    private String acceptorString() {
        StringBuilder sb=new StringBuilder();
        boolean first=true;
        for (Acceptor a: getAcceptors()) {
            if (!first) {
                sb.append(", ");
            } else {
                first=false;
            }
            sb.append(a);     
        }
        return sb.toString();
    }
}