package edu.pmdm.movaapp.models

import FlightOffer

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
        "isDirect" to (segments.size == 1),
        "price" to price.total,
        "currency" to price.currency,
        "type" to if (isReturn) "return" else "outbound"
    )
}

