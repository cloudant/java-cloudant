/*
 * Copyright (c) 2015 IBM Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.cloudant.tests.util;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.logging.Logger;

public class TestLog implements TestRule {

    //default to a logger name for this class, but replace when the rule is applied
    public Logger logger = Logger.getLogger(TestLog.class.getName());

    @Override
    public Statement apply(final Statement base, Description description) {
        //set a logger for the name of the test class
        logger = Logger.getLogger(description.getClassName());
        return base;
    }

}
