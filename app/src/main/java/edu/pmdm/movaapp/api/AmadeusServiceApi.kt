package edu.pmdm.movaapp.api

import AirlinesResponse
import FlightResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface AmadeusServiceApi {
    @GET("v2/shopping/flight-offers")
    suspend fun searchFlights(
        @Header("Authorization") authHeader: String,
        @Query("originLocationCode") origin: String,
        @Query("destinationLocationCode") destination: String,
        @Query("departureDate") departureDate: String,
        @Query("returnDate") returnDate: String?,
        @Query("adults") adults: Int,
        @Query("max") max: Int = 15
    ): FlightResponse

    @GET("v2/shopping/flight-offers")
    suspend fun searchFlightsWithoutReturn(
        @Header("Authorization") authHeader: String,
        @Query("originLocationCode") origin: String,
        @Query("destinationLocationCode") destination: String,
        @Query("departureDate") departureDate: String,
        @Query("adults") adults: Int,
        @Query("max") max: Int = 10
    ): FlightResponse


    @GET("v1/reference-data/airlines")
    suspend fun getAirlines(
        @Header("Authorization") authHeader: String
    ): AirlinesResponse


}