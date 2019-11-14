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
import java.nio.file.Paths
import image.RoundedImages
import java.time.LocalDateTime

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents, store: Store, chartData: ChartData, RestApiAction: RestAuth) extends AbstractController(cc) {

  val logger = Logger("home-controller")
  
  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def indexPage() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.index())
  }
  
  def hsRedirect() = Action { implicit request: Request[AnyContent] =>
    val today = LocalDateTime.now()
    val formatter = java.time.format.DateTimeFormatter.ofPattern("ddMMyyyy")
    TemporaryRedirect(s"https://www.hs.fi/paivanlehti/${formatter.format(today)}/")
  }
  
  def temperaturePage() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.temperature.index())
  }
  
  def iconFormPage() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.icon.index())
  }
  
  def createIconFromForm() = Action.async(parse.multipartFormData) { request =>
    request.body
      .file("picture")
      .map { picture =>
        // only get the last part of the filename
        // otherwise someone can send a path like ../../home/foo/bar.txt to write to other files on the system
        val filename    = Paths.get(picture.filename).getFileName
        val fileSize    = picture.fileSize
        val contentType = picture.contentType
        
        if (fileSize > 10000000)
          Future.successful(BadRequest("File must be under 10MB."))
  
        //picture.ref.copyTo(Paths.get(s"/tmp/picture/$filename"), replace = true)
        val ref = picture.ref
        
        val futureBytes = RoundedImages.multipartFormdataToIcon(picture.ref)
        futureBytes.map(Ok(_).as("image/png").withHeaders("Content-Disposition" -> "attachment; filename=\"rounded-icon.png\""))
      }
      .getOrElse {
        Future.successful(Redirect(routes.HomeController.iconFormPage).flashing("error" -> "Missing file"))
      }
  }
  
  /**
   * A JSON document that lists interesting statistics from Redis to show that everything is working.
   */
  def status() = Action.async { implicit request: Request[AnyContent] =>
    val deviceIdsF = store.uniqueDeviceIds
    val keysF = store.countKeys
    val latestDataF = store.latestResultsByDevice
    
    for(deviceIds <- deviceIdsF;
        keys <- keysF;
        latestData <- latestDataF) yield {
      val latestDataJson = latestData.map { case (deviceId, optDate) => 
        Json.obj(
            "deviceId" -> deviceId, 
            "date" -> optDate.map(_.toString)
        )
      }
      
      Ok(Json.obj(
          "keys" -> keys,
          "deviceIds" -> Json.arr(deviceIds),
          "latestData" -> Json.arr(latestDataJson)))
    }
  }
  
  def measurements(): Action[AnyContent] = RestApiAction.async { implicit request: Request[AnyContent] =>
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
    logger.info("Got time: " + time + " --> " + validTime)
    
    val now = DateTime.now()
    val (start, end) = TimeSlots.getStartAndEnd(validTime, now)
    logger.info("Got start and end: " + start + " " + end)
    
    val resultsF = validType match {
      case "pressure" => store.listMeasurements("Pa", start, end)
      case _ => store.listMeasurements("milliC", start, end) // temperature
    }
    
    resultsF.map { temperatures => 
      logger.info("Got data: " + temperatures.size)
      val groupedTemps = temperatures.groupBy { t => t.deviceId }.toList
      val data = validTime match {
        case "rolling30days" => chartData.dailyMinsAndMaxes(start, end, groupedTemps)
        case _ => chartData.fromMeasurements(start, end, groupedTemps, validGrouping)
      }
      Ok(data)
    }
  }
}

case class Logging[A](action: Action[A]) extends Action[A] {

  val logger = Logger("home-controller-logging")
  
  def apply(request: Request[A]): Future[Result] = {
    logger.info("Calling action")
    action(request)
  }

  override def parser = action.parser
  override def executionContext = action.executionContext
}