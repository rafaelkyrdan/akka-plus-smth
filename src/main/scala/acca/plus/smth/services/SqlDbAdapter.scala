package acca.plus.smth.services

import java.sql.ResultSet
import java.util.concurrent.atomic.AtomicBoolean

import com.typesafe.config.Config
import com.zaxxer.hikari.HikariDataSource
import javax.inject.{ Inject, Singleton }
import monix.eval.Task
import scalikejdbc.{ ConnectionPool, ConnectionPoolFactory, ConnectionPoolFactoryRepository, ConnectionPoolSettings, DataSourceConnectionPool, NamedDB, SQL, SQLBatch, SQLToList, WrappedResultSet }
import scalikejdbc.config.{ DBs, NoEnvPrefix, TypesafeConfig, TypesafeConfigReader }

trait SqlDbAdapter extends Logging {


  def connectionId: String

  def connectionName: String = connectionId

  var initialized = false

  def appConfig: Config

  trait SystemMonitoringTypesafeConfig extends TypesafeConfig {
    override val config: Config = appConfig
  }

  lazy val ensureDBSetup = {
    logger.info(s"Initializing SqlDbAdapter: $connectionId")
    ConnectionPoolFactoryRepository.add("hikari", HikariCPConnectionPoolFactory)

    val dbConfig = new DBs with TypesafeConfigReader with SystemMonitoringTypesafeConfig with NoEnvPrefix
    dbConfig.loadGlobalSettings()

    val dbSettings = dbConfig.readJDBCSettings(Symbol(connectionId))
    val dbCPSettings = dbConfig.readConnectionPoolSettings(Symbol(connectionId))
    logger.info(s"connecting to db ${dbSettings.url}")
    ConnectionPool.add(connectionName, dbSettings.url, dbSettings.user, dbSettings.password, dbCPSettings)

    initialized = true
  }

  def start(): Unit = {
    ensureDBSetup
  }

  def stop(): Unit = {
    if (initialized) {
      ConnectionPool.dataSource(connectionName).unwrap(classOf[HikariDataSource]).close()
    }
  }

  def executeQueryToList[T](query: SQL[_, _], rowMapper: WrappedResultSet => T, isUpdate: Boolean = false): Task[List[T]] = {
    ensureDBSetup

    Task {
      implicit val session = if (isUpdate) {
        NamedDB(connectionName).autoCommitSession()
      } else {
        NamedDB(connectionName).readOnlySession()
      }
      try {
        query.map(rowMapper).list.apply()
      } catch {
        case ex: Exception => {
          logQueryErrorMessage(ex, query)
          List.empty
        }
      } finally {
        session.close()
      }
    }
  }


  def executeQuery[T](query: SQL[_, _], isUpdate: Boolean = false): Task[Option[ResultSet]] = {
    ensureDBSetup

    val session = if (isUpdate) {
      NamedDB(connectionName).autoCommitSession()
    } else {
      NamedDB(connectionName).readOnlySession()
    }

    session.fetchSize(10000)

    Task {
      try {
        val resultSet = session.toStatementExecutor(query.statement, query.parameters).executeQuery()
        Option(resultSet)
      } catch {
        case ex: Exception => {
          logQueryErrorMessage(ex, query)
          None
        };
      } finally {
        session.close()
      }
    }

  }

  def executeBatch(batch: SQLBatch): Task[Unit] = {
    Task {
      NamedDB(connectionName) autoCommit { implicit session =>
        try {
          batch.apply()
          ()
        } catch {
          case ex: Exception => logBatchErrorMessage(ex, batch)
        }
      }
    }
  }

  def executeUpdate(update: SQL[_, _]): Task[Unit] = {
    ensureDBSetup

    Task {
      NamedDB(connectionName) autoCommit { implicit session =>
        try {
          update.update.apply()
          ()
        } catch {
          case ex: Exception => logQueryErrorMessage(ex, update)
        }

      }
    }
  }

  def doHealthCheck(): Task[Unit] = {
    ensureDBSetup
    Task {
      val connection = ConnectionPool.get(connectionName).borrow()
      connection.close()
    }
  }

  private def logQueryErrorMessage(exception: Throwable, query: SQL[_, _]): Unit = {
    logger.error(s"Query failed, ${query.statement}, error: ${exception.getMessage}")
  }

  private def logBatchErrorMessage(exception: Throwable, batch: SQLBatch): Unit = {
    logger.error(s"Query failed, ${batch.statement}", exception)
  }

}


object SqlDbAdapter {

  private val shutdownFlag = new AtomicBoolean(false)

  def raiseShutdownFlag(): Unit = {
    shutdownFlag.set(true)
  }
}

@Singleton
class PostgresAdapter @Inject()(val appConfig: Config) extends SqlDbAdapter {

  val connectionId = "postgres"

}

object HikariCPConnectionPoolFactory extends ConnectionPoolFactory {

  override def apply(
      url: String,
      user: String,
      password: String,
      settings: ConnectionPoolSettings = ConnectionPoolSettings()): DataSourceConnectionPool = {
    val ds = new HikariDataSource()
    ds.setDriverClassName(settings.driverName)
    ds.setJdbcUrl(url)
    ds.setUsername(user)
    ds.setPassword(password)
    ds.setConnectionTestQuery(settings.validationQuery)
    ds.setMinimumIdle(settings.initialSize)
    ds.setMaximumPoolSize(settings.maxSize)
    ds.setConnectionTimeout(settings.connectionTimeoutMillis)
    ds.setInitializationFailFast(true)
    new DataSourceConnectionPool(ds)
  }

}

