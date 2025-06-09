package edu.pmdm.movaapp

import FlightOffer
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import edu.pmdm.movaapp.adapter.FlightAdapter
import edu.pmdm.movaapp.databinding.FragmentFlightItemsBinding
import edu.pmdm.movaapp.repository.AmadeusRepository
import edu.pmdm.movaapp.viewmodel.SharedViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.format.DateTimeParseException

class FlightItemsFragment : Fragment() {

    private var _binding: FragmentFlightItemsBinding? = null
    private val binding get() = _binding!!

    private lateinit var flightAdapter: FlightAdapter
    private lateinit var repository: AmadeusRepository

    private var from: String = ""
    private var to: String = ""
    private var departureDate: String = ""
    private var returnDate: String? = null
    private var passengers: Int = 1
    private var fromFullName: String = ""
    private var toFullName: String = ""

    private var allFlights: List<FlightOffer> = listOf()

    private val sharedViewModel: SharedViewModel by activityViewModels()
    private var isReturn: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFlightItemsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                findNavController().navigate(R.id.flightFragment)
            }
        })

        // Recuperar argumentos del Safe Args
        arguments?.let {
            val args = FlightItemsFragmentArgs.fromBundle(it)
            fromFullName = args.fromFullName
            toFullName = args.toFullName
            departureDate = args.departureDate
            returnDate = args.returnDate
            passengers = args.passengers
            isReturn = args.isReturn


            from = fromFullName.substringBefore(" -").trim()
            to = toFullName.substringBefore(" -").trim()
        }

        // Mostrar resumen de parámetros
        isReturn = FlightItemsFragmentArgs.fromBundle(requireArguments()).isReturn
        val fechaInfo = if (!returnDate.isNullOrEmpty()) {
            "$departureDate ➝ $returnDate"
        } else {
            "$departureDate"
        }

        val date = SimpleDateFormat("dd/MM/yyyy").format(SimpleDateFormat("yyyy-MM-dd").parse(fechaInfo))

        // Mostrar resumen
        if (isReturn) {
            binding.tvTitle.text = "Return flight"
            binding.tvRouteSummary.text = "$toFullName ➝ $fromFullName"
            binding.tvSummary.text = "$toFullName ➝ $fromFullName"
            binding.tvDatePassengerSummary.text = "$departureDate - $passengers passenger (s)"
        } else {
            binding.tvTitle.text = "Outbound flight"
            binding.tvRouteSummary.text = "$fromFullName ➝ $toFullName"
            binding.tvSummary.text = "$fromFullName ➝ $toFullName"
            binding.tvDatePassengerSummary.text = "$date - $passengers passenger(s)"
        }

        // Setup RecyclerView
        flightAdapter = FlightAdapter()
        flightAdapter.setOnFlightSelectedListener { selectedFlight ->

            if (isReturn) {
                sharedViewModel.setReturnFlight(selectedFlight)

                val action = FlightItemsFragmentDirections
                    .actionFlightItemsFragmentToSummaryFragment(
                        departureDate = departureDate,
                        returnDate = returnDate,
                        fromFullName = fromFullName,
                        toFullName = toFullName,
                        isReturnTrip = true
                    )
                findNavController().navigate(action)
            } else {
                sharedViewModel.setOutboundFlight(selectedFlight)

                if (!returnDate.isNullOrBlank()) {
                    // Ir a buscar vuelos de vuelta
                    val action = FlightItemsFragmentDirections
                        .actionFlightItemsFragmentSelf(
                            departureDate = returnDate ?: "",
                            returnDate = null,
                            passengers = passengers,
                            isReturn = true,
                            fromFullName = toFullName,
                            toFullName = fromFullName
                        )
                    findNavController().navigate(action)
                } else {
                    val action = FlightItemsFragmentDirections
                        .actionFlightItemsFragmentToSummaryFragment(
                            departureDate = departureDate,
                            returnDate = null,
                            fromFullName = fromFullName,
                            toFullName = toFullName,
                            isReturnTrip = false
                        )

                    findNavController().navigate(action)
                }
            }
        }



        binding.recyclerFlights.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = flightAdapter
        }

        repository = AmadeusRepository(
            requireContext(),
            clientId = "wX1aknDANkB8S3zFGEzG9Z9MDXKAPvFx",
            clientSecret = "aGnRKAAA1KIStJaS"
        )

        loadFlights()
    }

    private fun loadFlights() {
        _binding?.loadingContainer?.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val flights = repository.searchFlights(from, to, departureDate, returnDate, passengers)

                withContext(Dispatchers.Main) {
                    if (!isAdded || _binding == null) return@withContext

                    if (!flights.isNullOrEmpty()) {
                        allFlights = flights
                        flightAdapter.setFlights(flights)
                        setupFilters()
                    } else {
                        binding.recyclerFlights.visibility = View.GONE
                        binding.noResultsContainer.visibility = View.VISIBLE

                        binding.noResultsContainer.postDelayed({
                            if (isAdded && findNavController().currentDestination?.id == R.id.flightItemsFragment) {
                                findNavController().navigate(R.id.flightFragment)
                            }
                        }, 2500)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded || _binding == null) return@withContext
                    Log.e("FlightItems", "Error al cargar vuelos: ${e.localizedMessage}")
                    Toast.makeText(requireContext(), "Error loading flights", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    if (!isAdded || _binding == null) return@withContext
                    _binding?.loadingContainer?.visibility = View.GONE
                }
            }
        }
    }

    private fun setupFilters() {
        binding.chipPriceDesc.setOnClickListener { applyFilters() }
        binding.chipDirect.setOnClickListener { applyFilters() }
        binding.chipScales.setOnClickListener { applyFilters() }
        binding.chipShortDuration.setOnClickListener { applyFilters() }
    }

    fun FlightOffer.getPriceValue(): Double {
        return price.total.toDoubleOrNull() ?: Double.MAX_VALUE
    }

    fun FlightOffer.getTotalDurationMinutes(): Int {
        return itineraries.sumOf {
            try {
                val duration = Duration.parse(it.duration)
                duration.toMinutes().toInt()
            } catch (e: DateTimeParseException) {
                0
            }
        }
    }

    fun FlightOffer.getTotalStops(): Int {
        return itineraries.sumOf { it.segments.size - 1 }
    }

    private fun applyFilters() {
        var filtered = allFlights

        // Escalas
        if (binding.chipDirect.isChecked) {
            filtered = filtered.filter { it.getTotalStops() == 0 }
        } else if (binding.chipScales.isChecked) {
            filtered = filtered.filter { it.getTotalStops() > 0 }
        }

        // Duración
        if (binding.chipShortDuration.isChecked) {
            filtered = filtered.sortedBy { it.getTotalDurationMinutes() }
        }

        // Precio
        if (binding.chipPriceDesc.isChecked) {
            filtered = filtered.sortedByDescending { it.getPriceValue() }
        }

        flightAdapter.setFlights(filtered)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
