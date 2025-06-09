package edu.pmdm.movaapp

import android.annotation.SuppressLint
import edu.pmdm.movaapp.models.Hotel
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import edu.pmdm.movaapp.adapter.HotelAdapter
import edu.pmdm.movaapp.databinding.FragmentHotelItemsBinding
import edu.pmdm.movaapp.repository.BookingRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class HotelItemsFragment : Fragment() {

    private var _binding: FragmentHotelItemsBinding? = null
    private val binding get() = _binding!!

    private lateinit var hotelAdapter: HotelAdapter
    private lateinit var repository: BookingRepository

    private var allHotels: List<Hotel> = listOf()

    private var destId: String = ""
    private var cityName: String = ""
    private var checkIn: String = ""
    private var checkOut: String = ""
    private var adults: Int = 1
    private var nights: Int = 1

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHotelItemsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("SuspiciousIndentation")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigate(R.id.hotelFragment)
            }
        })

        val args = HotelItemsFragmentArgs.fromBundle(requireArguments())
        destId = args.destId
        cityName = args.cityName
        checkIn = args.checkIn
        checkOut = args.checkOut
        adults = args.adults
        nights = args.nights

        binding.tvRouteSummary.text = "$cityName"
        binding.tvDatePassengerSummary.text = "${formatDate(checkIn)} ➝ ${formatDate(checkOut)} - $adults huésped(es)"

        binding.recyclerHotels.layoutManager = LinearLayoutManager(requireContext())

        Log.d("HOTEL_DEBUG", "destId=$destId, checkIn=$checkIn, checkOut=$checkOut, adults=$adults")

        hotelAdapter = HotelAdapter { selectedHotel ->
            val action = HotelItemsFragmentDirections
                .actionHotelItemsFragmentToHotelDetailsFragment(
                    hotel = selectedHotel,
                    checkIn = checkIn,
                    checkOut = checkOut,
                    nights = nights,
                    adults = adults
                )
            findNavController().navigate(action)
        }

        binding.recyclerHotels.adapter = hotelAdapter


    repository = BookingRepository()
        loadHotels()
    }

    private fun loadHotels() {
        binding.loadingContainer.visibility = View.VISIBLE
        binding.recyclerHotels.visibility = View.GONE
        binding.noResultsContainer.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val hotels = repository.searchHotels(destId, checkIn, checkOut, adults)

                withContext(Dispatchers.Main) {
                    binding.loadingContainer.visibility = View.GONE
                    if (hotels.isNotEmpty()) {
                        allHotels = hotels
                        hotelAdapter.setHotels(hotels)
                        setupFilters()
                        binding.recyclerHotels.visibility = View.VISIBLE
                    } else {
                        showNoResults("WOW! There are no hotels available for this date.")
                    }
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("HOTEL_ITEMS", "Error: ${e.localizedMessage}")
                    if (!isAdded || _binding == null) return@withContext
                    showNoResults("Ocurrió un error al buscar hoteles.")
                }
            }
        }
    }

    private fun showNoResults(message: String) {
        binding.recyclerHotels.visibility = View.GONE
        binding.noResultsContainer.visibility = View.VISIBLE
        binding.tvNoResultsMessage.text = message

        binding.noResultsContainer.postDelayed({
            if (isAdded && findNavController().currentDestination?.id == R.id.hotelItemsFragment) {
                findNavController().navigate(R.id.hotelFragment)
            }
        }, 2500)
    }

    fun formatDate(dateString: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val date = inputFormat.parse(dateString)
        return outputFormat.format(date!!)
    }

    private fun setupFilters() {
        binding.chipPriceDesc.setOnClickListener { applyFilters() }
        binding.chipDistance.setOnClickListener { applyFilters() }
        binding.chipPuntuation.setOnClickListener { applyFilters() }
    }

    fun Hotel.getPriceValue(): Double {
        return min_total_price ?: Double.MAX_VALUE
    }

    fun Hotel.getDistanceValue(): Double {
        return distance?.toDoubleOrNull() ?: Double.MAX_VALUE
    }

    fun Hotel.getReviewScoreValue(): Double {
        return review_score ?: Double.MAX_VALUE
    }

    private fun applyFilters(){
        var filtered = allHotels

        if (binding.chipPriceDesc.isChecked) {
            filtered = filtered.sortedBy { it.getPriceValue() }
        }

        if (binding.chipDistance.isChecked) {
            filtered = filtered.sortedBy { it.getDistanceValue() }
        }

        if (binding.chipPuntuation.isChecked) {
            filtered = filtered.sortedByDescending { it.getReviewScoreValue() }
        }

        hotelAdapter.setHotels(filtered)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
