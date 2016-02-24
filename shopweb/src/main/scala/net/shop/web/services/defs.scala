package net.shop.web.services

import net.shop.api.persistence.Persistence
import net.shift.common.Config


trait ServiceDependencies {
  implicit val cfg: Config
  implicit val store: Persistence
}