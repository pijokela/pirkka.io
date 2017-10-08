package jchart

import com.typesafe.config.Config
import play.api.Configuration
import com.typesafe.config.ConfigFactory
import play.api.libs.json._

object CreateTestConfig {
  
  def apply(json: JsObject): Configuration = {
    val config = ConfigFactory.parseString(json.toString())
    
    Configuration(config)
  }
  
}