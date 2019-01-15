package acca.plus.smth.services

import java.time.Duration

import acca.plus.smth.models.Job
import javax.inject.{ Inject, Singleton }
import monix.eval.Task
import scalikejdbc._

@Singleton
class SmthRepository @Inject()(postgresAdapter: PostgresAdapter) extends Logging {

  def writeJobs(jobs: Seq[Job]): Task[Unit] = {
    val jobsBatch = jobs.map(j => Seq[Any](j.id, j.factDate, j.jobName, j.delayed, j.delayDuration.getSeconds, j.remarks.getOrElse(""), j.externalCause))

    postgresAdapter.executeBatch(
      sql"""
       INSERT INTO ???.??? (id, fact_date, job_name, delayed, delay_duration,
        remarks, external_cause)
       VALUES (?, ?, ?, ?, ?, ?, ?)
      """.batch(jobsBatch: _*)
    )
  }

  def getJobs(offset: Int = 0, limit: Int = 100): Task[List[Job]] = {
    postgresAdapter.executeQueryToList(
      sql"""
        SELECT *
        FROM ???.???
        ORDER BY fact_date
        LIMIT $limit OFFSET $offset
      """, createJobEntity
    )
  }

  def addJobRemark(id: Long, remark: String, isDelay: Boolean): Task[Unit] = {
    if (isDelay) {
      postgresAdapter.executeUpdate(
        sql"""
       UPDATE ???.???
       SET remarks = $remark
       WHERE id = $id """)
    } else {
      postgresAdapter.executeUpdate(
        sql"""
       UPDATE ???.???
       SET remarks = $remark
       WHERE id = $id """)
    }
  }

  private def createJobEntity(row: WrappedResultSet): Job = {
    Job(
      id = row.long("id"),
      factDate = row.string("fact_date"),
      jobName = row.string("job_name"),
      delayed = row.boolean("delayed"),
      delayDuration = Duration.ofSeconds(row.long("delay_duration")),
      remarks = row.stringOpt("remarks"),
      externalCause = row.boolean("external_cause")
    )
  }
}
