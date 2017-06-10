package flaky.web

import flaky.history.{Git, GitCommit, HistoryReportDescription}
import flaky.web.HistoryHtmlReport.BuildDiff
import org.scalatest.{Matchers, WordSpec}

import scala.util.{Success, Try}

class HistoryHtmlReportSpec extends WordSpec with Matchers {


  val git = new Git {
    override def currentId(): Try[String] = Success("0000001")

    override def history(): Try[List[GitCommit]] =
      Success(
        (1 to 9)
          .map(i => GitCommit(s"000000$i", "a@a.pl", "msg", i))
          .toList
          .reverse
      )

  }

  "HistoryHtmlReportTest" should {

    "calculateBuildDiffs for empty list" in {
      val hrd: List[HistoryReportDescription] = List.empty
      val diffs: Seq[HistoryHtmlReport.BuildDiff] = HistoryHtmlReport.calculateBuildDiffs(hrd, git)

      diffs shouldBe List.empty[HistoryHtmlReport.BuildDiff]
    }

    "calculateBuildDiffs for single build with commit hash" in {
      val hrd: List[HistoryReportDescription] = List(
        HistoryReportDescription(1L, Some("0000001"))
      )
      val diffs: Seq[HistoryHtmlReport.BuildDiff] = HistoryHtmlReport.calculateBuildDiffs(hrd, git)

      diffs shouldBe List(BuildDiff(hrd.head, 1, List.empty[GitCommit]))
    }

    "calculateBuildDiffs for single build without commit hash" in {
      val hrd: List[HistoryReportDescription] = List(
        HistoryReportDescription(1L, None)
      )
      val diffs: Seq[HistoryHtmlReport.BuildDiff] = HistoryHtmlReport.calculateBuildDiffs(hrd, git)

      diffs shouldBe List(BuildDiff(hrd.head, 1, List.empty[GitCommit]))
    }

    "calculateBuildDiffs for builds with commit hashes" in {
      val hrd: List[HistoryReportDescription] = List(
        HistoryReportDescription(100L, Some("0000001")),
        HistoryReportDescription(200L, Some("0000004"))
      )
      val diffs: Seq[HistoryHtmlReport.BuildDiff] = HistoryHtmlReport.calculateBuildDiffs(hrd, git)

      diffs shouldBe List(
        BuildDiff(hrd(1), 2, List(
          GitCommit("0000004", "a@a.pl", "msg", 4),
          GitCommit("0000003", "a@a.pl", "msg", 3),
          GitCommit("0000002", "a@a.pl", "msg", 2)
        )),
        BuildDiff(hrd.head, 1, List.empty[GitCommit])
      )
    }

    "calculateBuildDiffs for builds with commit hashes added later" in {
      val hrd: List[HistoryReportDescription] = List(
        HistoryReportDescription(100L, None),
        HistoryReportDescription(200L, Some("0000001")),
        HistoryReportDescription(300L, Some("0000004"))
      )
      val diffs: Seq[HistoryHtmlReport.BuildDiff] = HistoryHtmlReport.calculateBuildDiffs(hrd, git)

      diffs shouldBe List(
        BuildDiff(hrd(2), 3, List(
          GitCommit("0000004", "a@a.pl", "msg", 4),
          GitCommit("0000003", "a@a.pl", "msg", 3),
          GitCommit("0000002", "a@a.pl", "msg", 2)
        )),
        BuildDiff(hrd(1), 2, List.empty[GitCommit]),
        BuildDiff(hrd.head, 1, List.empty[GitCommit])
      )
    }

    "calculateBuildDiffs for builds without commit hashes" in {
      val hrd: List[HistoryReportDescription] = List(
        HistoryReportDescription(100L, None),
        HistoryReportDescription(200L, None)
      )
      val diffs: Seq[HistoryHtmlReport.BuildDiff] = HistoryHtmlReport.calculateBuildDiffs(hrd, git)

      diffs shouldBe List(
        BuildDiff(hrd(1), 2, List.empty[GitCommit]),
        BuildDiff(hrd.head, 1, List.empty[GitCommit])
      )
    }

  }
}
