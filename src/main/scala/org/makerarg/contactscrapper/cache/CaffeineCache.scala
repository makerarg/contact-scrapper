package org.makerarg.contactscrapper.cache

import org.makerarg.contactscrapper.model.Contact

class CaffeineCache {

  import scalacache._
  import scalacache.caffeine._

  val contactCache: Cache[Contact] = CaffeineCache[Contact]

}
