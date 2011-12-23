package scalad

import java.lang.StringBuffer


object PreparedQuery {

  def apply(rawQuery: Query)(implicit simplifier: RestrictionSimplifier) = {
    abstract case class Token(position: Int)
    case class Start() extends Token(0)
    case class Mid(position_ : Int, text: String) extends Token(position_)
    case class End(text: String) extends Token(0)

    val queryText: String = rawQuery.query

    def next(token: Token): Token = {
      val text = new StringBuffer
      var openString: Boolean = false
      var openEscape: Boolean = false
      val start = token.position + 1

      for (i <- start until queryText.length()) {
        val c = queryText.charAt(i)
        if (c == '\\' && !openEscape) openEscape = !openEscape
        if (c == '\'' && !openEscape) openString = !openString

        if (!openEscape && !openString) {
          if (text.length() > 0) {
            c match {
              case '?' => return Mid(i, "?")
              case ' ' => return Mid(i, text.toString)
              case '=' => return Mid(i, text.toString)
              case '<' => return Mid(i, text.toString)
              case '>' => return Mid(i, text.toString)
              case '!' => return Mid(i, text.toString)
              case '(' => return Mid(i, text.toString)
              case ')' => return Mid(i, text.toString)
              case _ =>
            }
          }
          text.append(c)
        }
      }

      End(text.toString)
    }

    def loop(token: Token, position: Int, params: Seq[PreparedQueryParameter]): Seq[PreparedQueryParameter] = {
      next(token) match {
        case t@Mid(_, text) =>
          text.charAt(0) match {
            case ':' => NamedPreparedQueryParameter(text, position) +: loop(t, position + 1, params)
            case '?' => PositionalPreparedQueryParameter(position) +: loop(t, position + 1, params)
            case _ => loop(t, position, params)
          }
        case End("") => params
        case End(text) =>
          text.charAt(0) match {
            case ':' => NamedPreparedQueryParameter(text, position) +: params
            case '?' => PositionalPreparedQueryParameter(position) +: params
            case _ => params
          }
      }
    }

    val params = loop(Start(), 0, Nil)
    var positionalQuery = queryText
    params.foreach { p =>
      p match {
        case NamedPreparedQueryParameter(name, _) => positionalQuery = positionalQuery.replace(name, "?")
        case _ =>
      }
    }

    new PreparedQuery(positionalQuery, params,
      simplifier.simplifyRestriction(rawQuery.restriction),
      rawQuery.orderByClauses, rawQuery.groupByClauses)
  }

}

class PreparedQuery private(private[scalad] val query: String,
                            private[scalad] val parameters: Iterable[PreparedQueryParameter],
                            private[scalad] val restriction: Restriction,
                            private[scalad] val orderByClauses: List[OrderBy],
                            private[scalad] val groupByClauses: List[GroupBy]) {


}

abstract case class PreparedQueryParameter()
case class PositionalPreparedQueryParameter(position: Int) extends PreparedQueryParameter
case class NamedPreparedQueryParameter(name: String, position: Int) extends PreparedQueryParameter