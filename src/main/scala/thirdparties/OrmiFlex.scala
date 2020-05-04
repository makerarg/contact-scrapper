package thirdparties

object OrmiFlex {
  val HOST = "https://www.ormiflex.com/wp-admin/admin-ajax.php?action=store_search&lat=-34.6037618&lng=-58.38171499999999&max_results=200&search_radius=3000"
}

case class OrmiFlexContact(
  address: String,
  store: String,
  thumb: String,
  id: String,
  address2: String,
  city: String,
  state: String,
  zip: String,
  country: String,
  lat: String,
  lng: String,
  phone: String,
  email: String,
  hours: String,
  url: String
)
