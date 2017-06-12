package flaky.history

import java.io.File

import flaky.Unzip
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}

import scala.util.Success

class GitSpec extends WordSpec with Matchers with BeforeAndAfterAll with Unzip {
  private val zipped = new File("./src/test/resources", "gitrepo.zip")
  private val unzipDir = new File("target/")
  private val gitFolder = new File(unzipDir, "gitrepo")

  "Git " should {
    "list all changes" in {
      val git = Git(gitFolder)
      val expected: Seq[GitCommit] = List(
        ("549df19", 1495403128), //Commit 1
        ("a220598", 1495403138), //Commit 2
        ("d4fa72b", 1495403141), //Commit 3
        ("a9958e5", 1495403142), //Commit 4
        ("e5677bb", 1495403143), //Commit 5
        ("2939f3a", 1495403144), //Commit 6
        ("b55953a", 1495403146), //Commit 7
        ("9d74e32", 1495403147)  //Commit 8
      )
      .zipWithIndex
        .map(a => GitCommit(a._1._1, "user@email.com", s"Commit ${a._2 + 1}", a._1._2))
        .reverse
      git.history() shouldBe Success(expected)
    }
    "find commits list between 2 hashes" in {
      val git = Git(gitFolder)
      val commitsList = git.commitsList("d4fa72b", "2939f3a")
      //4,5,6
      commitsList shouldBe Success(List(
        GitCommit("2939f3a", "user@email.com", "Commit 6", 1495403144),
        GitCommit("e5677bb", "user@email.com", "Commit 5", 1495403143),
        GitCommit("a9958e5", "user@email.com", "Commit 4", 1495403142)
      ))
    }
    "find current commit" in {
      val git = Git(gitFolder)
      git.currentId() shouldBe Success("9d74e32")
    }
    "find git root folder" in {
      val gitInSubfolder = Git(new File(unzipDir, "gitrepo/.git/logs/"))
      gitInSubfolder.currentId() shouldBe Success("9d74e32")
    }

    "resolve remote repo" in {
      val git = Git(new File("."))
      git.remoteUrl().map(_.contains("github.com")) shouldBe Success(true)
    }
  }

  override protected def beforeAll(): Unit = {
    unzip(zipped, unzipDir)
  }
}
