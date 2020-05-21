package org.makerarg.contactscrapper

import akka.actor.ActorSystem
import cats.effect.{ContextShift, IO}
import org.makerarg.contactscrapper.cache.CaffeineCache
import org.makerarg.contactscrapper.db.{ContactRepo, DBConfig, RoofieDBConfig}
import org.makerarg.contactscrapper.scrapper.{StreamingScrapper, SyncScrapper}

import scala.concurrent.ExecutionContext

object ScrapperApp extends App {
  implicit val actorSystem: ActorSystem = ActorSystem("ScrapSys")
  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)

  val cache = new CaffeineCache
  val repo = new ContactRepo(new RoofieDBConfig)

  val syncScrapper = new SyncScrapper(cache, repo)
  val streamingScrapper = new StreamingScrapper(cache, repo)

  streamingScrapper.graph.run()

}
