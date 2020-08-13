/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.performance.regression.corefeature

import org.gradle.performance.AbstractCrossBuildPerformanceTest
import org.gradle.performance.categories.PerformanceRegressionTest
import org.junit.experimental.categories.Category
import org.gradle.testfixtures.SafeUnroll

import static org.gradle.performance.generator.JavaTestProject.LARGE_JAVA_MULTI_PROJECT
import static org.gradle.performance.generator.JavaTestProject.LARGE_MONOLITHIC_JAVA_PROJECT

@Category(PerformanceRegressionTest)
class TaskAvoidancePerformanceTest extends AbstractCrossBuildPerformanceTest {

    @SafeUnroll
    def "help on #testProject with lazy and eager tasks"() {
        given:
        runner.testGroup = "configuration avoidance"
        runner.buildSpec {
            warmUpCount = warmUpRuns
            invocationCount = runs
            projectName(testProject.projectName).displayName("lazy").invocation {
                tasksToRun("help")
            }
        }
        runner.baseline {
            warmUpCount = warmUpRuns
            invocationCount = runs
            projectName(testProject.projectName).displayName("eager").invocation {
                tasksToRun("help").args("-Dorg.gradle.internal.tasks.eager=true")
            }
        }

        when:
        def results = runner.run()

        then:
        results

        where:
        testProject                   | warmUpRuns | runs
        LARGE_MONOLITHIC_JAVA_PROJECT | 5          | 10
        LARGE_JAVA_MULTI_PROJECT      | 5          | 10
    }
}
