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
  
  def measurements() = Action { implicit request: Request[AnyContent] =>
    Ok(JsArray(measurementsReceived))
  }
  
  def postMeasurements() = Action.async { implicit request: Request[AnyContent] =>
    val jsonOpt = request.body.asJson
    jsonOpt.map { json => 
      val array = json.as[JsArray]
      store.storeMeasurements(array).map { _ =>
        measurementsReceived = measurementsReceived ++ array.value
        Ok(Json.obj())
      }
    }
    .getOrElse(Future.successful(BadRequest(Json.obj("reason" -> "No JSON array in body."))))
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
      case "pressure" => ??? // pressureDao.list(start, end).map(_.map(_.toMeasurement))
      case "temperature" => store.listMeasurements(start, end)
      case _ => store.listMeasurements(start, end)
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
  
  var measurementsReceived: List[JsValue] = Nil
}
