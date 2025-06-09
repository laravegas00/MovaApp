package edu.pmdm.movaapp.ui.flights

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import edu.pmdm.movaapp.adapter.AirportAdapter
import edu.pmdm.movaapp.api.Retrofit
import edu.pmdm.movaapp.databinding.FragmentHomeBinding
import edu.pmdm.movaapp.repository.AmadeusRepository
import edu.pmdm.movaapp.viewmodel.SharedViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.getValue

class FlightFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: AmadeusRepository //Repositorios de datos de Amadeus

    private var fromFullText: String = ""
    private var toFullText: String = ""

    private val viewModel: SharedViewModel by activityViewModels()

    private var suppressTextChange = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        repository = AmadeusRepository(
            context = requireContext(),
            clientId = "wX1aknDANkB8S3zFGEzG9Z9MDXKAPvFx",
            clientSecret = "aGnRKAAA1KIStJaS"
        )

        binding.etArrival.visibility = View.GONE

        binding.rgTripType.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId == binding.rbOneWay.id) {
                binding.etArrival.visibility = View.GONE
                binding.etArrival.setText("")
            } else {
                binding.etArrival.visibility = View.VISIBLE
            }
        }

        setupAirportSearch(
            binding.editTextFrom,
            binding.recyclerAirportFrom
        ) { selectedText ->
            fromFullText = selectedText
        }

        setupAirportSearch(
            binding.editTextTo,
            binding.recyclerAirportTo
        ) { selectedText ->
            toFullText = selectedText
        }

        binding.btnSwap.setOnClickListener {
            val from = binding.editTextFrom.text.toString()
            val to = binding.editTextTo.text.toString()

            binding.editTextFrom.setText(to)
            binding.editTextTo.setText(from)
        }

        var departureApiDate = ""
        var returnApiDate = ""
        var departureMillis: Long? = null

        binding.etDeparture.setOnClickListener {
            showDatePicker { userDate, apiDate ->
                binding.etDeparture.setText(userDate)
                departureApiDate = apiDate

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val calendar = Calendar.getInstance()
                calendar.time = sdf.parse(apiDate)!!
                departureMillis = calendar.timeInMillis
            }
        }

        binding.etArrival.setOnClickListener {
            if (departureMillis != null) {
                showDatePicker(minDate = departureMillis) { userDate, apiDate ->
                    binding.etArrival.setText(userDate)
                    returnApiDate = apiDate
                }
            } else {
                Toast.makeText(
                    requireContext(),
                    "Select a departure date first",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        var passengerCount = 1
        val minPassengers = 1
        val maxPassengers = 6

        binding.btnIncrease.setOnClickListener {
            if (passengerCount < maxPassengers) {
                passengerCount++
                binding.txtPassengerCount.text = passengerCount.toString()
            }
        }

        binding.btnDecrease.setOnClickListener {
            if (passengerCount > minPassengers) {
                passengerCount--
                binding.txtPassengerCount.text = passengerCount.toString()
            }
        }

        binding.btnSearch.setOnClickListener {

            fromFullText = binding.editTextFrom.text.toString().trim()
            toFullText = binding.editTextTo.text.toString().trim()

            val departure = departureApiDate
            val returnDate = returnApiDate.takeIf { it.isNotEmpty() }
            val passengers = binding.txtPassengerCount.text.toString().toIntOrNull() ?: 1

            if (fromFullText.isBlank() || toFullText.isBlank()) {
                Toast.makeText(requireContext(), "Please select airports", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (departure.isBlank()) {
                Toast.makeText(requireContext(), "Please select a departure date", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val action = FlightFragmentDirections
                .actionHomeFragmentToFlightItemsFragment(
                    departureDate = departure,
                    returnDate = returnDate,
                    passengers = passengers,
                    fromFullName = fromFullText,
                    toFullName = toFullText
                )

            findNavController().navigate(action)
        }
        return root
    }

    private fun setupAirportSearch(
        inputField: AutoCompleteTextView,
        recyclerView: RecyclerView,
        onSelected: (String) -> Unit
    ) {
        val adapter = AirportAdapter { selected ->
            suppressTextChange = true
            inputField.setText(selected)
            inputField.clearFocus()
            recyclerView.visibility = View.GONE
            onSelected(selected)
        }

        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        inputField.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                recyclerView.visibility = View.GONE
            }
        }

        inputField.doOnTextChanged { text, _, _, _ ->
            if (suppressTextChange) {
                suppressTextChange = false
                return@doOnTextChanged
            }

            if (text != null && text.length >= 2 && inputField.hasFocus()) {
                lifecycleScope.launch {
                    try {
                        val response = Retrofit.hotelService().getAirportSuggestions(city = text.toString())
                        val results = response.map { item ->
                            val iata = item.iata_code
                            val fullName = "${item.name} (${item.countryName})"
                            viewModel.airportNameMap[iata] = fullName
                            "$iata - $fullName"
                        }

                        withContext(Dispatchers.Main) {
                            adapter.submitList(results)
                            recyclerView.visibility = View.VISIBLE
                        }

                    } catch (e: Exception) {
                        Log.e("AirportSearch", "Error: ${e.localizedMessage}")
                    }
                }
            } else {
                recyclerView.visibility = View.GONE
            }
        }
    }

    private fun showDatePicker(
        minDate: Long? = null,
        onDateSelected: (userFormat: String, apiFormat: String) -> Unit
    ) {

        val utc = TimeZone.getTimeZone("UTC")
        val calendarStart = Calendar.getInstance(utc)

        //Ponemos la hora a 00:00:00
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        binding.editTextFrom.text.clear()
        binding.editTextTo.text.clear()
        binding.etDeparture.text.clear()
        binding.etArrival.text.clear()
        binding.txtPassengerCount.text = "1"

        fromFullText = ""
        toFullText = ""

        binding.recyclerAirportFrom.visibility = View.GONE
        binding.recyclerAirportTo.visibility = View.GONE
    }

}