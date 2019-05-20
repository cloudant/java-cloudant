/*
 * Copyright © 2019 IBM Corp. All rights reserved.
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

package com.cloudant.tests.extensions;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit execution condition for checking whether an IAM API key is being used for auth during
 * testing. The environment variable takes precedence over the system property. The values are
 * exposed for use in other helpers and extensions.
 */
public class IamAuthCondition implements ExecutionCondition {

    private static final String IAM_API_KEY_PROP_NAME = "test.iam.api.key";
    private static final String IAM_API_KEY_ENV_VAR_NAME = "IAM_API_KEY";
    public static final String IAM_API_KEY;
    public static final boolean IS_IAM_ENABLED;

    static {
        String key = System.getenv(IAM_API_KEY_ENV_VAR_NAME);
        if (key == null) {
            key = System.getProperty(IAM_API_KEY_PROP_NAME);
        }
        IAM_API_KEY = key;
        IS_IAM_ENABLED = IAM_API_KEY != null;
    }

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        if (IS_IAM_ENABLED) {
            return ConditionEvaluationResult.disabled("Test is not supported when using IAM.");
        } else {
            return ConditionEvaluationResult.enabled("Test enabled.");
        }
    }
}
