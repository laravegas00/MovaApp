package edu.pmdm.movaapp.ui.reservations

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.pmdm.movaapp.adapter.ReservationAdapter
import edu.pmdm.movaapp.databinding.FragmentSlideshowBinding
import edu.pmdm.movaapp.models.Reservation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReservationFragment : Fragment() {

    private var _binding: FragmentSlideshowBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ReservationAdapter
    private var allReservations: List<Reservation> = listOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSlideshowBinding.inflate(inflater, container, false)
        val root: View = binding.root

        adapter = ReservationAdapter()
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter

        cargarReservasUsuario()

        return root
    }

    private fun cargarReservasUsuario() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("users")
            .document(userId)
            .collection("reservation")
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Toast.makeText(requireContext(), "No reservations found", Toast.LENGTH_SHORT).show()
                } else {
                    val reservas = result.mapNotNull {
                        try {
                            it.toObject(Reservation::class.java)
                        } catch (e: Exception) {
                            Log.e("ReservationFragment", "Error al convertir reserva: ${e.message}", e)
                            null
                        }
                    }
                    allReservations = reservas
                    adapter.setReservas(reservas)
                    setupFilters()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error loading reservations", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupFilters(){
        binding.chipHotel.setOnClickListener { applyFilters() }
        binding.chipFlight.setOnClickListener { applyFilters() }
        binding.chipHotelFlight.setOnClickListener { applyFilters() }
    }

    private fun applyFilters() {
        var filtered = allReservations

        // Filtro por tipo
        filtered = when {
            binding.chipFlight.isChecked -> filtered.filter { it.type == "flight" }
            binding.chipHotel.isChecked -> filtered.filter { it.type == "hotel" }
            binding.chipHotelFlight.isChecked -> filtered.filter { it.type == "flight+hotel" }
            else -> filtered
        }

        adapter.setReservas(filtered)
    }



    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}