package com.tenzki.akkacrqrs.cqrs

import java.util.UUID

import org.joda.time.DateTime

final case class User(userId:UUID, email: String, firstName: String, lastName: String, created: DateTime)
