package org.makerarg.contactscrapper.db

import cats.effect._
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie.hikari._
import doobie.util.transactor.Transactor
import doobie.util.io.IOActions

import scala.concurrent.ExecutionContext

class DBConfig(implicit val ec: ExecutionContext, cs: ContextShift[IO]) {

  private val config: HikariConfig = new HikariConfig()
  config.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/roofie")
  config.setUsername("root")
  config.setPassword("")

  val transactor: Transactor[IO] = {
    HikariTransactor.apply[IO](
      new HikariDataSource(config),
      ec,
      Blocker.liftExecutionContext(ec)
    )
  }
}
