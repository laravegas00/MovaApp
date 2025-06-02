package edu.pmdm.movaapp.models

data class Reservation (
    val timestamp: Long = 0,
    val totalPrice: String = "",
    val outboundFlight: Map<String, Any> = emptyMap(),
    var returnFlight: Map<String, Any> = emptyMap()
)