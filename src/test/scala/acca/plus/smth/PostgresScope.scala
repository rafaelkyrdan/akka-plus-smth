package acca.plus.smth

import java.util.Locale

import acca.plus.smth.services.{ Logging, PostgresAdapter, SqlDbAdapter }
import com.opentable.db.postgres.embedded.EmbeddedPostgres
import org.apache.commons.lang3.RandomStringUtils
import org.specs2.matcher.Scope
import scalikejdbc._
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.Await
import scala.concurrent.duration._

// Tests are using the embedded DB
trait PostgresScope extends Scope with Logging {
  val dbName = RandomStringUtils.randomAlphabetic(12).toLowerCase(Locale.ENGLISH)

  val instance = PostgresScope.instance


  private val postgresConnectionString = instance.getJdbcUrl("postgres", dbName)

  val connection = instance.getDatabase("postgres", "postgres")
  val createDbStatement = connection.getConnection.prepareStatement(String.format("CREATE DATABASE %s OWNER %s ENCODING = 'utf8'", dbName, "postgres"))
  createDbStatement.execute()

  val postgresConfig = Map(
    "db.postgres.url" -> postgresConnectionString,
    "db.postgres.user" -> "postgres",
    "db.postgres.password" -> "postgres",
    "doInitializeDb" -> "true",
    "doSeedDb" -> "false"
  )

  def count(postgresAdapter: PostgresAdapter, tableName: String): Int = {
    val name = SQLSyntax.createUnsafely(tableName)
    val task = postgresAdapter.executeQueryToList(
      sql"select count(*) from $name", row => row.int("count")
    )
    val future = task.runToFuture
    val result = Await.result(future, 3.minutes)
    result.head
  }

  logger.info(s"Created Postgres database: $postgresConnectionString")

}

object PostgresScope {
  private lazy val instance = EmbeddedPostgres.builder().start()

  sys.addShutdownHook {
    SqlDbAdapter.raiseShutdownFlag()
  }

  def initializeDb(postgresAdapter: PostgresAdapter): Unit = {
    val task = postgresAdapter.executeUpdate(
      sql"""
           CREATE SCHEMA IF NOT EXISTS reqres;

           CREATE TABLE IF NOT EXISTS reqres.users (
               email                varchar PRIMARY KEY,
               id                      bigint,
               first_name              varchar,
               last_name               varchar
           );


         """)

    val future = task.runToFuture
    Await.result(future, 3.minutes)
  }

  def seedDb(postgresAdapter: PostgresAdapter): Unit = {
    val task1 = postgresAdapter.executeUpdate(
      sql"""
           INSERT INTO reqres.users (email, id, first_name, last_name)
           VALUES ('rafael3@gmail.com', 3, 'rafael', 'k')
         """
    )

    val future1 = task1.runToFuture
    Await.result(future1, 3.minutes)

  }

}

