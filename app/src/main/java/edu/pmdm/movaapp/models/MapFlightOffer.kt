package edu.pmdm.movaapp.models

import FlightOffer

fun FlightOffer.toMap(): Map<String, Any> {
        return mapOf(
            "from" to this.itineraries.firstOrNull()?.segments?.firstOrNull()?.departure?.iataCode.orEmpty(),
            "to" to this.itineraries.firstOrNull()?.segments?.lastOrNull()?.arrival?.iataCode.orEmpty(),
            "price" to this.price.total,
            "currency" to this.price.currency,
            "segments" to this.itineraries.flatMap { itinerary ->
                itinerary.segments.map { segment ->
                    mapOf(
                        "departure" to segment.departure.iataCode,
                        "arrival" to segment.arrival.iataCode,
                        "carrier" to segment.carrierCode,
                        "duration" to segment.duration
                    )
                }
            }
        )
    }
