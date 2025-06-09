package edu.pmdm.movaapp.models

import com.google.gson.annotations.SerializedName

data class BookingAirport(
    @SerializedName("short_code")val iata_code: String,
    val name: String,
    val countryName: String
)
