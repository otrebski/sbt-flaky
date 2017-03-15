package flaky

import org.scalatest.{Matchers, WordSpec}

class package$Test extends WordSpec with Matchers {

  "findCommonString" should {
    "return empty for empty list" in {
      findCommonString(List.empty[String]) shouldBe None
    }

    "return element from list for list of 1 element" in {
      findCommonString(List("abc")) shouldBe Some("abc")
    }


    "return element from list for list of the same strings" in {
      findCommonString(List("abc", "abc", "abc")) shouldBe Some("abc")
    }

    "return common element part of 2 strings - difference at end" in {
      findCommonString(List("abc", "abd")) shouldBe Some("ab_")
    }


    "return common element part of 2 strings - difference at start" in {
      findCommonString(List("abc", "abc")) shouldBe Some("_bc")
    }

    "return common element part of 2 strings - difference int the middle" in {
      findCommonString(List("abc", "abc")) shouldBe Some("a_c")
    }

    

    "return common element part of 3 strings with 1 difference" in {
      findCommonString(List("abcde", "AbXde","abcde" )) shouldBe Some("ab_de")
    }

    "return common element part of 3 strings with 2 differences" in {
      findCommonString(List("abcde", "AbXde","abcdX" )) shouldBe Some("ab_d_")
    }

    "return common element part of 3 strings with different length 1" in {
      findCommonString(List("abcdefg", "abcdefg","abcdefgh")) shouldBe Some("abcdefg_")
    }

    "return common element part of 3 strings with different length 2" in {
      findCommonString(List("0abcdefg", "abcdefg","abcdefg")) shouldBe Some("_abcdefg")
    }

  }

}
