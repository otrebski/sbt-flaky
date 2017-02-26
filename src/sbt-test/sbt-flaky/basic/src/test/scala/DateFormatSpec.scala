import java.text.SimpleDateFormat
import java.time.Instant
import java.util.{Date, TimeZone}

import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{Matchers, WordSpecLike}

class DateFormatSpec extends WordSpecLike with Matchers with LazyLogging {
//  2011-12-03T10:15:30Z
  "Instant" should {

    val format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    format.setTimeZone(TimeZone.getTimeZone("UTC"))

    "format date 1" in {
      val formattedDate = Instant.now().toString
      formattedDate shouldBe format.format(new Date())
    }

    "format date 2" in {
      val date = new Date()
      val formattedDate = Instant.ofEpochMilli(date.getTime).toString
      formattedDate shouldBe format.format(date)
    }

    "format date 3" in {
      for (timestamp <- 0 to 20){
        val date = new Date(System.currentTimeMillis() + timestamp)
        val formattedDate = Instant.ofEpochMilli(date.getTime).toString
        formattedDate shouldBe format.format(date)
      }

    }


  }
}
