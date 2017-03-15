
package object flaky {

  def findCommonString(list: List[String]): Option[String] = {
    list match {
      case Nil => None
      case s::Nil => Some(s)
      case l =>
        Some(l.head.intersect(l(1)))
    }}

}
