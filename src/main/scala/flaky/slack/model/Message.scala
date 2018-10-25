package flaky.slack.model

case class Message(
                    text: Option[String] = None,
                    attachments: Seq[Attachment] = Seq.empty[Attachment]
                  )
