package at.chaosfield.packupdate

import java.io.{File, FileInputStream}
import java.net.URL
import java.security.MessageDigest

import org.apache.commons.codec.digest.DigestUtils

class Component(val name: String, val version: String, val downloadUrl: Option[URL], val componentType: ComponentType, val hash: Option[String], val flags: Array[ComponentFlag]) {
  def toCSV = {
    Array(
      name,
      version,
      downloadUrl.toString,
      componentType.stringValue,
      hash.getOrElse(""),
      flags.map(_.internalName).mkString(";")
    ).mkString(",")
  }

  def verifyChecksum(config: MainConfig): Boolean = {
    val file = Util.fileForComponent(this, config.minecraftDir)
    componentType match {
      case ComponentType.Mod =>
        if (file.exists) {
          hash match {
            case Some(h) =>
              val d = DigestUtils.sha256Hex(new FileInputStream(file))
              //println(s"Hash of $name: $d <=> $h")
              d == h
            case _ =>
              println(s"Warning: Could not validate integrity of Component $name, because no hash was provided")
              true
          }
        } else {
          // Even if no checksum is provided, we know something is wrong if the file is missing
          false
        }
      case _ =>
        println(s"Warning: Checking integrity of component type ${componentType.stringValue} not supported yet, assuming working")
        true
    }
  }

  def neededOnSide(packSide: PackSide): Boolean = {
    packSide match {
      case PackSide.Client => !flags.contains(ComponentFlag.ServerOnly)
      case PackSide.Server => !flags.contains(ComponentFlag.ClientOnly)
    }
  }
}

object Component {
  def fromCSV(data: Array[String]) = {
    new Component(
      data(0), // name
      data(1), // version
      if (data(2).isEmpty) None else Some(new URL(data(2))), // downloadUrl
      ComponentType.fromString(data(3)).getOrElse(ComponentType.Unknown), // componentType
      data.lift(4).filter(_ != ""), // hash
      data.lift(5).map(_.split(';')).getOrElse(Array.empty[String]).flatMap(ComponentFlag.fromString) // flags
    )
  }
}
