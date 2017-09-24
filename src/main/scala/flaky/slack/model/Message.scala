package flaky.slack.model

case class Message(
                    text: Option[String] = None,
                    attachments: List[Attachment] = List.empty[Attachment]
                  )
