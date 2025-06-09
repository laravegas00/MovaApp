package edu.pmdm.movaapp.viewmodel

import FlightOffer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import edu.pmdm.movaapp.models.Hotel

class SharedViewModel : ViewModel() {

    var userDataConfirmed: Boolean = false

    val airportNameMap = mutableMapOf<String, String>()

    private val _selectedOutboundFlight = MutableLiveData<FlightOffer?>()
    val selectedOutboundFlight: MutableLiveData<FlightOffer?> = _selectedOutboundFlight

    private val _selectedReturnFlight = MutableLiveData<FlightOffer?>()
    val selectedReturnFlight: MutableLiveData<FlightOffer?> = _selectedReturnFlight

    val selectedHotel = MutableLiveData<Hotel?>()

    fun setOutboundFlight(flight: FlightOffer) {
        _selectedOutboundFlight.value = flight
    }

    fun setReturnFlight(flight: FlightOffer) {
        _selectedReturnFlight.value = flight
    }

    fun setHotel(hotel: Hotel) {
        selectedHotel.value = hotel
    }

}

