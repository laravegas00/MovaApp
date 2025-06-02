package edu.pmdm.movaapp.ui.slideshow

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.pmdm.movaapp.adapter.ReservationAdapter
import edu.pmdm.movaapp.databinding.FragmentSlideshowBinding
import edu.pmdm.movaapp.models.Reservation

class ReservationFragment : Fragment() {

    private var _binding: FragmentSlideshowBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ReservationAdapter

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
                    Toast.makeText(requireContext(), "No hay reservas encontradas", Toast.LENGTH_SHORT).show()
                } else {
                    val reservas = result.mapNotNull {
                        try {
                            it.toObject(Reservation::class.java)
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Error al convertir reserva: ${e.message}", Toast.LENGTH_SHORT).show()
                            null
                        }
                    }
                    adapter.setReservas(reservas)
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al cargar reservas", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}