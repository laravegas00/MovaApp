package edu.pmdm.movaapp.models

import com.google.gson.annotations.SerializedName

data class Facility(

    @SerializedName("hotelfacilitytype_id")val facilityId: Int,
    @SerializedName("facility_name")val facilityName: String
)
