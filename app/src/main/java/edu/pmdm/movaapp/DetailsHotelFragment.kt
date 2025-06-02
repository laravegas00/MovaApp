package edu.pmdm.movaapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import edu.pmdm.movaapp.databinding.FragmentDetailsHotelBinding
import edu.pmdm.movaapp.databinding.FragmentHotelItemsBinding
import edu.pmdm.movaapp.models.Hotel
import edu.pmdm.movaapp.repository.BookingRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class DetailsHotelFragment : Fragment() {

    private var _binding: FragmentDetailsHotelBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: BookingRepository
    private lateinit var hotel: Hotel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailsHotelBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigate(R.id.hotelItemsFragment)
            }
        })

        val args = DetailsHotelFragmentArgs.fromBundle(requireArguments())
        hotel = args.hotel

        // Nombre, rating y localización
        binding.tvHotelName.text = hotel.hotel_name
        binding.tvHotelRating.text = "${hotel.review_score ?: "--"} ★"
        binding.tvHotelLocation.text = "${hotel.city ?: "Unknown"}, ${hotel.country_trans ?: ""}"

        // Imagen
        Glide.with(requireContext())
            .load(hotel.max_photo_url ?: hotel.main_photo_url)
            .into(binding.ivHotelImage)

        // Info de estancia
        val stayInfo = "${formatDate(args.checkIn)} → ${formatDate(args.checkOut)} \n ${args.nights} night(s), ${args.adults} guest(s)"
        binding.tvStayInfo.text = stayInfo


        // Cancelación
        binding.tvCancelation.text = if (hotel.is_free_cancellation == true)
            "Free cancellation"
        else
            "No free cancellation"

        binding.txtHotelPrice.text = "Price for ${args.nights} nights"

        val price = hotel.min_total_price ?: 0.0
        binding.tvHotelPrice.text = String.format("%.2f %s", price, hotel.currencycode ?: "")


        // Check-in / Check-out
        binding.tvCheckIn.text = hotel.checkin?.from ?: "From 14:00"
        binding.tvCheckOut.text = hotel.checkout?.until ?: "Until 11:00"

        // Facilities
        repository = BookingRepository()
        loadFacilities()
        // Distance
        binding.tvDistance.text = "Distance from centre: ${hotel.distance ?: "--"} km"



    }

    private fun loadFacilities() {
        lifecycleScope.launch {
            val facilityMap = try {
                repository.loadFacilityMap(hotel.hotel_id)
            } catch (e: Exception) {
                Log.e("HOTEL_DETAILS", "Error loading facility map: ${e.localizedMessage}")
                binding.tvFacilities.text = "Facilities not available"
                return@launch
            }

            val facilityIds = hotel.hotel_facilities
                ?.split(",")
                ?.mapNotNull { it.trim().toIntOrNull() }
                ?: emptyList()

            val allFacilities = facilityIds.mapNotNull { id ->
                facilityMap.getOrElse(id) { null }
            }

            if (allFacilities.isEmpty()) {
                binding.tvFacilities.text = "Facilities not available"
                return@launch
            }

            val maxToShow = 5
            val previewFacilities = allFacilities.take(maxToShow).joinToString(" • ")
            binding.tvFacilities.text = previewFacilities

            if (allFacilities.size > maxToShow) {
                binding.tvFacilities.append(" • • • • • • More facilities...")
                binding.tvFacilities.setOnClickListener {
                    showFacilitiesDialog(allFacilities)
                }
            }
        }
    }


    private fun showFacilitiesDialog(facilities: List<String>) {
        val builder = android.app.AlertDialog.Builder(requireContext())
        builder.setTitle("List of all the facilities available")
        builder.setItems(facilities.toTypedArray(), null)
        builder.setPositiveButton("Close", null)
        builder.show()
    }

    fun formatDate(date: String): String {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val parsedDate = inputFormat.parse(date)
        return outputFormat.format(parsedDate!!)
    }

}
