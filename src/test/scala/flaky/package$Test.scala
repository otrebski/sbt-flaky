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

    "return element part from one character strings when it's at the beginning" in {
      findCommonString(List("abc", "a")) shouldBe Some("a__")
    }

    "return element part from one character strings when it's in the middle" in {
      findCommonString(List("abc", "b")) shouldBe Some("_b_")
    }

    "return element part from one character strings when it's at the end" in {
      findCommonString(List("abc", "c")) shouldBe Some("__c")
    }

    "return common element part of 2 strings - difference at end" in {
      findCommonString(List("abc", "abd")) shouldBe Some("ab_")
    }


    "return common element part of 2 strings - difference at start" in {
      findCommonString(List("abc", "Abc")) shouldBe Some("_bc")
    }

    "return common element part of 2 strings - difference int the middle" in {
      findCommonString(List("abc", "aBc")) shouldBe Some("a_c")
    }

    "return common element part of 3 strings with 1 difference" in {
      findCommonString(List("abcde", "abXde","abcde" )) shouldBe Some("ab_de")
    }

    "return common element part of 3 strings with 2 differences" in {
      findCommonString(List("abcde", "abXde","abcdX" )) shouldBe Some("ab_d_")
    }

    "fill missing chars inside the string" in {
      findCommonString(List("abcccde", "abXde")) shouldBe Some("ab___de")
    }

    "return common element part of 3 strings with different length 1" in {
      findCommonString(List("abcdefg", "abcdefg","abcdefgh")) shouldBe Some("abcdefg_")
    }

    "return common element part of 3 strings with different length 2" in {
      findCommonString(List("0abcdefg", "abcdefg","abcdefg")) shouldBe Some("_abcdefg")
    }

    "process real life case" in {
      val input = """java.lang.AssertionError: assertion failed: expected User(Name(Kowalski),List(),List(),List()), found User(Name(Kowalski),List(Property(P1(3908304518889162941),P2(xxx),P3(X),192838475652)),List(),List())
                    |java.lang.AssertionError: assertion failed: expected User(Name(Kowalski),List(),List(),List()), found User(Name(Kowalski),List(Property(P1(4066995287767169607),P2(xxx),P3(X),1223234)),List(),List())
                    |java.lang.AssertionError: assertion failed: expected User(Name(Kowalski),List(),List(),List()), found User(Name(Kowalski),List(Property(P1(3339977301001549636),P2(xxx),P3(X),654556765778)),List(),List())
                    |java.lang.AssertionError: assertion failed: expected User(Name(Kowalski),List(),List(),List()), found User(Name(Kowalski),List(Property(P1(220123700341947058),P2(xxx),P3(X),2333223)),List(),List())
                    |java.lang.AssertionError: assertion failed: expected User(Name(Kowalski),List(),List(),List()), found User(Name(Kowalski),List(Property(P1(2168806444424252285),P2(xxx),P3(X),988667678)),List(),List())
                    |java.lang.AssertionError: assertion failed: expected User(Name(Kowalski),List(),List(),List()), found User(Name(Kowalski),List(Property(P1(5918482956638044904),P2(xxx),P3(X),876866786787)),List(),List())
                    |java.lang.AssertionError: assertion failed: expected User(Name(Kowalski),List(),List(),List()), found User(Name(Kowalski),List(Property(P1(2848338480078734399),P2(xxx),P3(X),192838475652)),List(),List())""".stripMargin
      val commonS = "java.lang.AssertionError: assertion failed: expected User(Name(Kowalski),List(),List(),List()), found User(Name(Kowalski),List(Property(P1(___________________),P2(xxx),P3(X),____________)),List(),List())"

      findCommonString(input.lines.toList) shouldBe Some(commonS)
    }
  }

}
