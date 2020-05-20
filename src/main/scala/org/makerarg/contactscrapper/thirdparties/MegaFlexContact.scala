package org.makerarg.contactscrapper.thirdparties

case class MegaFlexContact(
  id: String,
  nombre: String,
  direccion: String,
  contacto: String,
  telefono: String,
  mail: String,
  web: String,
  distance: String,
  lat: String,
  lng: String,
  products: String,
  categories: String,
  visible: String,
  id_vendedores: String
) extends RawContact
