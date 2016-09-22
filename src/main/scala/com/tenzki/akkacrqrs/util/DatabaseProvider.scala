package com.tenzki.akkacrqrs.util

import javax.inject.{Inject, Singleton}

import com.typesafe.config.Config
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}

@Singleton
class DatabaseProvider @Inject() (config: Config) {

  val databaseConfig = config.getConfig("db")

  val url = databaseConfig.getString("url")
  val user = databaseConfig.getString("user")
  val password = databaseConfig.getString("password")

  private val hikariConfig = new HikariConfig()
  hikariConfig.setJdbcUrl(url)
  hikariConfig.setUsername(user)
  hikariConfig.setPassword(password)

  private val dataSource = new HikariDataSource(hikariConfig)

  val driver = slick.driver.PostgresDriver
  import driver.api._
  val db = Database.forDataSource(dataSource)
  db.createSession()

}
