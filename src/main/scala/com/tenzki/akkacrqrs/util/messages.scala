package com.tenzki.akkacrqrs.util

case class Start(persistenceId: String)
case class StartReading(persistenceId: String, seqNo: Long)

case object Stop

case object Unstash
