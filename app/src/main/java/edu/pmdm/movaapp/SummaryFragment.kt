package edu.pmdm.movaapp

import FlightOffer
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import edu.pmdm.movaapp.databinding.FragmentSummaryBinding
import edu.pmdm.movaapp.databinding.ItemSummaryFlightCardBinding
import edu.pmdm.movaapp.viewmodel.SharedViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.activity.OnBackPressedCallback


class SummaryFragment : Fragment() {

    private var _binding: FragmentSummaryBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SharedViewModel by activityViewModels()

    private lateinit var fromFullName: String
    private lateinit var toFullName: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigate(R.id.homeFragment)
            }
        })

        val args = SummaryFragmentArgs.fromBundle(requireArguments())
        fromFullName = args.fromFullName
        toFullName = args.toFullName

        viewModel.selectedOutboundFlight.value?.let {
            bindFlight(binding.outboundContainer, it, isReturn = false)
        }

        viewModel.selectedReturnFlight.value?.let {
            bindFlight(binding.returnContainer, it, isReturn = true)
        }


        // Total
        val price1 = viewModel.selectedOutboundFlight.value?.price?.total?.toDoubleOrNull() ?: 0.0
        val price2 = viewModel.selectedReturnFlight.value?.price?.total?.toDoubleOrNull() ?: 0.0
        val currency = viewModel.selectedOutboundFlight.value?.price?.currency ?: "EUR"
        binding.tvTotalPrice.text = String.format("%.2f %s", price1 + price2, currency)
    }

    private fun bindFlight(container: LinearLayout, flight: FlightOffer, isReturn: Boolean) {
        container.removeAllViews() // Limpia las tarjetas anteriores

        val itinerary = flight.itineraries[0]
        val segments = itinerary.segments

        val formatter = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val outFormatter = SimpleDateFormat("EEE, d MMM · HH:mm", Locale.getDefault())

        // Cabecera (info general)
        val scheduleType = if (segments.size > 1) "Con escalas" else "Directo"
        val duration = formatDuration(itinerary.duration)
        val header = "$scheduleType · $duration · Turista"

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
            cardBinding.tvRouteDeparture.text = segment.departure.iataCode
            cardBinding.tvRouteArrival.text = segment.arrival.iataCode
            cardBinding.tvAirline.text = segment.carrierCode
            cardBinding.tvDuration.text = "Duración del vuelo: ${formatDuration(segment.duration)}"

            container.addView(cardBinding.root)
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
