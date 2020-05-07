package cache

import model.Contact

class CaffeineCache {

  import scalacache._
  import scalacache.caffeine._

  val caffeineCache: Cache[Contact[_]] = CaffeineCache[Contact[_]]

}
