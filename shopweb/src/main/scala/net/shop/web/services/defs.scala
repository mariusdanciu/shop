package net.shop.web.services

import net.shift.common.Config
import net.shift.io.{FileSystem, LocalFileSystem}
import net.shop.persistence.Persistence

trait ServiceDependencies {
  implicit val cfg: Config
  implicit val store: Persistence
  implicit val fs: FileSystem = LocalFileSystem
}