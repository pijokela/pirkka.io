package store

import redis._

import scala.concurrent.ExecutionContext.Implicits.global

class Store {
  implicit val akkaSystem = akka.actor.ActorSystem()
  val redis = RedisClient()
  
  redis.keys("temperature:2017-09")
}