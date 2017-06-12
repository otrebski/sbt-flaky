package flaky.history

import scala.util.matching.Regex

object GitRepo {

  private val ssh: Regex = "(\\w+)@([\\w\\.]*):(.*)/(.*)\\.git".r
  private val http: Regex = "(http|https)://([^@]*@)?(.*)/(.*)/(.*)\\.git".r

  def fromUrl(url: String): Option[GitRepo] = {
    url match {
      case ssh(_, host, owner, name) => Some(GitRepo("https", host, owner , name))
      case http(protocol, _, host, owner, name) => Some(GitRepo(protocol,host,owner, name))
      case _ => None
    }
  }

}


case class GitRepo(protocol: String, host: String, owner: String, name: String) {

  def commitLink(gitCommit: GitCommit): String = s"$protocol://$host/$owner/$name/commit/${gitCommit.id}"

  def diffLink(older: GitCommit, newer: GitCommit): String = diffLink(older.id,newer.id)

  def diffLink(older: String, newer: String): String = s"$protocol://$host/$owner/$name/compare/$older...$newer"

}


