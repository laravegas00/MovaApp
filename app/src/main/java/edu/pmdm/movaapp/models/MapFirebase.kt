package edu.pmdm.movaapp.models

import FlightOffer
import edu.pmdm.movaapp.DetailsHotelFragmentArgs

fun FlightOffer.toMap(isReturn: Boolean = false): Map<String, Any?> {
    val itinerary = itineraries.firstOrNull()
    val segments = itinerary?.segments ?: return emptyMap()

    val first = segments.first()
    val last = segments.last()

    return mapOf(
        "origin" to first.departure.iataCode,
        "destination" to last.arrival.iataCode,
        "departureTime" to first.departure.at,
        "arrivalTime" to last.arrival.at,
        "duration" to itinerary.duration,
        "price" to "${price.total} ${price.currency}",
        "type" to if (isReturn) "return" else "outbound"
    )
}

fun Hotel.toMap(checkIn: String, checkOut: String): Map<String, Any> {
    return mapOf(
        "name" to this.hotel_name,
        "location" to "${this.address}, ${this.city}, ${this.country_trans}",
        "checkInDate" to checkIn,
        "checkOutDate" to checkOut,
        "price" to "${this.min_total_price} ${this.currencycode}"
    )
}


