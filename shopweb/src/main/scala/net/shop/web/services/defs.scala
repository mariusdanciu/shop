package net.shop.web.services

import net.shop.api.persistence.Persistence
import net.shift.common.Config
import net.shift.io.FileSystem
import net.shift.io.LocalFileSystem
import net.shift.template.TemplateFinder

trait ServiceDependencies {
  implicit val cfg: Config
  implicit val store: Persistence
  implicit val fs: FileSystem = LocalFileSystem
}