package acca.plus.smth.services

import acca.plus.smth.models.{ AddUserRequest, User, UserResponse }
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.unmarshalling.{ FromRequestUnmarshaller, Unmarshaller }
import akka.stream.ActorMaterializer
import spray.json._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

case class JsonSupport()(implicit ec: ExecutionContext, mat: ActorMaterializer) extends SprayJsonSupport with DefaultJsonProtocol {

  implicit object UserJsonFormat extends JsonFormat[User] {
    def write(obj: User) = JsObject(
      "email" -> JsString(obj.email),
      "id" -> JsNumber(obj.id),
      "firstName" -> JsString(obj.firstName),
      "lastName" -> JsString(obj.lastName)
    )

    // until we need a read fill with dumb data
    def read(value: JsValue) = User("", 0, "", "")
  }

  implicit object AddRequestJsonFormat extends JsonFormat[AddUserRequest] {
    def write(obj: AddUserRequest) = JsObject(
      "id" -> JsNumber(obj.id),
      "email" -> JsString(obj.email)
    )

    def read(value: JsValue) = {
      value.asJsObject.getFields("id", "email") match {
        case Seq(JsNumber(id), JsString(email)) =>
          AddUserRequest(id.toInt, email)
        case _ => throw new DeserializationException("AddUserRequest expected")

      }
    }
  }

  implicit val jsonToRequestUnmarshaller: FromRequestUnmarshaller[AddUserRequest] = {
    Unmarshaller(ec => httpreq => {
      implicit val ec = this.ec
      httpreq.entity.toStrict(2.seconds) map { e => e.data.utf8String } map ((jsonStr) => {
        AddRequestJsonFormat.read(jsonStr.parseJson)
      })
    })
  }

  implicit object UserResponseJsonFormat extends JsonFormat[UserResponse] {
    def write(obj: UserResponse) = JsObject(
      "id" -> JsNumber(obj.id),
      "first_name" -> JsString(obj.firstName),
      "last_name" -> JsString(obj.lastName)
    )

    def read(value: JsValue): UserResponse = {
      value.asJsObject.getFields("data") match {
        case Seq(JsObject(fields)) => {
          (fields.get("id"), fields.get("first_name"), fields.get("last_name")) match {
            case (Some(JsNumber(id)), Some(JsString(firstName)), Some(JsString(lastName))) => UserResponse(id.toInt, firstName, lastName)
            case _ => throw new DeserializationException("AddUserRequest expected")
          }
        }
        case _ => throw new DeserializationException("AddUserRequest expected")
      }
    }
  }
}
