package org.eigengo.scalad.mongo

import java.util.UUID

trait UuidChecker {
  def parseUuidString(token: String): Option[UUID] = {
    if (token.length != 36) None
    else try Some(UUID.fromString(token))
    catch {
      case p: IllegalArgumentException => return None
    }
  }
}
