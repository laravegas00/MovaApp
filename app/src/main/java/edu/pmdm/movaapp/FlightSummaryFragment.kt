package edu.pmdm.movaapp

import FlightOffer
import Segment
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import edu.pmdm.movaapp.databinding.FragmentSummaryBinding
import edu.pmdm.movaapp.databinding.ItemSummaryFlightCardBinding
import edu.pmdm.movaapp.viewmodel.SharedViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.activity.OnBackPressedCallback
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.pmdm.movaapp.api.Retrofit
import edu.pmdm.movaapp.models.toMap
import edu.pmdm.movaapp.repository.TokenManager
import kotlinx.coroutines.launch


class FlightSummaryFragment : Fragment() {

    private var _binding: FragmentSummaryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SharedViewModel by activityViewModels()

    private lateinit var fromFullName: String
    private lateinit var toFullName: String

    private var airlineMap: Map<String, String> = emptyMap()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigate(R.id.flightFragment)
                }
            })

        val args = FlightSummaryFragmentArgs.fromBundle(requireArguments())
        fromFullName = args.fromFullName
        toFullName = args.toFullName
        val isReturnTrip = args.isReturnTrip
        if (!isReturnTrip) {
            binding.imageView2.visibility = View.GONE
            binding.txtReturn.visibility = View.GONE
            binding.tvSchedule2.visibility = View.GONE
            binding.returnContainer.visibility = View.GONE
        }

        viewLifecycleOwner.lifecycleScope.launch {
            loadAirlines()
            viewModel.selectedOutboundFlight.value?.let {
                loadAirportNamesFromSegments(it.itineraries[0].segments)
                bindFlight(binding.outboundContainer, it, isReturn = false)
            }

            viewModel.selectedReturnFlight.value?.let {
                loadAirportNamesFromSegments(it.itineraries[0].segments)
                bindFlight(binding.returnContainer, it, isReturn = true)
            }
        }



        // Total
        val price1 = viewModel.selectedOutboundFlight.value?.price?.total?.toDoubleOrNull() ?: 0.0
        val price2 = viewModel.selectedReturnFlight.value?.price?.total?.toDoubleOrNull() ?: 0.0
        val currency = viewModel.selectedOutboundFlight.value?.price?.currency ?: "EUR"
        binding.tvTotalPrice.text = String.format("%.2f %s", price1 + price2, currency)

        val select = binding.btnSelect
        select.setOnClickListener {
            val db = FirebaseFirestore.getInstance()
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@setOnClickListener

            val outbound = viewModel.selectedOutboundFlight.value
            val returnFlight = viewModel.selectedReturnFlight.value

            val reserva = hashMapOf(
                "timestamp" to System.currentTimeMillis(),
                "totalPrice" to binding.tvTotalPrice.text.toString(),
                "outboundFlight" to outbound?.toMap(isReturn = false)
            )

            if (returnFlight != null) {
                reserva["returnFlight"] = returnFlight.toMap(isReturn = true)
            }


            db.collection("users")
                .document(userId)
                .collection("reservation")
                .add(reserva)
                .addOnSuccessListener {
                    Toast.makeText(
                        requireContext(),
                        "Reservation saved correctly",
                        Toast.LENGTH_SHORT
                    ).show()
                    findNavController().navigate(R.id.flightFragment)
                }
                .addOnFailureListener {
                    Toast.makeText(
                        requireContext(),
                        "Error saving reservation",
                        Toast.LENGTH_SHORT
                    ).show()
                }

        }

    }

    private fun bindFlight(container: LinearLayout, flight: FlightOffer, isReturn: Boolean) {
        container.removeAllViews() // Limpia las tarjetas anteriores

        val itinerary = flight.itineraries[0]
        val segments = itinerary.segments

        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outFormatter = SimpleDateFormat("EEE, d MMM · HH:mm", Locale.getDefault())

        // Cabecera (info general)
        val scheduleType = if (segments.size > 1) "With scales" else "Direct"
        val duration = formatDuration(itinerary.duration)
        val header = "$scheduleType · $duration · Turist"

        if (!isReturn) {
            binding.tvSchedule.text = header
        } else {
            binding.tvSchedule2.text = header
        }

        for (segment in segments) {
            val cardBinding = ItemSummaryFlightCardBinding.inflate(layoutInflater)

            val departureTime = formatter.parse(segment.departure.at)
            val arrivalTime = formatter.parse(segment.arrival.at)

            cardBinding.tvSchedule.text = outFormatter.format(departureTime!!)
            cardBinding.tvSchedule2.text = outFormatter.format(arrivalTime!!)

            val airportNames = viewModel.airportNameMap
            cardBinding.tvRouteDeparture.text =
                airportNames[segment.departure.iataCode] ?: segment.departure.iataCode
            cardBinding.tvRouteArrival.text =
                airportNames[segment.arrival.iataCode] ?: segment.arrival.iataCode

            cardBinding.tvAirline.text = airlineMap[segment.carrierCode] ?: segment.carrierCode
            cardBinding.tvDuration.text = "Flight duration: ${formatDuration(segment.duration)}"

            container.addView(cardBinding.root)
        }
    }

    private suspend fun loadAirportNamesFromSegments(segments: List<Segment>) {
        val iataCodes = segments.flatMap { listOf(it.departure.iataCode, it.arrival.iataCode) }
            .toSet()

        for (iata in iataCodes) {
            if (!viewModel.airportNameMap.containsKey(iata)) {
                try {
                    val response = Retrofit.hotelService().getAirportSuggestions(iata)
                    val match = response.firstOrNull { it.iata_code == iata }

                    match?.let {
                        val fullName = "${it.name} (${it.countryName})"
                        viewModel.airportNameMap[iata] = fullName
                    }
                } catch (e: Exception) {
                    Log.e("Summary", "Error cargando nombre de aeropuerto: ${e.localizedMessage}")
                }
            }
        }
    }

    private suspend fun loadAirlines (){
        try {
            val token = TokenManager.getValidToken(requireContext()) ?: return
            val response = Retrofit.flightService().getAirlines("Bearer $token")
            airlineMap = response.data.associateBy(
                keySelector = { it.iataCode },
                valueTransform = { it.commonName }
            )
        } catch (e: Exception) {
            Log.e("Summary", "Error cargando aerolíneas: ${e.localizedMessage}")
        }
    }


    private fun formatDuration(duration: String): String {
        return duration.replace("PT", "")
            .replace("H", "h ")
            .replace("M", "min")
            .trim()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
