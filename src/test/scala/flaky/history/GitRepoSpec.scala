package flaky.history

import org.scalatest.{Matchers, WordSpec}

class GitRepoSpec extends WordSpec with Matchers {

  "GitRepoSpec" should {

    "fromUrl read http links with user" in {
      val maybeRepo = GitRepo.fromUrl("http://otrebski@github.com/otrebski/sbt-flaky.git")
      maybeRepo shouldBe Some(GitRepo("http","github.com", "otrebski","sbt-flaky"))
    }
    "fromUrl read https links with user" in {
      val maybeRepo = GitRepo.fromUrl("https://otrebski@github.com/otrebski/sbt-flaky.git")
      maybeRepo shouldBe Some(GitRepo("https","github.com", "otrebski","sbt-flaky"))
    }

    "fromUrl read https links without user" in {
      val maybeRepo = GitRepo.fromUrl("https://github.com/otrebski/sbt-flaky.git")
      maybeRepo shouldBe Some(GitRepo("https","github.com", "otrebski","sbt-flaky"))
    }

    "fromUrl read ssh link" in {
      val maybeRepo = GitRepo.fromUrl("git@gitlab.mydomain.com:owner/repo.git")
      maybeRepo shouldBe Some(GitRepo("https","gitlab.mydomain.com", "owner","repo"))
    }



  }
}
