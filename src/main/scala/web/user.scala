package web.session

import net.liftweb.http._
import net.liftweb.common.Box
import net.liftweb.common.Empty

object UserSession extends SessionVar[Box[Long]](Empty)
