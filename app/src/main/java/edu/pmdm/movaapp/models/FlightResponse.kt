data class FlightResponse(
    val data: List<FlightOffer>
)

data class FlightOffer(
    val type: String,
    val id: String,
    val source: String,
    val itineraries: List<Itinerary>,
    val price: Price,
    val travelerPricings: List<TravelerPricing>
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

data class AirlinesResponse(
    val data: List<Airline>
)

data class Airline(
    val commonName: String,
    val iataCode: String
)

data class TravelerPricing(
    val travelerId: String,
    val fareOption: String,
    val travelerType: String,
    val price: Price,
    val fareDetailsBySegment: List<FareDetails>
)

data class FareDetails(
    val segmentId: String,
    val cabin: String,
    val fareBasis: String,
    val brandedFare: String?,
    val brandedFareLabel: String?,
    val `class`: String,
    val includedCheckedBags: Baggage?,
    val includedCabinBags: Baggage?, // <- NUEVO
    val amenities: List<Amenity>?    // <- NUEVO
)

data class Baggage(
    val weight: Int?,
    val weightUnit: String?,
    val quantity: Int?               // <- NUEVO
)

data class Amenity(
    val description: String?,
    val isChargeable: Boolean,
    val amenityType: String,
    val amenityProvider: AmenityProvider?
)

data class AmenityProvider(
    val name: String?
)


