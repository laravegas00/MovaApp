package edu.pmdm.movaapp

import FlightOffer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.pmdm.movaapp.databinding.FragmentFlightHotelSummaryBinding
import edu.pmdm.movaapp.models.Hotel
import edu.pmdm.movaapp.models.toMap
import edu.pmdm.movaapp.viewmodel.SharedViewModel
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.Locale

class FlightHotelSummary : Fragment() {

    private var _binding: FragmentFlightHotelSummaryBinding? = null
    private val binding get() = _binding!!
    private val args: FlightHotelSummaryArgs by navArgs()
    private lateinit var checkInDate: String
    private lateinit var checkOutDate: String
    private var guests: Int = 1
    private val viewModel: SharedViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFlightHotelSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val outboundFlight = viewModel.selectedOutboundFlight.value
        val returnFlight = viewModel.selectedReturnFlight.value
        val hotel = viewModel.selectedHotel.value

        if (outboundFlight == null || hotel == null) {
            Toast.makeText(requireContext(), "There are empty fields", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        checkInDate = args.checkInDate
        checkOutDate = args.checkOutDate
        guests = args.adults

        binding.tvCheckInDate.text = formatDate(checkInDate)
        binding.tvCheckOutDate.text = formatDate(checkOutDate)
        binding.tvGuests.text = "$guests passengers · $guests guest"

        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

        val segIda = outboundFlight.itineraries[0].segments
        val depIda = formatter.parse(segIda.first().departure.at)
        val arrIda = formatter.parse(segIda.last().arrival.at)
        binding.tvResumenVuelo.text = "${segIda.first().departure.iataCode} ➝ ${segIda.last().arrival.iataCode}"
        binding.tvResumenHorasVuelo.text = "${timeFormatter.format(depIda!!)} - ${timeFormatter.format(arrIda!!)}"
        binding.tvResumenDuracionVuelo.text = "Duration: ${formatDuration(outboundFlight.itineraries[0].duration)}"
        binding.tvResumenPrecioVuelo.text = "Price: ${outboundFlight.price.total} ${outboundFlight.price.currency}"

        returnFlight?.let {
            val segVuelta = it.itineraries[0].segments
            val depVuelta = formatter.parse(segVuelta.first().departure.at)
            val arrVuelta = formatter.parse(segVuelta.last().arrival.at)
            binding.tvResumenVueloVuelta.text = "${segVuelta.first().departure.iataCode} ➝ ${segVuelta.last().arrival.iataCode}"
            binding.tvResumenHorasVueloVuelta.text = "${timeFormatter.format(depVuelta!!)} - ${timeFormatter.format(arrVuelta!!)}"
            binding.tvResumenDuracionVueloVuelta.text = "Duration: ${formatDuration(it.itineraries[0].duration)}"
            binding.tvResumenPrecioVueloVuelta.text = "Price: ${it.price.total} ${it.price.currency}"
        } ?: run {
            binding.vueltaContainer.visibility = View.GONE
        }

        binding.txtHotelName.text = hotel.hotel_name
        binding.txtHotelLocation.text = hotel.address
        binding.txtHotelPrice.text = "Hotel Price: %.2f ${hotel.currencycode}".format(hotel.min_total_price)

        Glide.with(requireContext())
            .load(hotel.max_photo_url ?: hotel.main_photo_url)
            .into(binding.imgHotel)

        binding.txtHotelName.text = hotel.hotel_name
        binding.txtHotelLocation.text = hotel.address
        binding.txtHotelPrice.text = "Hotel price: ${hotel.min_total_price} EUR"

        val total = (outboundFlight.price.total.toDoubleOrNull() ?: 0.0) +
                (returnFlight?.price?.total?.toDoubleOrNull() ?: 0.0) +
                (hotel.min_total_price)

        binding.txtTotal.text = "Total: %.2f ${outboundFlight.price.currency}".format(total)

        binding.btnConfirmBooking.setOnClickListener {
            saveCombinedReservation(outboundFlight, returnFlight, hotel, checkInDate, checkOutDate)
        }
    }

    private fun saveCombinedReservation(
        vuelo: FlightOffer,
        vueloVuelta: FlightOffer?,
        hotel: Hotel,
        checkIn: String,
        checkOut: String,
    ) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val total = (vuelo.price.total.toDoubleOrNull() ?: 0.0) +
                (vueloVuelta?.price?.total?.toDoubleOrNull() ?: 0.0) +
                (hotel.min_total_price)

        val reserva = hashMapOf(
            "timestamp" to System.currentTimeMillis(),
            "type" to "flight+hotel",
            "totalPrice" to "%.2f ${vuelo.price.currency}".format(total),
            "outboundFlight" to vuelo.toMap(),
            "returnFlight" to vueloVuelta?.toMap(),
            "hotel" to hotel.toMap(checkInDate, checkOutDate),
            "checkIn" to checkIn,
            "checkOut" to checkOut
        )

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

    fun formatDate(date: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val parsedDate = inputFormat.parse(date)
            outputFormat.format(parsedDate!!)
        } catch (e: Exception) {
            "-"
        }
    }

    fun formatDuration(isoDuration: String): String {
        return try {
            val duration = Duration.parse(isoDuration)
            val hours = duration.toHours()
            val minutes = duration.toMinutesPart()
            "${hours}h ${minutes}min"
        } catch (e: Exception) {
            "--"
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

