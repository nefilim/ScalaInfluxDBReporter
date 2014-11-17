package org.nefilim.influxdb

import java.net.InetSocketAddress

import akka.actor.{Stash, ActorLogging, ActorRef, Actor}
import akka.io.{Udp, IO}
import akka.util.ByteString

/**
 * Created by peter on 11/16/14.
 */
class UDPSender(remote: InetSocketAddress) extends Actor with ActorLogging with Stash {
  import context.system
  IO(Udp) ! Udp.SimpleSender

  def receive = {
    case Udp.SimpleSenderReady =>
      context.become(ready(sender()))
      unstashAll()

    case s: String =>
      stash()
  }

  def ready(send: ActorRef): Receive = {
    case json: String =>
      log.info("sending json {}", json)
      send ! Udp.Send(ByteString(json), remote)
  }
}

