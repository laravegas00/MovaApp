package edu.pmdm.movaapp.viewmodel

import FlightOffer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class SharedViewModel : ViewModel() {

    val airportNameMap = mutableMapOf<String, String>()

    private val _selectedOutboundFlight = MutableLiveData<FlightOffer?>()
    val selectedOutboundFlight: LiveData<FlightOffer?> = _selectedOutboundFlight

    private val _selectedReturnFlight = MutableLiveData<FlightOffer?>()
    val selectedReturnFlight: LiveData<FlightOffer?> = _selectedReturnFlight

    private val _fromFullName = MutableLiveData<String>()
    private val _toFullName = MutableLiveData<String>()

    fun setOutboundFlight(flight: FlightOffer) {
        _selectedOutboundFlight.value = flight
    }

    fun setReturnFlight(flight: FlightOffer) {
        _selectedReturnFlight.value = flight
    }

    fun setAirportFullNames(from: String, to: String) {
        _fromFullName.value = from
        _toFullName.value = to
    }

    fun getFromFullName(): LiveData<String> = _fromFullName
    fun getToFullName(): LiveData<String> = _toFullName

    fun clearAll() {
        _selectedOutboundFlight.value = null
        _selectedReturnFlight.value = null
        _fromFullName.value = ""
        _toFullName.value = ""
    }
}

