package org.makerarg.contactscrapper

import akka.actor.ActorSystem
import cats.effect.{ContextShift, IO}
import org.makerarg.contactscrapper.cache.CaffeineCache
import org.makerarg.contactscrapper.db.{ContactRepo, RoofieDBConfig}
import org.makerarg.contactscrapper.scrapper.{StreamingScrapper, SyncScrapper}

import scala.concurrent.ExecutionContext

object ScrapperApp extends App {
  implicit val actorSystem: ActorSystem = ActorSystem("ScrapSys")
  implicit val ec: ExecutionContext = ExecutionContext.global
  implicit val cs: ContextShift[IO] = IO.contextShift(ec)

  val cache = new CaffeineCache
  val dBConfig = new RoofieDBConfig
  val repo = new ContactRepo(dBConfig)

  val syncScrapper = new SyncScrapper(cache, repo)
  val streamingScrapper = new StreamingScrapper(cache, repo)

  (for {
    _ <- repo.safeWipe
    _ <- IO.pure(streamingScrapper.graph.run())
  } yield ()).unsafeRunSync()

}
