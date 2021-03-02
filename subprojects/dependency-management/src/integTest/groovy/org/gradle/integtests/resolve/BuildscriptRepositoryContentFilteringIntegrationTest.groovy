/*
 * Copyright 2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.integtests.resolve

import org.gradle.integtests.fixtures.AbstractHttpDependencyResolutionTest

class BuildscriptRepositoryContentFilteringIntegrationTest extends AbstractHttpDependencyResolutionTest {

    def "can exclude a module from a repository using #notation for buildscript classpath (in settings: #inSettings)"() {
        def mod = ivyHttpRepo.module('org', 'foo', '1.0').publish()

        given:
        def repositories = """
                repositories {
                    maven {
                        url "${mavenHttpRepo.uri}"
                        content {
                            $notation
                        }
                    }
                    ivy {
                        url "${ivyHttpRepo.uri}"
                    }
                }
"""
        if (inSettings) {
            settingsFile << """
pluginManagement {
    $repositories
}

"""
            repositories = ""
        }
        settingsFile << """
rootProject.name = 'test'
"""
        buildFile.text = """
            buildscript {
                $repositories

                dependencies {
                    classpath "org:foo:1.0"
                }
            }
            plugins {
                id('base')
            }
        """

        when:
        mod.ivy.expectGet()
        mod.artifact.expectGet()

        run 'buildEnvironment'

        then:
        outputContains("org:foo:1.0")

        where:
        inSettings  | notation
        true        | "excludeGroup('org')"
        true        | "excludeGroupByRegex('or.+')"
        false       | "excludeGroup('org')"
        false       | "excludeGroupByRegex('or.+')"
    }

    def 'exclusive content filtering in settings prevents adding repositories in project'() {
        given:
        settingsFile << """
pluginManagement {
    repositories {
        ivy {
            url "irrelevant"
        }
        exclusiveContent {
            forRepository {
                maven {
                    url "whatever"
                }
            }
            filter {
                includeGroup('org')
            }
        }
    }
}

rootProject.name = 'test-exclusive'
"""

        buildFile << """
buildscript {
    repositories {
        maven {
            url 'another'
        }
    }
}

plugins {
    id('base')
}
"""
        when:
        fails 'buildEnvironment'

        then:
        failureCauseContains("When using exclusive repository content in 'settings.pluginManagement.repositories', you cannot add repositories to 'buildscript.repositories'.")
    }


}
