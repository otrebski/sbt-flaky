package flaky.slack.model

case class Attachment(
                       fallback: Option[String] = None,
                       callback_id: Option[String] = None,
                       color: Option[String] = None,
                       pretext: Option[String] = None,
                       author_name: Option[String] = None,
                       author_link: Option[String] = None,
                       author_icon: Option[String] = None,
                       title: Option[String] = None,
                       title_link: Option[String] = None,
                       text: Option[String] = None,
                       image_url: Option[String] = None,
                       thumb_url: Option[String] = None,
                       footer: Option[String] = None,
                       ts: Option[Long] = None,
                       mrkdwn_in: Seq[String] = Seq.empty
                     )

