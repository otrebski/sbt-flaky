import scalatags.Text.all._
import scalatags.stylesheet._

object TestingCss extends App {
  println(TestCss.styleSheetText)
}

object TestCss extends StyleSheet {
  initStyleSheet()

  val x = cls(
    backgroundColor := "red",
    height := 125
  )
  val y = cls.hover(
    opacity := 0.5
  )

  val z = cls(x.splice, y.splice)
}
