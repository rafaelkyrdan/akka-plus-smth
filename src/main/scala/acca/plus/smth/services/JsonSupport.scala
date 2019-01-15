package acca.plus.smth.services

import java.time.{ Duration, Instant, LocalTime }

import acca.plus.smth.models.{ AddRemarkRequest, Job }
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.unmarshalling.{ FromRequestUnmarshaller, Unmarshaller }
import akka.stream.ActorMaterializer
import spray.json._

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

case class JsonSupport()(implicit ec: ExecutionContext, mat: ActorMaterializer) extends SprayJsonSupport with DefaultJsonProtocol {

  implicit object JobJsonFormat extends JsonFormat[Job] {
    def write(obj: Job) = JsObject(
      "id" -> JsNumber(obj.id),
      "factDate" -> JsString(obj.factDate),
      "jobName" -> JsString(obj.jobName),
      "delayed" -> JsBoolean(obj.delayed),
      "delayDuration" -> JsNumber(obj.delayDuration.getSeconds),
      "remarks" -> obj.remarks.map(JsString(_)).getOrElse(JsString("")),
      "externalCause" -> JsBoolean(obj.externalCause)
    )

    // until we need a read fill with dumb data
    def read(value: JsValue) = Job(0l, "", "", delayed = false, Duration.ZERO, None)
  }

  implicit object RequestJsonFormat extends JsonFormat[AddRemarkRequest] {
    def write(obj: AddRemarkRequest) = JsObject(
      "id" -> JsNumber(obj.id),
      "remarks" -> JsString(obj.remark),
      "isDelay" -> JsBoolean(obj.isDelay)
    )

    def read(value: JsValue) = {
      value.asJsObject.getFields("id", "remark", "isDelay") match {
        case Seq(JsNumber(id), JsString(remark), JsBoolean(isDelay)) =>
          AddRemarkRequest(id.toLong, remark, isDelay)
        case _ => throw new DeserializationException("AddRemarkRequest expected")

      }
    }
  }

  implicit val jsonToRequestUnmarshaller: FromRequestUnmarshaller[AddRemarkRequest] = {
    Unmarshaller(ec => httpreq => {
      implicit val ec = this.ec
      httpreq.entity.toStrict(2.seconds) map { e => e.data.utf8String } map ((jsonStr) => {
        RequestJsonFormat.read(jsonStr.parseJson)
      })
    })
  }
}
