package controllers

import javax.inject._
import play.api._
import play.api.mvc._
import play.api.libs.json._

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

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
  
  def postMeasurements() = Action { implicit request: Request[AnyContent] =>
    val jsonOpt = request.body.asJson
    jsonOpt.map { json => 
      measurementsReceived = measurementsReceived ++ json.as[JsArray].value
      Ok(Json.obj())
    }
    .getOrElse(BadRequest(Json.obj("reason" -> "No JSON array in body.")))
  }
  
  var measurementsReceived: List[JsValue] = Nil
}
