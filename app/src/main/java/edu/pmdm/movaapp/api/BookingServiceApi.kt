package edu.pmdm.movaapp.api

import edu.pmdm.movaapp.models.BookingAirport
import edu.pmdm.movaapp.models.CityResponse
import edu.pmdm.movaapp.models.Facility
import edu.pmdm.movaapp.models.HotelResponse
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

interface BookingServiceApi {

    @Headers(
        "X-RapidAPI-Key:  9c04b1a854msh2056acaabc5ce24p142c9djsnbdd53d066b65", // Reemplaza con tu key
        "X-RapidAPI-Host: booking-com.p.rapidapi.com"
    )
    @GET("v1/flights/locations")
    suspend fun getAirportSuggestions(
        @Query("name") city: String,
        @Query("locale") locale: String = "en-us"
    ): List<BookingAirport>

    @Headers(
        "X-RapidAPI-Key:  9c04b1a854msh2056acaabc5ce24p142c9djsnbdd53d066b65", // Reemplaza con tu key
        "X-RapidAPI-Host: booking-com.p.rapidapi.com"
    )
    @GET("v1/hotels/locations")
    suspend fun getCitySuggestions(
        @Query("name") city: String,
        @Query("locale") locale: String = "en-us"
    ): List<CityResponse>

    @Headers(
        "X-RapidAPI-Key:  9c04b1a854msh2056acaabc5ce24p142c9djsnbdd53d066b65", // Reemplaza con tu key
        "X-RapidAPI-Host: booking-com.p.rapidapi.com"
    )
    @GET("v1/hotels/search")
    suspend fun searchHotels(
        @Query("dest_id") destId: String,
        @Query("checkin_date") checkIn: String,
        @Query("checkout_date") checkOut: String,
        @Query("adults_number") adults: Int,
        @Query("dest_type") destType: String = "city",
        @Query("room_number") room: Int = 1,
        @Query("order_by") orderBy: String = "popularity",
        @Query("filter_by_currency") currency: String = "EUR",
        @Query("locale") locale: String = "en-us",
        @Query("units") units: String = "metric",
    ): HotelResponse

    @Headers(
        "X-RapidAPI-Key:  9c04b1a854msh2056acaabc5ce24p142c9djsnbdd53d066b65", // Reemplaza con tu key
        "X-RapidAPI-Host: booking-com.p.rapidapi.com"
    )

    @GET("v1/hotels/facilities")
    suspend fun getHotelFacilities(
        @Query("hotel_id") hotelId: Int,
        @Query("locale") locale: String = "en-us"
    ): List<Facility>


}
