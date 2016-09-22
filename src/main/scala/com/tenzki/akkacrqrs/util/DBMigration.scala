package com.tenzki.akkacrqrs.util

import javax.inject.{Inject, Singleton}

import com.typesafe.config.Config
import org.flywaydb.core.Flyway

@Singleton
class DBMigration @Inject() (config: Config) {

  private val databaseConfig = config.getConfig("db")

  val url = databaseConfig.getString("url")
  val user = databaseConfig.getString("user")
  val password = databaseConfig.getString("password")

  private val flyway = new Flyway()
  flyway.setDataSource(url, user, password)

  def migrateDatabaseSchema = {
    flyway.migrate()
    this
  }

  def dropDatabase = {
    flyway.clean()
    this
  }

}
