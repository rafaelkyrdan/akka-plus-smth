package acca.plus.smth.services

import acca.plus.smth.models.User
import javax.inject.{ Inject, Singleton }
import monix.eval.Task
import scalikejdbc._

class UserRepository @Inject()(postgresAdapter: PostgresAdapter) extends Logging {

  def writeUsers(users: Seq[User]): Task[Unit] = {
    val usersBatch = users.map(u => Seq[Any](u.email, u.id, u.firstName, u.lastName))

    postgresAdapter.executeBatch(
      sql"""
       INSERT INTO reqres.users (email, id, first_name, last_name)
       VALUES (?, ?, ?, ?)
      """.batch(usersBatch: _*)
    )
  }

  def getUsers(ids: Seq[Int]): Task[List[User]] = {
    postgresAdapter.executeQueryToList(
      sql"""
        SELECT *
        FROM reqres.users
        WHERE id IN ($ids)
      """, createUserEntity
    )
  }

  def getUsersByEmail(email: String): Task[Option[User]] = {

    postgresAdapter.getOne(
      sql"""
        SELECT *
        FROM reqres.users
        WHERE email = $email
      """, createUserEntity
    )
  }

  def deleteUser(email: String): Task[Unit] = {
    postgresAdapter.executeUpdate(
      sql"""
       DELETE FROM reqres.users
       WHERE email = $email """)
  }

  private def createUserEntity(row: WrappedResultSet): User = {
    User(
      email = row.string("email"),
      id = row.int("id"),
      firstName = row.string("first_name"),
      lastName = row.string("last_name")
    )
  }
}
