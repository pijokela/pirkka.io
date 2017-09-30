package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._
import store.Store

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import jchart.ChartData
import org.joda.time.DateTime
import java.io.File
import scala.io.Source

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents, store: Store, chartData: ChartData) extends AbstractController(cc) {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }
  
  /**
   * A JSON document that lists interesting statistics from Redis to show that everything is working.
   */
  def status() = Action.async { implicit request: Request[AnyContent] =>
    val deviceIdsF = store.uniqueDeviceIds
    val keysF = store.countKeys
    
    for(deviceIds <- deviceIdsF;
        keys <- keysF) yield {
      Ok(Json.obj(
          "keys" -> keys,
          "deviceIds" -> Json.arr(deviceIds)))
    }
  }
  
  def measurements() = Action.async { implicit request: Request[AnyContent] =>
    val jsonOpt = request.body.asJson
    jsonOpt.map { json => 
      val array = json.as[JsArray]
      store.storeMeasurements(array).map { _ =>
        Ok(Json.obj())
      }
    }
    .getOrElse(Future.successful(BadRequest(Json.obj("reason" -> "No JSON array in body."))))
  }
  
  /**
   * POST maximum size is pretty limited. This allows reading test data in quickly.
   */
  def readMeasurementsFromDisk() = Action.async { implicit request: Request[AnyContent] =>
    val measurementSource = Source.fromFile("/tmp/measurements.json", "UTF-8")
    val array = Json.parse(measurementSource.mkString).as[JsArray]
    store.storeMeasurements(array).map { _ =>
      Ok(Json.obj("count" -> array.value.size))
    }
  }
  
  /**
   * The UI uses this endpoint to get measurement data
   */
  def data(`type`: Option[String], time: Option[String], grouping: Option[String]) = Action.async { request =>
    val validType = `type`.getOrElse("temperature")
    val validTime = time.getOrElse(TimeSlots.times.head)
    val validGrouping = chartData.selectGroupingForTime(validTime)
    Logger.info("Got time: " + time + " --> " + validTime)
    
    val now = DateTime.now()
    val (start, end) = TimeSlots.getStartAndEnd(validTime, now)
    Logger.info("Got start and end: " + start + " " + end)
    
    val resultsF = validType match {
      case "pressure" => store.listMeasurements("Pa", start, end)
      case _ => store.listMeasurements("milliC", start, end) // temperature
    }
    
    resultsF.map { temperatures => 
      Logger.info("Got data: " + temperatures.size)
      val groupedTemps = temperatures.groupBy { t => t.deviceId }.toList
      val data = validTime match {
        case "rolling30days" => chartData.dailyMinsAndMaxes(start, end, groupedTemps)
        case _ => chartData.fromMeasurements(start, end, groupedTemps, validGrouping)
      }
      Ok(data)
    }
  }
}
