package flaky.slack

package object model {

  implicit def stringToImplicit(s: String): Option[String] = Some(s)

  implicit def longToImplicit(s: Long): Option[Long] = Some(s)
}
