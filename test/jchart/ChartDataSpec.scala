package jchart

import org.scalatestplus.play._
import org.scalatestplus.play.guice._
import play.api.test._
import play.api.test.Helpers._
import play.api.Configuration
import org.joda.time.DateTime
import controllers.Measurement
import controllers.MeasurementSource
import play.api.libs.json._

class ChartDataSpec extends PlaySpec {

  "ChartData" should {

    "create the correct list of data " in {
      val configuration = CreateTestConfig(Json.obj("deviceId" -> Json.obj("device123" -> Json.obj("label" -> "device123"))))
      val start = new DateTime(2017, 10, 7, 14, 0)
      val end = new DateTime(2017, 10, 8, 14, 0)
      val data = Map(
          "device123" -> List(
              Measurement(start.plusHours(1), "device123", 21000, MeasurementSource.TEMPERATURE),
              Measurement(start.plusHours(2), "device123", 22000, MeasurementSource.TEMPERATURE),
              Measurement(start.plusHours(5), "device123", 23004, MeasurementSource.TEMPERATURE)
          )
      ).toList
      
      val chartData = new ChartData(configuration)
      val jsObject = chartData.fromMeasurements(start, end, data, HourlyGrouping)
      
      println(jsObject.toString())
      val labels: JsArray = jsObject.value("labels").as[JsArray]
      val values: JsArray = (jsObject \\ "data").head.as[JsArray]
      assert(values.value.size == labels.value.size)
      assert(values.value.filter(_.as[JsNumber].value != 0).size == 3)
    }
  }
}
