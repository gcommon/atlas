/*
 * Copyright 2014-2016 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.atlas.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken


object JsonParserHelper {

  @scala.annotation.tailrec
  def skip(parser: JsonParser, stop: JsonToken): Unit = {
    val t = parser.nextToken()
    if (t != null && t != stop) skip(parser, stop)
  }

  @scala.annotation.tailrec
  def skipTo(parser: JsonParser, token: JsonToken, stop: JsonToken): Boolean = {
    val t = parser.nextToken()
    if (t == null || t == token || t == stop) t == token else skipTo(parser, token, stop)
  }

  def fail(parser: JsonParser, msg: String): Nothing = {
    val loc = parser.getCurrentLocation
    val line = loc.getLineNr
    val col = loc.getColumnNr
    val fullMsg = s"$msg (line=$line, col=$col)"
    throw new IllegalArgumentException(fullMsg)
  }

  def requireNextToken(parser: JsonParser, expected: JsonToken) {
    val t = parser.nextToken()
    if (t != expected) fail(parser, s"expected $expected but received $t")
  }

  def nextString(parser: JsonParser): String = {
    requireNextToken(parser, JsonToken.VALUE_STRING)
    parser.getText
  }

  def nextStringList(parser: JsonParser): List[String] = {
    val vs = List.newBuilder[String]
    foreachItem(parser) {
      vs += parser.getText
    }
    vs.result()
  }

  def nextInt(parser: JsonParser): Int = {
    requireNextToken(parser, JsonToken.VALUE_NUMBER_INT)
    parser.getValueAsInt
  }

  def nextLong(parser: JsonParser): Long = {
    requireNextToken(parser, JsonToken.VALUE_NUMBER_INT)
    parser.getValueAsLong
  }

  def nextDouble(parser: JsonParser): Double = {
    import com.fasterxml.jackson.core.JsonToken._
    parser.nextToken() match {
      case VALUE_NUMBER_INT   => parser.getValueAsLong
      case VALUE_NUMBER_FLOAT => parser.getValueAsDouble
      case VALUE_STRING       => java.lang.Double.valueOf(parser.getText)
      case t                  => fail(parser, s"expected VALUE_NUMBER_FLOAT but received $t")
    }
  }

  def foreachItem[T](parser: JsonParser)(f: => T) {
    requireNextToken(parser, JsonToken.START_ARRAY)
    while (parser.nextToken() != JsonToken.END_ARRAY) { f }
  }

  def foreachField[T](parser: JsonParser)(f: PartialFunction[String, T]) {
    while (skipTo(parser, JsonToken.FIELD_NAME, JsonToken.END_OBJECT)) {
      f(parser.getText)
    }
  }

  def firstField[T](parser: JsonParser)(f: PartialFunction[String, T]) {
    if (skipTo(parser, JsonToken.FIELD_NAME, JsonToken.END_OBJECT)) {
      f(parser.getText)
    }
  }

  def skipToEndOfObject(parser: JsonParser): Unit = skip(parser, JsonToken.END_OBJECT)
}