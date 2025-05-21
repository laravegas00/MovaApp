package edu.pmdm.movaapp.repository

import FlightOffer
import android.content.Context
import edu.pmdm.movaapp.api.Retrofit

class AmadeusRepository(
    private val context: Context,
    private val clientId: String,
    private val clientSecret: String
) {
    private val authService = Retrofit.authService()
    private val flightService = Retrofit.flightService()
    //private val hotelService = RetrofitClient.hotelService()

    private suspend fun getValidToken(): String? {
        var token = TokenManager.getValidToken(context)
        if (token == null) {
            try {
                val authResponse = authService.getAccessToken(clientId, clientSecret)
                token = authResponse.accessToken
                TokenManager.saveToken(context, token, authResponse.expiresIn)
            } catch (e: Exception) {
                e.printStackTrace()
                return null
            }
        }
        return token
    }

    suspend fun searchFlights(
        origin: String,
        destination: String,
        departureDate: String,
        returnDate: String?,
        adults: Int
    ): List<FlightOffer>? {
        var token = TokenManager.getValidToken(context)

        // Si no hay token válido, autenticamos
        if (token == null) {
            val response = authService.getAccessToken(
                clientId = clientId,
                clientSecret = clientSecret
            )
            token = response.accessToken
            TokenManager.saveToken(context, token, response.expiresIn)
        }

        val result = if (!returnDate.isNullOrBlank()) {
            flightService.searchFlights(
                authHeader = "Bearer $token",
                origin = origin,
                destination = destination,
                departureDate = departureDate,
                returnDate = returnDate,
                adults = adults
            )
        } else {
            flightService.searchFlightsWithoutReturn(
                authHeader = "Bearer $token",
                origin = origin,
                destination = destination,
                departureDate = departureDate,
                adults = adults
            )
        }

        return result.data
    }

//    // 🔹 HOTELS (cuando lo implementes)
//    suspend fun searchHotels(...): List<Hotel>? {
//        val token = getValidToken() ?: return null
//        return try {
//            hotelService.searchHotels("Bearer $token", ...)
//        } catch (e: Exception) {
//            e.printStackTrace()
//            null
//        }
//    }
}

