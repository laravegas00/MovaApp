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
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.pmdm.movaapp.api.Retrofit
import edu.pmdm.movaapp.databinding.FragmentSummaryBinding
import edu.pmdm.movaapp.databinding.ItemSummaryFlightCardBinding
import edu.pmdm.movaapp.models.toMap
import edu.pmdm.movaapp.repository.TokenManager
import edu.pmdm.movaapp.viewmodel.SharedViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class FlightSummaryFragment : Fragment() {

    private var _binding: FragmentSummaryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SharedViewModel by activityViewModels()
    private val args: FlightSummaryFragmentArgs by navArgs()

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

        viewModel.selectedHotel.value = null

        requireActivity().onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    findNavController().navigate(R.id.flightFragment)
                }
            })

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

        val price1 = viewModel.selectedOutboundFlight.value?.price?.total?.toDoubleOrNull() ?: 0.0
        val price2 = viewModel.selectedReturnFlight.value?.price?.total?.toDoubleOrNull() ?: 0.0
        val currency = viewModel.selectedOutboundFlight.value?.price?.currency ?: "EUR"
        binding.tvTotalPrice.text = String.format("%.2f %s", price1 + price2, currency)

        binding.btnSelect.setOnClickListener {
            checkUserDataBeforeReservation()
        }

    }

    private fun bindFlight(container: LinearLayout, flight: FlightOffer, isReturn: Boolean) {
        container.removeAllViews()

        val itinerary = flight.itineraries[0]
        val segments = itinerary.segments

        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outFormatter = SimpleDateFormat("EEE, d MMM · HH:mm", Locale.getDefault())

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

        val baggageInfo = flight.travelerPricings
            .flatMap { it.fareDetailsBySegment }
            .mapNotNull { it.includedCheckedBags }
            .map { bag ->
                when {
                    bag.quantity != null -> "${bag.quantity} checked bag(s) included"
                    bag.weight != null -> "${bag.weight} ${bag.weightUnit ?: "KG"} included"
                    else -> "No checked baggage included"
                }
            }.distinct()

        val cabinBaggageInfo = flight.travelerPricings
            .flatMap { it.fareDetailsBySegment }
            .mapNotNull { it.includedCabinBags }
            .map { bag ->
                when {
                    bag.quantity != null -> "${bag.quantity} cabin bag(s) included"
                    else -> "No cabin baggage info"
                }
            }.distinct()

        binding.tvCheckedBag.text = baggageInfo.joinToString("\n")
        binding.tvCabinBag.text = cabinBaggageInfo.joinToString("\n")

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

    private fun showHotelConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Do you want to book a hotel?")
            .setMessage("You can complete your reservation adding an hotel")
            .setIcon(R.drawable.cama)
            .setPositiveButton("Yes") { _, _ ->
                viewModel.selectedHotel.value = null
                findNavController().navigate(R.id.action_flightSummaryFragment_to_hotelFragment)
            }
            .setNegativeButton("No") { _, _ ->
                saveFlightReservation()
            }
            .setCancelable(false)
            .show()
    }

    private fun confirmReserve() {
        if (viewModel.selectedHotel.value == null) {
            showHotelConfirmationDialog()
        } else {
            saveCompleteReservation()
        }
    }

    private fun saveFlightReservation() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val outbound = viewModel.selectedOutboundFlight.value
        val returnFlight = viewModel.selectedReturnFlight.value

        val reserva = hashMapOf(
            "timestamp" to System.currentTimeMillis(),
            "type" to "flight",
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

    private fun saveCompleteReservation() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val reserva = mutableMapOf<String, Any>(
            "timestamp" to System.currentTimeMillis(),
            "type" to "flight+hotel"
        )

        viewModel.selectedOutboundFlight.value?.let {
            reserva["outboundFlight"] = it.toMap()
        }
        viewModel.selectedReturnFlight.value?.let {
            reserva["returnFlight"] = it.toMap()
        }
        viewModel.selectedHotel.value?.let {
            reserva["hotel"] = it.toMap(args.departureDate.toString(), args.returnDate.toString())
        }

        db.collection("users")
            .document(userId)
            .collection("reservation")
            .add(reserva)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Reservation saved correctly", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.flightFragment)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error saving reservation", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showConfirmationDialog(name: String, address: String, phone: String, email: String) {
        val mensaje = """
        Name: $name
        
        Address: $address
        
        Phone: $phone
        
        Email: $email
        
        
        Do you confirm this data?
    """.trimIndent()

        AlertDialog.Builder(requireContext())
            .setTitle("Confirm your data")
            .setMessage(mensaje)
            .setIcon(R.drawable.user)
            .setPositiveButton("Yes") { _, _ ->
                viewModel.userDataConfirmed = true
                confirmReserve()
            }
            .setNegativeButton("No") { _, _ ->
                findNavController().navigate(R.id.userDataFragment)
            }
            .setCancelable(false)
            .show()
    }

    private fun checkUserDataBeforeReservation() {

        if (viewModel.userDataConfirmed) {
            confirmReserve()
            return
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userDocRef = FirebaseFirestore.getInstance().collection("users").document(userId)

        userDocRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val name = document.getString("name") ?: "No name"
                val address = document.getString("address") ?: "No address"
                val phone = document.getString("phone") ?: "No phone"
                val email = document.getString("email") ?: "No email"

                showConfirmationDialog(name, address, phone, email)
            } else {
                findNavController().navigate(R.id.userDataFragment)
            }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "Error retrieving user data", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
