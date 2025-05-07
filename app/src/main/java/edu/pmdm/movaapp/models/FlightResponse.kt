data class FlightResponse(
    val data: List<FlightOffer>
)

data class FlightOffer(
    val type: String,
    val id: String,
    val source: String,
    val itineraries: List<Itinerary>,
    val price: Price
)

data class Itinerary(
    val duration: String,
    val segments: List<Segment>
)

data class Segment(
    val departure: LocationInfo,
    val arrival: LocationInfo,
    val carrierCode: String,
    val duration: String
)

data class LocationInfo(
    val iataCode: String,
    val at: String
)

data class Price(
    val currency: String,
    val total: String
)
