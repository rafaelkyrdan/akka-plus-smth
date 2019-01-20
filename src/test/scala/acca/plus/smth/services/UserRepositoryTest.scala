package acca.plus.smth.services

import acca.plus.smth.models.User
import acca.plus.smth.{ IntegrationTest, PostgresScope, TestApp }
import monix.execution.Scheduler.Implicits.global

import scala.concurrent.Await
import scala.concurrent.duration._

class UserRepositoryTest extends IntegrationTest {

  class Fixture extends PostgresScope {

    withApp {
      new TestApp(postgresConfig)
    }
    val app = new TestApp(postgresConfig)

    val userRepositoryRepository = app.injector.getInstance(classOf[UserRepository])
  }

  "UserRepositoryTest" should {
    "write/read user" in new Fixture {

      val users = Seq(User("rafael3@gmail.com", 3, "rafael", "k"), User("rafael2@gmail.com", 2, "rafael", "k"))
      userRepositoryRepository.writeUsers(users)
      val task = userRepositoryRepository.writeUsers(users)
      val future = task.runToFuture
      Await.result(future, 1.minutes)

      val taskList = userRepositoryRepository.getUsers(Seq(2, 3))
      val futureList = taskList.runToFuture
      val ls = Await.result(futureList, 1.minutes)

      ls.size ==== 2
    }

    "get user by email" in new Fixture {

      val task = userRepositoryRepository.getUsersByEmail("rafael4@gmail.com")
      val future = task.runToFuture
      val result = Await.result(future, 1.minutes)

      result.isEmpty ==== true

      val users = Seq(User("rafael4@gmail.com", 4, "rafael", "k"))
      userRepositoryRepository.writeUsers(users)
      val task1 = userRepositoryRepository.writeUsers(users)
      val future1 = task1.runToFuture
      Await.result(future1, 1.minutes)

      val task2 = userRepositoryRepository.getUsersByEmail("rafael4@gmail.com")
      val future2 = task2.runToFuture
      val result2 = Await.result(future2, 1.minutes)

      result2.nonEmpty ==== true
    }

    "delete user by email" in new Fixture {

      val users = Seq(User("rafael@gmail.com", 1, "rafael", "k"))
      userRepositoryRepository.writeUsers(users)
      val task = userRepositoryRepository.writeUsers(users)
      val future = task.runToFuture
      Await.result(future, 1.minutes)

      val task1 = userRepositoryRepository.deleteUser("rafael@gmail.com")
      val future1 = task1.runToFuture
      Await.result(future1, 1.minutes)

      val task2 = userRepositoryRepository.getUsersByEmail("rafael@gmail.com")
      val future2 = task2.runToFuture
      val result2 = Await.result(future2, 1.minutes)

      result2.isEmpty ==== true
    }
  }

}
