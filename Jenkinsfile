#!groovy

/*
 * Copyright © 2016 IBM Corp. All rights reserved.
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

stage('Build') {
    // Checkout, build and assemble the source and doc
    node {
        checkout scm
        sh './gradlew clean assemble'
        stash name: 'built'
    }
}

stage('QA') {
    parallel(
            findBugs: {
                node {
                    unstash name: 'built'
                    // findBugs
                    try {
                        sh './gradlew -Dfindbugs.xml.report=true findbugsMain'
                    } finally {
                        step([$class: 'FindBugsPublisher', pattern: '**/build/reports/findbugs/*.xml'])
                    }
                }
            },
            cloudant: {
                node {
                    unstash name: 'built'
                    // tests
                    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'clientlibs-test', usernameVariable: 'DB_USER', passwordVariable: 'DB_PASSWORD']]) {
                        try {
                            sh './gradlew -Dtest.with.specified.couch=true -Dtest.couch.username=$DB_USER -Dtest.couch.password=$DB_PASSWORD -Dtest.couch.host=clientlibs-test.cloudant.com -Dtest.couch.port=443 -Dtest.couch.http=https -Dtest.couch.ignore.compaction=true -Dtest.couch.auth.headers=true cloudantServiceTest'
                        } finally {
                            junit '**/build/test-results/*.xml'
                        }
                    }
                }
            },
            couchdb1dot6: {
                node("couchdb1.6") {
                    unstash name: 'built'
                    try {
                        sh 'sudo /usr/local/bin/wrapdocker \n docker pull couchdb:1.6.1 \n docker run -d -p 5984:5984 --name couchdb couchdb:1.6.1\n ./gradlew test'
                    } finally {
                        junit '**/build/test-results/*.xml'
                    }
                }
            },
            couchdb2: {
                node("couchdb1.6") {
                    unstash name: 'built'
                    try {
                        sh 'sudo /usr/local/bin/wrapdocker \n docker pull klaemo/couchdb:2.0.0 \n docker run -d -p 5984:5984 --name couchdb klaemo/couchdb:2.0.0\n ./gradlew test'
                    } finally {
                        junit '**/build/test-results/*.xml'
                    }
                }
            })
}

// Publish the master branch
stage('Publish') {
    if (env.BRANCH_NAME == "master") {
        node {
            checkout scm // re-checkout to be able to git tag
            unstash name: 'built'
            // read the version name and determine if it is a release build
            version = readFile('VERSION').trim()
            isReleaseVersion = !version.toUpperCase(Locale.ENGLISH).contains("SNAPSHOT")

            // Upload using the ossrh creds (upload destination logic is in build.gradle)
            withCredentials([usernamePassword(credentialsId: 'ossrh-creds', passwordVariable: 'OSSRH_PASSWORD', usernameVariable: 'OSSRH_USER'), usernamePassword(credentialsId: 'signing-creds', passwordVariable: 'KEY_PASSWORD', usernameVariable: 'KEY_ID'), file(credentialsId: 'signing-key', variable: 'SIGNING_FILE')]) {
                sh './gradlew -Dsigning.keyId=$KEY_ID -Dsigning.password=$KEY_PASSWORD -Dsigning.secretKeyRingFile=$SIGNING_FILE -DossrhUsername=$OSSRH_USER -DossrhPassword=$OSSRH_PASSWORD upload'
            }

            // if it is a release build then do the git tagging
            if (isReleaseVersion) {

                // Read the CHANGES.md to get the tag message
                changes = """"""
                changes += readFile('CHANGES.md')
                tagMessage = """"""
                for (line in changes.readLines()) {
                    if (!"".equals(line)) {
                        // append the line to the tagMessage
                        tagMessage = "${tagMessage}${line}\n"
                    } else {
                        break
                    }
                }

                // Use git to tag the release at the version
                try {
                    // Awkward workaround until resolution of https://issues.jenkins-ci.org/browse/JENKINS-28335
                    withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'github-token', usernameVariable: 'GIT_USERNAME', passwordVariable: 'GIT_PASSWORD']]) {
                        sh "git config user.email \"nomail@hursley.ibm.com\""
                        sh "git config user.name \"Jenkins CI\""
                        sh "git config credential.username ${env.GIT_USERNAME}"
                        sh "git config credential.helper '!echo password=\$GIT_PASSWORD; echo'"
                        sh "git tag -a ${version} -m '${tagMessage}'"
                        sh "git push origin ${version}"
                    }
                } finally {
                    sh "git config --unset credential.username"
                    sh "git config --unset credential.helper"
                }
            }
        }
    }
}
