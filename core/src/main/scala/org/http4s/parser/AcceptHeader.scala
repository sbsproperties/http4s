/*
 * Derived from https://github.com/spray/spray/blob/v1.1-M7/spray-http/src/main/scala/spray/http/parser/AcceptHeader.scala
 *
 * Copyright (C) 2011-2012 spray.io
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
package org.http4s
package parser

import org.parboiled2._
import scalaz.Validation
import org.http4s.Header.Accept

private[parser] trait AcceptHeader {

  def ACCEPT(value: String): Validation[ParseErrorInfo, Accept] = new AcceptParser(value).parse

  private class AcceptParser(value: String) extends Http4sHeaderParser[Accept](value) with MediaParser {

    def entry: Rule1[Header.Accept] = rule {
      oneOrMore(FullRange).separatedBy("," ~ OptWS) ~ EOL ~> { xs: Seq[MediaRange] =>
        Header.Accept(xs.head, xs.tail: _*)}
    }

    def FullRange: Rule1[MediaRange] = rule {
      (MediaRangeDef ~ optional( QAndExtensions )) ~> {
        (mr: MediaRange, params: Option[(Q, Seq[(String, String)])]) =>
          params.map{ case (q, extensions) =>
            val m1 = if (q.intValue != Q.MAX_VALUE) mr.withQuality(q) else mr
            if (extensions.isEmpty) m1 else m1.withExtensions(extensions.toMap)
          }.getOrElse(mr)
      }
    }

    def QAndExtensions: Rule1[(Q, Seq[(String, String)])] = rule {
      AcceptParams | (oneOrMore(MediaTypeExtension) ~> {s: Seq[(String, String)] => (Q.Unity, s) })
    }

    def AcceptParams: Rule1[(Q, Seq[(String, String)])] = rule {
      (";" ~ OptWS ~ "q" ~ "=" ~ QValue ~ zeroOrMore(MediaTypeExtension)) ~> ((_:Q,_:Seq[(String, String)]))
    }
  }
}
