package edu.pmdm.movaapp

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.pmdm.movaapp.databinding.FragmentDetailsHotelBinding
import edu.pmdm.movaapp.models.Hotel
import edu.pmdm.movaapp.models.toMap
import edu.pmdm.movaapp.repository.BookingRepository
import edu.pmdm.movaapp.viewmodel.SharedViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.getValue

class DetailsHotelFragment : Fragment() {

    private var _binding: FragmentDetailsHotelBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: BookingRepository
    private lateinit var hotel: Hotel

    private val args : DetailsHotelFragmentArgs by navArgs()

    private val viewModel: SharedViewModel by activityViewModels()

    private lateinit var googleMap: GoogleMap

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

        binding.tvHotelName.text = hotel.hotel_name
        binding.tvHotelRating.text = "${hotel.review_score ?: "--"} ★"
        binding.tvHotelLocation.text = "${hotel.city ?: "Unknown"}, ${hotel.country_trans ?: ""}"

        Glide.with(requireContext())
            .load(hotel.max_photo_url ?: hotel.main_photo_url)
            .into(binding.ivHotelImage)

        val stayInfo = "${formatDate(args.checkIn)} → ${formatDate(args.checkOut)} \n ${args.nights} night(s), ${args.adults} guest(s)"
        binding.tvStayInfo.text = stayInfo

        binding.tvCancelation.text = if (hotel.is_free_cancellable == 1)
            "Free cancellation"
        else
            "No free cancellation"

        binding.txtHotelPrice.text = "Price for ${args.nights} nights"

        val price = hotel.min_total_price ?: 0.0
        binding.tvHotelPrice.text = String.format("%.2f %s", price, "EUR")

        binding.tvCheckIn.text = hotel.checkin?.from ?: "From 14:00"
        binding.tvCheckOut.text = hotel.checkout?.until ?: "Until 11:00"

        repository = BookingRepository()
        loadFacilities()

        binding.tvDistance.text = "Distance from centre: ${hotel.distance ?: "--"} km"

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.mapContainer) as SupportMapFragment?
            ?: SupportMapFragment.newInstance().also {
                childFragmentManager.beginTransaction()
                    .replace(R.id.mapContainer, it).commit()
            }
        mapFragment.getMapAsync(this::onMapReady)

        binding.btnReserve.setOnClickListener {
            val selectedHotel = hotel
            viewModel.selectedHotel.value = selectedHotel

            if (!viewModel.userDataConfirmed) {
                checkAndConfirmUserData {
                    continueWithHotelReservation()
                }
            } else {
                continueWithHotelReservation()
            }
        }
    }

    fun onMapReady(gMap: GoogleMap) {
        googleMap = gMap
        hotel?.let {
            val lat = it.latitude ?: return
            val lng = it.longitude ?: return
            var pos = LatLng(lat, lng)
            googleMap.addMarker(MarkerOptions().position(pos).title(it.hotel_name))
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(pos, 15f))
        }
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

    private fun saveHotelReservationOnly(hotel: Hotel) {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        viewModel.selectedOutboundFlight.value = null
        viewModel.selectedReturnFlight.value = null

        val reservaHotel = hashMapOf(
            "timestamp" to System.currentTimeMillis(),
            "hotel" to hotel.toMap(args.checkIn, args.checkOut), // Usa tu función de extensión para mapear el hotel
            "type" to "hotel",
            "totalPrice" to binding.tvHotelPrice.text.toString(),
        )

        db.collection("users")
            .document(userId)
            .collection("reservation")
            .add(reservaHotel)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Hotel reservation saved correctly", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.hotelFragment)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error saving hotel reservation", Toast.LENGTH_SHORT).show()
            }
    }

    private fun continueWithHotelReservation() {
        if (viewModel.selectedOutboundFlight.value != null) {
            val action = DetailsHotelFragmentDirections
                .actionHotelDetailsFragmentToFinalSummaryFragment(
                    checkInDate = args.checkIn,
                    checkOutDate = args.checkOut,
                    adults = args.adults
                )
            findNavController().navigate(action)
        } else {
            saveHotelReservationOnly(hotel)
        }
    }

    private fun checkAndConfirmUserData(onConfirmed: () -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val userDocRef = FirebaseFirestore.getInstance().collection("users").document(userId)

        userDocRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val name = document.getString("name")?.let { encryptHelper.decrypt(it) } ?: ""
                val address = document.getString("address")?.let { encryptHelper.decrypt(it) } ?: ""
                val phone = document.getString("phone")?.let { encryptHelper.decrypt(it) } ?: ""
                val email = document.getString("email")?.let { encryptHelper.decrypt(it) } ?: ""

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
                    .setPositiveButton("Yes") { _, _ ->
                        viewModel.userDataConfirmed = true
                        onConfirmed()
                    }
                    .setNegativeButton("No") { _, _ ->
                        findNavController().navigate(R.id.userDataFragment)
                    }
                    .setCancelable(false)
                    .show()
            } else {
                findNavController().navigate(R.id.userDataFragment)
            }
        }
    }


}
