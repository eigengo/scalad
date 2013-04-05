package org.eigengo.scalad.mongo

import java.util.{Date, UUID}
import java.text.{ParseException, SimpleDateFormat}

trait UuidChecker {
  def parseUuidString(token: String): Option[UUID] = {
    if (token.length != 36) None
    else try Some(UUID.fromString(token))
    catch {
      case p: IllegalArgumentException => return None
    }
  }
}

trait IsoDateChecker {
  private val localIsoDateFormatter = new ThreadLocal[SimpleDateFormat] {
    override def initialValue() = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
  }

  def dateToIsoString(date: Date) = localIsoDateFormatter.get().format(date)

  def parseIsoDateString(date: String): Option[Date] =
    if (date.length != 28) None
    else try Some(localIsoDateFormatter.get().parse(date))
    catch {
      case p: ParseException => None
    }
}
