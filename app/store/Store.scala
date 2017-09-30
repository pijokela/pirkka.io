package store

import redis._

import javax.inject.Inject
import javax.inject.Singleton

import play.api.Configuration
import play.api.libs.json._

import scala.concurrent.Future
import scala.concurrent.Future._
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.DateTimeZone
import org.joda.time.DateTime
import scala.annotation.tailrec
import org.joda.time.LocalDate
import controllers.Measurement

@Singleton
class Store @Inject()(configuration: Configuration) {
  implicit val akkaSystem = akka.actor.ActorSystem()
  val redis = RedisClient()
  
  /**
    {
      "date": "2017-09-20T19:17:56Z",
      "deviceId": "28-0000072744d0",
      "milliC": 15250
	  },
   */
  def storeMeasurements(array: JsArray): Future[Unit] = {
    val devicesFuture = storeUniqueDeviceIds(array)
    val resultsFuture = sequence(
        array.value.map(json => storeMeasurement(json.as[JsObject]))
    )
    
    for(devices <- devicesFuture;
        results <- resultsFuture) yield {
      Unit
    }
  }
  
  def countMeasurements(): Future[Long] =
    redis.keys("*").map(_.size)
  
  def listMeasurements(unit: String, start: DateTime, end: DateTime): Future[List[Measurement]] = {
    uniqueDeviceIds.flatMap { deviceIds =>
      sequence(deviceIds.map { deviceId =>
        deviceMeasurements(unit, deviceId, start, end)
      }).map(_.flatten)
    }
  }
  
  /**
   * @return The device ids found from the array.
   */
  private def storeUniqueDeviceIds(array: JsArray): Future[List[String]] = {
    val deviceIds = array.value
      .map(json => (json \ "deviceId").as[String])
      .toSet
    sequence(deviceIds.map(deviceId => redis.sadd("deviceIds", deviceId)))
      .map(_ => deviceIds.toList)
  }
  
  /**
   * @return The device ids found from the array.
   */
  private def uniqueDeviceIds: Future[List[String]] =
    redis.smembers[String]("deviceIds").map(_.toList)
    
  private def deviceMeasurements(unit: String, deviceId: String, start: DateTime, end: DateTime): Future[List[Measurement]] = {
    val startMonth = start.toLocalDate().withDayOfMonth(1)
    val endMonth = end.toLocalDate().plusMonths(1).withDayOfMonth(1)
    val months = monthsTo(startMonth, endMonth)
    
    sequence(months.map(month => 
      deviceMeasurementsForMonth(unit, deviceId, month.getYear, month.getMonthOfYear)))
        .map(_.flatten
              .map(Measurement(_))
              .filter(m => m.date.isBefore(end) && m.date.isAfter(start)))
  }
  
  private def monthsTo(from: LocalDate, to: LocalDate): List[LocalDate] =
    if (from.isBefore(to)) { from :: monthsTo(from.plusMonths(1), to) } else Nil
  
  
  private def deviceMeasurementsForMonth(unit: String, deviceId: String, year: Int, month: Int): Future[List[JsObject]] =
    redis.hvals[String](s"measurementDetails-$unit-$deviceId-$year-$month")
      .map(_.map(s => Json.parse(s).as[JsObject]).toList)
  
  private def storeMeasurement(json: JsObject): Future[Boolean] = {
    val suomiZone = DateTimeZone.forID("Europe/Helsinki")
    val dtParser = ISODateTimeFormat.dateTimeNoMillis()
    
    val deviceId = (json \ "deviceId").as[String]
    val unit = ("Pa" :: "milliC" :: Nil).find(u => json.keys.contains(u)).getOrElse("unknown")
    val date = dtParser.parseDateTime((json \ "date").as[String])
                 .withZone(suomiZone)
    redis.hset(createKey(unit, deviceId, date),
        dtParser.print(date),
        json.toString())
  }
  
  private def createKey(unit: String, deviceId: String, date: DateTime): String = 
    s"measurementDetails-$unit-$deviceId-${date.getYear}-${date.getMonthOfYear}"
  
}