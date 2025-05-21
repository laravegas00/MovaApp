package edu.pmdm.movaapp.models

data class LocationResponse(
    val data: List<LocationItem>
)

data class LocationItem(
    val iataCode: String,
    val name: String,
    val subType: String,
    val address: LocationAddress
)

data class LocationAddress(
    val cityName: String?,
    val countryName: String?
)
