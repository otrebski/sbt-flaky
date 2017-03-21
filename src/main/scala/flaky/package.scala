import java.lang.Math.max

package object flaky {

  private val SUBSTITUTION_CHAR = "_"

  // TODO using a simple reduction between pairs is a heuristic that has to be validated with real data
  def findCommonString(sequences: List[String]): Option[String] = sequences match {
    case Nil => None : Option[String]
    case _ => Some(sequences.sortBy(_.length).reduce(hirschberg))
  }

  // https://en.wikipedia.org/wiki/Hirschberg%27s_algorithm
  def hirschberg(firstSeq: String, secondSeq: String): String = {
    if (firstSeq.length > secondSeq.length)
      hirschberg(secondSeq, firstSeq)
    else if (firstSeq.length <= 1) {
      reduceInSimpleCase(firstSeq, secondSeq)
    } else {
      divideIntoSubProblems(firstSeq, secondSeq)
    }
  }

  private def divideIntoSubProblems(firstSeq: String, secondSeq: String) = {
    val firstSeqDivision = firstSeq.length / 2

    val leftSideDistances = nwScore(firstSeq.substring(0, firstSeqDivision), secondSeq)
    val rightSideDistances = nwScore(firstSeq.substring(firstSeqDivision).reverse, secondSeq.reverse).reverse

    val secondSeqDivision = findBestDivisionPoint(leftSideDistances, rightSideDistances)
    val reducedLestSide = hirschberg(firstSeq.substring(0, firstSeqDivision), secondSeq.substring(0, secondSeqDivision))
    val reducedRightSide = hirschberg(firstSeq.substring(firstSeqDivision), secondSeq.substring(secondSeqDivision))
    reducedLestSide + reducedRightSide
  }

  private def reduceInSimpleCase(firstSeq: String, secondSeq: String) = {
    val indexOfFirstEl = secondSeq.indexOf(firstSeq)
    if (indexOfFirstEl >= 0) {
      SUBSTITUTION_CHAR * indexOfFirstEl + firstSeq + SUBSTITUTION_CHAR * (secondSeq.length - indexOfFirstEl - 1)
    } else {
      SUBSTITUTION_CHAR * secondSeq.length
    }
  }

  private def findBestDivisionPoint(scoreL: Seq[Int], scoreR: Seq[Int]) = {
    (scoreL, scoreR).zipped.map(_ + _).zipWithIndex.maxBy(_._1)._2
  }

  //https://en.wikipedia.org/wiki/Needleman%E2%80%93Wunsch_algorithm
  private def nwScore(firstSeq: String, secondSeq: String): Seq[Int] = {
    val scoreMatrix = initScoreMatrix(firstSeq, secondSeq)
    for {
      i <- 1 to firstSeq.length
      j <- 1 to secondSeq.length
    } scoreMatrix(i)(j) = chooseMaxScore(i, j, firstSeq, secondSeq, scoreMatrix)

    for (j <- 0 to secondSeq.length) yield scoreMatrix(firstSeq.length)(j)
  }

  private def chooseMaxScore(
      i: Int,
      j: Int,
      firstSeq: String,
      secondSeq: String,
      scoreMatrix: Array[Array[Int]]
  ) = {
    val scoreSub = scoreMatrix(i - 1)(j - 1) + substitutionPenalty(firstSeq(i - 1), secondSeq(j - 1))
    val scoreDel = scoreMatrix(i - 1)(j) + deletionPenalty(firstSeq(i - 1))
    val scoreIns = scoreMatrix(i)(j - 1) + insertionPenalty(secondSeq(j - 1))
    max(max(scoreDel, scoreIns), scoreSub)
  }

  private def initScoreMatrix(firstSeq: String, secondSeq: String) = {
    val scoreMatrix = Array.ofDim[Int](firstSeq.length + 1, secondSeq.length + 1)
    for (i <- 1 to firstSeq.length) {
      scoreMatrix(i)(0) = scoreMatrix(i - 1)(0) + deletionPenalty(firstSeq(i - 1))
    }
    for (j <- 1 to secondSeq.length) {
      scoreMatrix(0)(j) = scoreMatrix(0)(j - 1) + insertionPenalty(secondSeq(j - 1))
    }
    scoreMatrix
  }

  /* TODO Penalty functions use a made up heuristic, will have to be validated against real data */
  private def deletionPenalty(char: Char) = -3

  private def insertionPenalty(char: Char) = -3

  private def substitutionPenalty(first: Char, second: Char) = {
    val premiumChars = Set('(', ')', SUBSTITUTION_CHAR)
    if (first == second && premiumChars.contains(first)) 3
    else if (first == second) 1
    else -1
  }


}
