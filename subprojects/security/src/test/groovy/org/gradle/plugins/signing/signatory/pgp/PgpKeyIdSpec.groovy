/*
 * Copyright 2019 the original author or authors.
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

package org.gradle.plugins.signing.signatory.pgp

import org.gradle.testfixtures.SafeUnroll
import spock.lang.Specification

class PgpKeyIdSpec extends Specification  {

    protected key(arg) {
        new PgpKeyId(arg)
    }

    def "conversion is symmetrical"() {
        expect:
        key("ABCDABCD").asHex == "ABCDABCD"
    }

    @SafeUnroll
    def "conversion #hex"() {
        expect:
        key(hex).asLong == decimal
        key(decimal).asHex == hex

        where:
        hex        | decimal
        "AAAAAAAA" | 2863311530
        "DA124B92" | 3658632082
    }

    def "equals impl"() {
        expect:
        key("AAAAAAAA") == key(2863311530)
    }

    def "comparison"() {
        expect:
        key("00000000") < key("00000001")
        key("00000001") > key("00000000")
    }
}
