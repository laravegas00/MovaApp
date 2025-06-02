package edu.pmdm.movaapp.repository

import edu.pmdm.movaapp.api.Retrofit
import edu.pmdm.movaapp.models.CityResponse
import edu.pmdm.movaapp.models.Hotel

class BookingRepository {
    private val service = Retrofit.hotelService()

    suspend fun getCitySuggestions(name: String): List<CityResponse> {
        return service.getCitySuggestions(name)
    }

    suspend fun searchHotels(destId: String, checkIn: String, checkOut: String, adults: Int): List<Hotel> {
        return service.searchHotels(destId, checkIn, checkOut, adults).result
    }

    suspend fun loadFacilityMap(hotelId: Int): Map<Int, String> {
        return service.getHotelFacilities(hotelId)
            .associateBy({ it.facilityId }, { it.facilityName })
    }

}
