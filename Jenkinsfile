#!groovy

/*
 * Copyright © 2016, 2018 IBM Corp. All rights reserved.
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

def runTests(testEnv, isServiceTests) {
    node {
        if (isServiceTests) {
            testEnv.add('GRADLE_TARGET=cloudantServiceTest')
        } else {
            testEnv.add('GRADLE_TARGET=test')
        }

        // Unstash the built content
        unstash name: 'built'

        //Set up the environment and run the tests
        withEnv(testEnv) {
            withCredentials([(env.CREDS_ID.contains('iam')) ? string(credentialsId: env.CREDS_ID, variable: 'IAM_API_KEY') : usernamePassword(credentialsId: env.CREDS_ID, usernameVariable: 'DB_USER', passwordVariable: 'DB_PASSWORD')]) {
                try {
                    sh "./gradlew ${(env.DB_USER?.trim()) ? '-Dtest.server.user=$DB_USER -Dtest.server.password=$DB_PASSWORD' : ''} -Dtest.server.host=\$DB_HOST -Dtest.server.port=\$DB_PORT -Dtest.server.protocol=\$DB_HTTP \$GRADLE_TARGET"
                } finally {
                    junit '**/build/test-results/**/*.xml'
                }
            }
        }
    }
}

stage('Build') {
    // Checkout, build and assemble the source and doc
    node {
        checkout scm
        sh './gradlew clean assemble'
        stash name: 'built'
    }
}

stage('QA') {
    // Define the matrix environments
    def CLOUDANT_ENV = ['DB_HTTP=https', 'DB_HOST=clientlibs-test.cloudant.com', 'DB_PORT=443', 'DB_IGNORE_COMPACTION=true', 'CREDS_ID=clientlibs-test']
    def CLOUDANT_IAM_ENV = ['DB_HTTP=https', 'DB_HOST=clientlibs-test.cloudant.com', 'DB_PORT=443', 'DB_IGNORE_COMPACTION=true', 'CREDS_ID=clientlibs-test-iam']
    def COUCH1_6_ENV = ['DB_HTTP=http', 'DB_HOST=cloudantsync002.bristol-victoria.uk.ibm.com', 'DB_PORT=5984', 'DB_IGNORE_COMPACTION=false', 'CREDS_ID=couchdb']
    def COUCH2_0_ENV = ['DB_HTTP=http', 'DB_HOST=cloudantsync002.bristol-victoria.uk.ibm.com', 'DB_PORT=5985', 'DB_IGNORE_COMPACTION=true', 'CREDS_ID=couchdb']
    def CLOUDANT_LOCAL_ENV = ['DB_HTTP=http', 'DB_HOST=cloudantsync002.bristol-victoria.uk.ibm.com', 'DB_PORT=8081', 'DB_IGNORE_COMPACTION=true', 'CREDS_ID=couchdb']

    // Standard builds do Findbugs and test against Cloudant
    def axes = [
            Spotbugs:
                    {
                        node {
                            unstash name: 'built'
                            // Spotbugs
                            try {
                                sh './gradlew -Dspotbugs.xml.report=true spotbugsMain'
                            } finally {
                                recordIssues enabledForFailure: true, tool: spotBugs(pattern: '**/build/reports/spotbugs/*.xml')
                            }
                        }
                    },
            Cloudant: {
                runTests(CLOUDANT_ENV, true)
            }
    ]

    // For the master branch, add additional axes to the coverage matrix for Couch 1.6, 2.0
    // and Cloudant Local
    if (env.BRANCH_NAME == "master") {
        axes.putAll(
//                Couch1_6: {
//                    runTests(COUCH1_6_ENV, false)
//                },
//                Couch2_0: {
//                    runTests(COUCH2_0_ENV, false)
//                },
//                CloudantLocal: {
//                    runTests(CLOUDANT_LOCAL_ENV, false)
//                },
                CloudantIam: {
                    runTests(CLOUDANT_IAM_ENV, true)
                }
        )
    }

    // Run the required axes in parallel
    parallel(axes)
}

// Publish the master branch
stage('Publish') {
    if (env.BRANCH_NAME == "master") {
        node {
            unstash name: 'built'

            // Upload using the ossrh creds (upload destination logic is in build.gradle)
            withCredentials([usernamePassword(credentialsId: 'ossrh-creds', passwordVariable: 'OSSRH_PASSWORD', usernameVariable: 'OSSRH_USER'), usernamePassword(credentialsId: 'signing-creds', passwordVariable: 'KEY_PASSWORD', usernameVariable: 'KEY_ID'), file(credentialsId: 'signing-key', variable: 'SIGNING_FILE')]) {
                sh './gradlew -Dsigning.keyId=$KEY_ID -Dsigning.password=$KEY_PASSWORD -Dsigning.secretKeyRingFile=$SIGNING_FILE -DossrhUsername=$OSSRH_USER -DossrhPassword=$OSSRH_PASSWORD upload'
            }
        }
    }
    gitTagAndPublish {
        isDraft=true
        releaseApiUrl='https://api.github.com/repos/cloudant/java-cloudant/releases'
    }
}
