package edu.pmdm.movaapp.models

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class HotelResponse(
    val result: List<Hotel>
)

@Parcelize
data class Hotel(
    val hotel_id: Int,
    val hotel_name: String,
    val address: String?,
    val city: String?,
    val country_trans: String?,
    val adults: Int?,
    val main_photo_url: String?,
    val max_photo_url: String?,
    val min_total_price: Double?,
    val currencycode: String?,
    val review_score: Double?,
    val review_nr: Int?,
    val checkin: CheckInOut?,
    val checkout: CheckInOut?,
    val is_free_cancellation: Boolean?,
    val is_breakfast_included: Boolean?,
    val distance: String?,
    val hotel_facilities: String? = null
) : Parcelable

@Parcelize
data class CheckInOut(
    val from: String?,
    val until: String?
) : Parcelable
