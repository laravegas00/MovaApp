package edu.pmdm.movaapp.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object Retrofit {
    private const val BASE_URL = "https://test.api.amadeus.com/"
    private const val BASE_URL_BOOKING = "https://booking-com.p.rapidapi.com/"

    fun authService(): AuthenticationService {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthenticationService::class.java)
    }

    fun flightService(): AmadeusServiceApi {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AmadeusServiceApi::class.java)
    }

    fun hotelService(): BookingServiceApi {
        return Retrofit.Builder()
            .baseUrl(BASE_URL_BOOKING)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(BookingServiceApi::class.java)
    }

}
