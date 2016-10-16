/*
 * Copyright (c) 2016 Drimachine.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.drimachine.grakmat.grammars

import org.drimachine.grakmat.*
import java.io.File

data class URI(val path: String, val anchor: String, val params: Map<String, String>) {
    override fun toString(): String {
        val anchorString = if (anchor.isEmpty()) "" else "#$anchor"
        val paramsString = if (params.isEmpty()) "" else params
                .map { it: Map.Entry<String, String> -> "${it.key}=${it.value}" }
                .joinToString(prefix = "?", separator = "&")
        return "$path$anchorString$paramsString"
    }

    companion object {
        private val hash:         Parser<Char> = char('#') withName "'#'"
        private val questionMark: Parser<Char> = char('?') withName "'?'"
        private val ampersand:    Parser<Char> = char('&') withName "'&'"
        private val equalsSign:   Parser<Char> = char('=') withName "'='"

        private val paramName: Parser<String> = zeroOrMore(except('&', '='))
                .map { it: List<Char> -> it.joinToString("") }
                .withName("parameter name")
        private val pairWithoutEqualsSign: Parser<Pair<String, String>> = paramName
                .map { it: String -> it to "" }
        private val pairWithoutValue: Parser<Pair<String, String>> = (paramName before equalsSign)
                .map { it: String -> it to "" }
        private val paramValue: Parser<String> = zeroOrMore(except('&'))
                .map { it: List<Char> -> it.joinToString("") }
                .withName("parameter value")
        private val pairWithValue: Parser<Pair<String, String>> = (paramName before equalsSign and paramValue)
        private val pair: Parser<Pair<String, String>> = (pairWithValue or pairWithoutValue or pairWithoutEqualsSign)
        private val pairs: Parser<List<Pair<String, String>>> = (pair and zeroOrMore(ampersand then optional(pair)))
                .map { it: Pair<Pair<String, String>, List<Pair<String, String>?>> ->
                    val result = arrayListOf<Pair<String, String>>()
                    val (firstPair, pairs) = it
                    result += firstPair
                    result += pairs.filterNotNull()
                    return@map result.filterNot { it.first.isEmpty() }
                }
        private val params: Parser<Map<String, String>> = (questionMark then optional(pairs))
                .map { it -> it?.toMap() ?: emptyMap() }
                .withName("parameters")

        private val anchor: Parser<String> = (hash then zeroOrMore(except('?')))
                .map { it: List<Char> -> it.joinToString("") }
                .withName("anchor")
        private val path: Parser<String> = (zeroOrMore(except('#', '?')))
                .map { it: List<Char> -> it.joinToString("") }
                .withName("path")
        private val uri: Parser<URI> = (path and optional(anchor) and optional(params))
                .map { it: Pair<Pair<String, String?>, Map<String, String>?> ->
                    val (pathAndAnchor, params: Map<String, String>?) = it
                    val (path: String, anchor: String?) = pathAndAnchor

                    return@map URI(path, anchor ?: "", params ?: emptyMap())
                }
                .withName("URI")

        @JvmStatic fun parse(json: String): URI = uri.parse(json)
        @JvmStatic fun parseFile(file: File): URI = uri.parseFile(file)
    }
}
