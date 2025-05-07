package edu.pmdm.movaapp

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
                findNavController().navigate(R.id.homeFragment)
            }
        })

        // Recuperar argumentos del Safe Args
        arguments?.let {
            val args = FlightItemsFragmentArgs.fromBundle(it)
            from = args.from
            to = args.to
            departureDate = args.departureDate
            returnDate = args.returnDate
            passengers = args.passengers
            fromFullName = args.fromFullName
            toFullName = args.toFullName
            isReturn = args.isReturn
        }

        Log.d("FlightItems", "CARGANDO: from=$from, to=$to, departureDate=$departureDate, returnDate=$returnDate")

        // Mostrar resumen de parámetros
        isReturn = FlightItemsFragmentArgs.fromBundle(requireArguments()).isReturn

        // Mostrar resumen
        if (isReturn) {
            binding.tvTitle.text = "Vuelos de vuelta"
            binding.tvRouteSummary.text = "$fromFullName ➝ $toFullName"
            binding.tvSummary.text = "$fromFullName ➝ $toFullName"
            binding.tvDatePassengerSummary.text = "$returnDate - $passengers pasajero(s)"
        } else {
            binding.tvTitle.text = "Vuelos de ida"
            binding.tvRouteSummary.text = "$fromFullName ➝ $toFullName"
            binding.tvSummary.text = "$fromFullName ➝ $toFullName"
            binding.tvDatePassengerSummary.text = "$departureDate ➝ $returnDate - $passengers pasajero(s)"
        }

        // Setup RecyclerView
        flightAdapter = FlightAdapter()
        flightAdapter.setOnFlightSelectedListener { selectedFlight ->

            if (isReturn) {
                sharedViewModel.setReturnFlight(selectedFlight)

                val action = FlightItemsFragmentDirections
                    .actionFlightItemsFragmentToSummaryFragment(
                        fromFullName = fromFullName,
                        toFullName = toFullName
                    )
                findNavController().navigate(action)
            } else {
                sharedViewModel.setOutboundFlight(selectedFlight)

                val action = FlightItemsFragmentDirections
                    .actionFlightItemsFragmentSelf(
                        from = to,
                        to = from,
                        departureDate = returnDate ?: "",
                        returnDate = returnDate, // Ya no hay otra vuelta
                        passengers = passengers,
                        isReturn = true,
                        fromFullName = fromFullName,
                        toFullName = toFullName
                    )
                Log.d(
                    "DEBUG",
                    "Navegando a vuelos de vuelta con from=$to, to=$from, departureDate=$returnDate"
                )


                findNavController().navigate(action)
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
        binding.loadingContainer.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val flights = repository.searchFlights(from, to, departureDate, returnDate, passengers)

                withContext(Dispatchers.Main) {
                    if (!flights.isNullOrEmpty()) {
                        flightAdapter.setFlights(flights)
                        Log.d("FlightItems", "Cargados ${flights.size} vuelos")
                    } else {
                        Toast.makeText(requireContext(), "No se encontraron vuelos", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("FlightItems", "Error al cargar vuelos: ${e.localizedMessage}")
                    Toast.makeText(requireContext(), "Error al cargar vuelos", Toast.LENGTH_SHORT).show()
                }
            } finally {
                withContext(Dispatchers.Main) {
                    binding.loadingContainer.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
