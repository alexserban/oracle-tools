/*
 * File: LocalPlatform.java
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

package com.oracle.tools.runtime;

import com.oracle.tools.runtime.java.JavaApplication;
import com.oracle.tools.runtime.java.LocalJavaApplicationBuilder;
import com.oracle.tools.runtime.network.Constants;

/**
 * The {@link Platform} in which this Java Virtual Machine is running.
 * <p>
 * Copyright (c) 2014. All Rights Reserved. Oracle Corporation.<br>
 * Oracle is a registered trademark of Oracle Corporation and/or its affiliates.
 *
 * @author Jonathan Knight
 * @author Brian Oliver
 */
public class LocalPlatform implements Platform
{
    /**
     * The singleton instance of {@link LocalPlatform}.
     */
    public static LocalPlatform INSTANCE = new LocalPlatform();

    /**
     * Construct a new {@link LocalPlatform}.
     */
    public LocalPlatform()
    {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHostname()
    {
        return Constants.getLocalHost();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public <A extends Application, B extends ApplicationBuilder<A>> B getApplicationBuilder(Class<A> applicationClass)
    {
        if (JavaApplication.class.isAssignableFrom(applicationClass))
        {
            return (B) new LocalJavaApplicationBuilder();
        }
        else
        {
            return (B) new SimpleApplicationBuilder();
        }
    }
}