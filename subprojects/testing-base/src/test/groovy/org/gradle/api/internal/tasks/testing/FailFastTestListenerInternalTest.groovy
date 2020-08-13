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

package org.gradle.api.internal.tasks.testing

import org.gradle.api.internal.tasks.testing.logging.SimpleTestDescriptor
import org.gradle.api.internal.tasks.testing.logging.SimpleTestOutputEvent
import org.gradle.api.internal.tasks.testing.results.TestListenerInternal
import org.gradle.api.tasks.testing.TestOutputEvent
import org.gradle.api.tasks.testing.TestResult
import spock.lang.Specification
import spock.lang.Subject
import org.gradle.testfixtures.SafeUnroll

class FailFastTestListenerInternalTest extends Specification {
    TestExecuter testExecuter = Mock()
    TestListenerInternal delegate = Mock()

    TestDescriptorInternal testDescriptor = new SimpleTestDescriptor()
    TestStartEvent startEvent = new TestStartEvent(0)
    TestCompleteEvent completeEvent = new TestCompleteEvent(0)
    TestResult testResult = new SimpleTestResult()
    TestOutputEvent event = new SimpleTestOutputEvent()

    @Subject
    FailFastTestListenerInternal unit

    def setup() {
        unit = new FailFastTestListenerInternal(testExecuter, delegate)
    }

    def "started invokes delegate"() {
        when:
        unit.started(testDescriptor, startEvent)

        then:
        1 * delegate.started(testDescriptor, startEvent)
    }

    def "output invokes delegate"() {
        when:
        unit.output(testDescriptor, event)

        then:
        1 * delegate.output(testDescriptor, event)
    }

    def "completed calls stopNow on failure"() {
        testResult.resultType = TestResult.ResultType.FAILURE

        when:
        unit.completed(testDescriptor, testResult, completeEvent)

        then:
        1 * testExecuter.stopNow()
    }

    @SafeUnroll
    def "completed invokes delegate with result #result"() {
        testResult.resultType = result

        when:
        unit.completed(testDescriptor, testResult, completeEvent)

        then:
        1 * delegate.completed(testDescriptor, testResult, completeEvent)

        where:
        result << TestResult.ResultType.values()
    }

    @SafeUnroll
    def "after failure completed indicates failure on composite for result #result"() {
        TestResult failedResult = new SimpleTestResult()
        failedResult.resultType = TestResult.ResultType.FAILURE
        testDescriptor.composite = true
        testResult.resultType = result

        given:
        unit.completed(testDescriptor, failedResult, completeEvent)

        when:
        unit.completed(testDescriptor, testResult, completeEvent)

        then:
        1 * delegate.completed(testDescriptor, { it.getResultType() == TestResult.ResultType.FAILURE }, completeEvent)

        where:
        result << TestResult.ResultType.values()
    }

    @SafeUnroll
    def "after failure completed indicates skipped on non-composite for result #result"() {
        TestResult failedResult = new SimpleTestResult()
        failedResult.resultType = TestResult.ResultType.FAILURE
        testDescriptor.composite = false
        testResult.resultType = result

        given:
        unit.completed(testDescriptor, failedResult, completeEvent)

        when:
        unit.completed(testDescriptor, testResult, completeEvent)

        then:
        1 * delegate.completed(testDescriptor, { it.getResultType() == TestResult.ResultType.SKIPPED }, completeEvent)

        where:
        result << TestResult.ResultType.values()
    }
}
