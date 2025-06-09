package edu.pmdm.movaapp.models

data class Reservation (
    val type : String = "",
    val timestamp: Long = 0,
    val totalPrice: String = "",
    val outboundFlight: Map<String, Any>? = null,
    var returnFlight: Map<String, Any>? = null,
    var hotel: Map<String, Any>? = null,
    val checkIn: String? = null,
    val checkOut: String? = null
)