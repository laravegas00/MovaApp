package edu.pmdm.movaapp.api

import FlightResponse
import edu.pmdm.movaapp.models.LocationResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface FlightService {
    @GET("v2/shopping/flight-offers")
    suspend fun searchFlights(
        @Header("Authorization") authHeader: String,
        @Query("originLocationCode") origin: String,
        @Query("destinationLocationCode") destination: String,
        @Query("departureDate") departureDate: String,
        @Query("returnDate") returnDate: String?,
        @Query("adults") adults: Int,
        @Query("max") max: Int = 10
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


    @GET("v1/reference-data/locations")
    suspend fun getAirportSuggestions(
        @Header("Authorization") authHeader: String,
        @Query("keyword") keyword: String,
        @Query("subType") subType: String = "AIRPORT"
    ): LocationResponse





}