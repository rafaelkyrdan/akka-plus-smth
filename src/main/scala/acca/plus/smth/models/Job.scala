package acca.plus.smth.models

import java.time.{ Duration, LocalTime }

case class Job(
    id: Long,
    factDate: String,
    jobName: String,
    delayed: Boolean,
    delayDuration: Duration, // build from seconds
    remarks: Option[String],
    externalCause: Boolean = false)
