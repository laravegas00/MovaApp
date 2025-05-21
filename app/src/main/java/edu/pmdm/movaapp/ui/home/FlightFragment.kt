package edu.pmdm.movaapp.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
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
import edu.pmdm.movaapp.repository.TokenManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class FlightFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: AmadeusRepository //Repositorios de datos de Amadeus

    private var fromFullText: String = ""
    private var toFullText: String = ""

    private lateinit var fromAdapter: AirportAdapter
    private lateinit var toAdapter: AirportAdapter

    private var suppressTextChange = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        //Inicializamos el repositorio de datos
        repository = AmadeusRepository(
            context = requireContext(),
            clientId = "wX1aknDANkB8S3zFGEzG9Z9MDXKAPvFx",
            clientSecret = "aGnRKAAA1KIStJaS"
        )

        //Configuramos el buscador de aeropuertos
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


        //Configuramos el botón de intercambio
        binding.btnSwap.setOnClickListener {
            val from = binding.editTextFrom.text.toString()
            val to = binding.editTextTo.text.toString()

            binding.editTextFrom.setText(to)
            binding.editTextTo.setText(from)
        }

        //Configuramos las fechas de salida y de llegada
        var departureApiDate = ""
        var returnApiDate = ""
        var departureMillis: Long? = null

        //Configuramos el botón de salida
        binding.etDeparture.setOnClickListener {
            showDatePicker { userDate, apiDate ->
                binding.etDeparture.setText(userDate)
                departureApiDate = apiDate

                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) //Formato de fecha para la API Amadeus
                val calendar = Calendar.getInstance() //Creamos el calendario
                calendar.time = sdf.parse(apiDate)!! //Parseamos la fecha
                departureMillis = calendar.timeInMillis //Obtenemos los milisegundos de la fecha de salida para el botón de llegada
            }
        }

        //Configuramos el botón de llegada
        binding.etArrival.setOnClickListener {
            if (departureMillis != null) { //Si hay una fecha de salida, mostramos el date picker
                showDatePicker(minDate = departureMillis) { userDate, apiDate -> //Pasamos la fecha de salida como fecha mínima
                    binding.etArrival.setText(userDate) //Mostramos la fecha de llegada
                    returnApiDate = apiDate //Guardamos la fecha de llegada para la búsqueda
                }
            } else { //Si no hay fecha de salida, mostramos un mensaje
                Toast.makeText(
                    requireContext(),
                    "Select a departure date first",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        //Configuramos el botón de pasajeros
        var passengerCount = 1
        val minPassengers = 1
        val maxPassengers = 6

        //Configuramos el botón de incremento
        binding.btnIncrease.setOnClickListener {
            if (passengerCount < maxPassengers) {
                passengerCount++ //Incrementamos el número de pasajeros
                binding.txtPassengerCount.text = passengerCount.toString()
            }
        }

        //Configuramos el botón de decremento
        binding.btnDecrease.setOnClickListener {
            if (passengerCount > minPassengers) {
                passengerCount-- //Decrementamos el número de pasajeros
                binding.txtPassengerCount.text = passengerCount.toString()
            }
        }

        //Configuramos el botón de búsqueda
        binding.btnSearch.setOnClickListener {
            fromFullText = binding.editTextFrom.text.toString().trim()
            toFullText = binding.editTextTo.text.toString().trim()

            val fromIata = fromFullText.substringBefore(" -")
            val toIata = toFullText.substringBefore(" -")

            val departure = departureApiDate
            val returnDate = returnApiDate.takeIf { it.isNotEmpty() }
            val passengers = binding.txtPassengerCount.text.toString().toIntOrNull() ?: 1

            val action = FlightFragmentDirections
                .actionHomeFragmentToFlightItemsFragment(
                    from = fromIata,
                    to = toIata,
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
                    val token = TokenManager.getValidToken(requireContext()) ?: return@launch
                    try {
                        val response = Retrofit.flightService()
                            .getAirportSuggestions("Bearer $token", text.toString())

                        val results = response.data.map {
                            "${it.iataCode} - ${it.name} (${it.address.cityName})"
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


    /**
     * Muestra un date picker para seleccionar una fecha.
     */
    private fun showDatePicker(
        minDate: Long? = null,
        onDateSelected: (userFormat: String, apiFormat: String) -> Unit
    ) {

        val utc = TimeZone.getTimeZone("UTC") //Creamos la zona horaria UTC
        val calendarStart = Calendar.getInstance(utc) //Creamos el calendario en la zona horaria UTC

        //Ponemos la hora a 00:00:00
        calendarStart.set(Calendar.HOUR_OF_DAY, 0)
        calendarStart.set(Calendar.MINUTE, 0)
        calendarStart.set(Calendar.SECOND, 0)
        calendarStart.set(Calendar.MILLISECOND, 0)

        val startDate = minDate ?: calendarStart.timeInMillis //Si hay una fecha mínima, la usamos, si no, la fecha actual

        //Configuración de los rangos de fechas
        val constraintsBuilder = CalendarConstraints.Builder()
            .setStart(startDate)
            .setEnd(Calendar.getInstance(utc).apply { add(Calendar.YEAR, 1) }.timeInMillis)
            .setValidator(DateValidatorPointForward.from(startDate))

        //Configuración del date picker
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select a date")
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        //Listener del date picker para obtener la fecha seleccionada y formatearla
        picker.addOnPositiveButtonClickListener { selection ->
            val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
            calendar.timeInMillis = selection

            val userFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()) //Formato de fecha para el usuario
            val apiFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) //Formato de fecha para la API Amadeus

            onDateSelected(userFormat.format(calendar.time), apiFormat.format(calendar.time))
        }

        picker.show(parentFragmentManager, "DATE_PICKER") //Mostramos el date picker con el fragment manager actual
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