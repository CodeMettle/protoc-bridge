package protocbridge.frontend

import java.io.{File, FileInputStream, FileOutputStream}

import protocbridge.ProtocCodeGenerator

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.sys.process._

/** PluginFrontend for Unix-like systems (Linux, Mac, etc)
  *
  * Creates a pair of named pipes for input/output and a shell script that communicates with them.
  */
object PosixPluginFrontend extends PluginFrontend {
  case class InternalState(inputPipe: File, outputPipe: File, shellScript: File)

  override def prepare(plugin: ProtocCodeGenerator): (File, InternalState) = {
    val inputPipe = createPipe()
    val outputPipe = createPipe()
    val sh = createShellScript(inputPipe, outputPipe)

    Future {
      val fsin = new FileInputStream(inputPipe)
      val response = PluginFrontend.runWithInputStream(plugin, fsin)
      fsin.close()

      val fsout = new FileOutputStream(outputPipe)
      fsout.write(response.toByteArray)
      fsout.close()
    }
    (sh, InternalState(inputPipe, outputPipe, sh))
  }

  override def cleanup(state: InternalState): Unit = {
    state.inputPipe.delete()
    state.outputPipe.delete()
    state.shellScript.delete()
  }

  private def createPipe(): File = {
    val pipeName = File.createTempFile("protopipe-", ".pipe")
    pipeName.delete()
    Seq("mkfifo", "-m", "600", pipeName.getAbsolutePath).!!
    pipeName
  }

  private def createShellScript(inputPipe: File, outputPipe: File): File = {
    val scriptName = PluginFrontend.createTempFile("",
      s"""|#!/usr/bin/env sh
          |set -e
          |cat /dev/stdin > "$inputPipe"
          |cat "$outputPipe"
      """.stripMargin)
    Seq("chmod", "+x", scriptName.getAbsolutePath).!!
    scriptName
  }
}
