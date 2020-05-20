package org.makerarg.contactscrapper.model

import org.makerarg.contactscrapper.thirdparties.{MegaFlexContact, OrmiFlexContact, RawContact}

sealed trait ContactSource {
  val id: String = this.getClass.getName
  val host: String
  def url(coordinates: Coordinates): String
}

case object OrmiFlex extends ContactSource {
  val host: String = "https://www.ormiflex.com/wp-admin/admin-ajax.php?action=store_search&max_results=200&search_radius=3000"
  override def url(coordinates: Coordinates): String = host + s"&lat=${coordinates.latitude.value}&lng=${coordinates.longitude.value}"
}

case object MegaFlex extends ContactSource {
  val host: String = "https://www.megaflex.com.ar/modules/aplicadores/api.php?a=getCloserClients&p=https%253A%2F%2Fwww.megaflex.com.ar%2Faplicadores%2F%2523&d=www.megaflex.com.ar&f=true&pr=0&ca=2"
  override def url(coordinates: Coordinates): String = host + s"&lat=${coordinates.latitude.value}&lng=${coordinates.longitude.value}"
}
