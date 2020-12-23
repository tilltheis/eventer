package eventer

import zio.Task

import java.util.{Base64 => JBase64}

object Base64 {
  def encode(string: String): String = new String(JBase64.getEncoder.encode(string.getBytes("UTF-8")), "UTF-8")
  def decode(string: String): Task[String] =
    Task(new String(JBase64.getDecoder.decode(string.getBytes("UTF-8")), "UTF-8"))
}
