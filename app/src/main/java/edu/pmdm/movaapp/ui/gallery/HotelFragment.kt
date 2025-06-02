package edu.pmdm.movaapp.ui.gallery

import CityHotelAdapter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import edu.pmdm.movaapp.databinding.FragmentGalleryBinding
import edu.pmdm.movaapp.repository.BookingRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class HotelFragment : Fragment() {

    private lateinit var binding: FragmentGalleryBinding
    private lateinit var repository: BookingRepository

    private var selectedDestId: String = ""
    private var selectedCityName: String = ""

    private var checkInDateApi = ""
    private var checkOutDateApi = ""
    private var checkInMillis: Long? = null
    private var guestCount = 1

    private var suppressTextChange = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        repository = BookingRepository()

        setupCityAutoComplete()

        binding.etCheckIn.setOnClickListener {
            showDatePicker { userDate, apiDate ->
                binding.etCheckIn.setText(userDate)
                checkInDateApi = apiDate

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val calendar = Calendar.getInstance()
                calendar.time = sdf.parse(apiDate)!!
                checkInMillis = calendar.timeInMillis
            }
        }

        binding.etCheckOut.setOnClickListener {
            if (checkInMillis != null) {
                showDatePicker(minDate = checkInMillis) { userDate, apiDate ->
                    binding.etCheckOut.setText(userDate)
                    checkOutDateApi = apiDate
                }
            } else {
                Toast.makeText(requireContext(), "Selecciona una fecha de entrada primero", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnIncrease.setOnClickListener {
            if (guestCount < 6) {
                guestCount++
                binding.txtGuestCount.text = guestCount.toString()
            }
        }

        binding.btnDecrease.setOnClickListener {
            if (guestCount > 1) {
                guestCount--
                binding.txtGuestCount.text = guestCount.toString()
            }
        }

        binding.btnSearch.setOnClickListener {
            if (selectedDestId.isBlank()) {
                Toast.makeText(requireContext(), "Selecciona una ciudad válida", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (checkInDateApi.isBlank() || checkOutDateApi.isBlank()) {
                Toast.makeText(requireContext(), "Selecciona fechas válidas", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Calcular noches entre fechas
            val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val checkIn = formatter.parse(checkInDateApi)
            val checkOut = formatter.parse(checkOutDateApi)

            val nights = if (checkIn != null && checkOut != null) {
                val diff = checkOut.time - checkIn.time
                (diff / (1000 * 60 * 60 * 24)).toInt()
            } else {
                1
            }

            val action = HotelFragmentDirections.actionHotelFragmentToHotelItemsFragment(
                destId = selectedDestId,
                cityName = selectedCityName,
                checkIn = checkInDateApi,
                checkOut = checkOutDateApi,
                adults = guestCount,
                nights = nights
            )
            findNavController().navigate(action)
        }

    }

    private fun setupCityAutoComplete() {
        val adapter = CityHotelAdapter { (displayText, destId) ->
            suppressTextChange = true
            binding.editTextHotel.setText(displayText)
            binding.editTextHotel.clearFocus()
            binding.recyclerHotel.visibility = View.GONE

            selectedDestId = destId
            selectedCityName = displayText
        }

        binding.recyclerHotel.adapter = adapter
        binding.recyclerHotel.layoutManager = LinearLayoutManager(requireContext())

        binding.editTextHotel.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) binding.recyclerHotel.visibility = View.GONE
        }

        binding.editTextHotel.doOnTextChanged { text, _, _, _ ->
            if (suppressTextChange) {
                suppressTextChange = false
                return@doOnTextChanged
            }

            if (!text.isNullOrBlank() && text.length >= 2 && binding.editTextHotel.hasFocus()) {
                lifecycleScope.launch {
                    try {
                        val response = repository.getCitySuggestions(text.toString())
                        val results = response.map {
                            Triple("${it.name} (${it.country})", it.dest_id, it.dest_type)
                        }
                        adapter.submitList(results)
                        binding.recyclerHotel.visibility = View.VISIBLE
                    } catch (e: Exception) {
                        Log.e("HOTEL_AUTOCOMPLETE", "Error: ${e.localizedMessage}")
                    }
                }
            }
        }
    }

    private fun showDatePicker(
        minDate: Long? = null,
        onDateSelected: (userFormat: String, apiFormat: String) -> Unit
    ) {
        val utc = TimeZone.getTimeZone("UTC")
        val calendarStart = Calendar.getInstance(utc)
        calendarStart.set(Calendar.HOUR_OF_DAY, 0)
        calendarStart.set(Calendar.MINUTE, 0)
        calendarStart.set(Calendar.SECOND, 0)
        calendarStart.set(Calendar.MILLISECOND, 0)

        val startDate = minDate ?: calendarStart.timeInMillis

        val constraintsBuilder = CalendarConstraints.Builder()
            .setStart(startDate)
            .setEnd(Calendar.getInstance(utc).apply { add(Calendar.YEAR, 1) }.timeInMillis)
            .setValidator(DateValidatorPointForward.from(startDate))

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select a date")
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = selection

            val userFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            onDateSelected(userFormat.format(calendar.time), apiFormat.format(calendar.time))
        }

        picker.show(parentFragmentManager, "DATE_PICKER")
    }


}