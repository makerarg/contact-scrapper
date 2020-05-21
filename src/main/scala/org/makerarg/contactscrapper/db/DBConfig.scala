package org.makerarg.contactscrapper.db

import cats.effect._
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie.hikari._
import doobie.util.transactor.Transactor
import doobie.util.io.IOActions

import scala.concurrent.ExecutionContext

trait DBConfig {
  implicit val ec: ExecutionContext
  implicit val cs: ContextShift[IO]

  private[db] val config: HikariConfig

  lazy val transactor: Transactor[IO] = {
    HikariTransactor.apply[IO](
      new HikariDataSource(config),
      ec,
      Blocker.liftExecutionContext(ec)
    )
  }
}

class RoofieDBConfig(implicit val ec: ExecutionContext, val cs: ContextShift[IO]) extends DBConfig {

  override val config: HikariConfig = new HikariConfig()
  config.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/roofie")
  config.setUsername("root")
  config.setPassword("")
}

class TestDBConfig(implicit val ec: ExecutionContext, val cs: ContextShift[IO]) extends DBConfig {

  override val config: HikariConfig = new HikariConfig()
  config.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/roofie-test")
  config.setUsername("root")
  config.setPassword("")
}
