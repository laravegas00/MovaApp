package edu.pmdm.movaapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.pmdm.movaapp.databinding.FragmentUserDataBinding

class UserDataFragment : Fragment() {

    private var _binding: FragmentUserDataBinding? = null
    private val binding get() = _binding!!

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var userId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUserDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        userId = auth.currentUser?.uid ?: return

        loadUserData()

        binding.btnConfirm.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val address = binding.etAddress.text.toString().trim()
            val phone = binding.etPhone.text.toString().trim()

            if (name.isEmpty() || address.isEmpty() || phone.isEmpty()) {
                Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val data = mapOf(
                "name" to encryptHelper.encrypt(name),
                "email" to encryptHelper.encrypt(email),
                "address" to encryptHelper.encrypt(address),
                "phone" to encryptHelper.encrypt(phone)
            )

            firestore.collection("users").document(userId)
                .set(data)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Data saved successfully", Toast.LENGTH_SHORT).show()
                    findNavController().popBackStack()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error saving data", Toast.LENGTH_SHORT).show()
                }
        }

        binding.btnEdit.setOnClickListener {
            binding.etName.visibility = View.VISIBLE
            binding.etEmail.visibility = View.VISIBLE
            binding.etEmail.setText(auth.currentUser?.email ?: "")
            binding.etEmail.isEnabled = false
            binding.etAddress.visibility = View.VISIBLE
            binding.etPhone.visibility = View.VISIBLE
            binding.tvSummary.visibility = View.GONE
        }

        binding.btnDelete.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account and all related data?")
                .setIcon(R.drawable.advertencia)
                .setPositiveButton("Yes") { _, _ ->
                    deleteUserAccount()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }


    }

    private fun loadUserData() {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name")?.let { encryptHelper.decrypt(it) } ?: ""
                    val email = document.getString("email")?.let { encryptHelper.decrypt(it) } ?: ""
                    val address = document.getString("address")?.let { encryptHelper.decrypt(it) } ?: ""
                    val phone = document.getString("phone")?.let { encryptHelper.decrypt(it) } ?: ""

                    if (name.isNotEmpty() && email.isNotEmpty() && address.isNotEmpty() && phone.isNotEmpty()) {
                        val resumen = "Name: $name\nEmail: $email\nAddress: $address\nPhone: $phone"
                        binding.tvSummary.text = resumen
                        binding.tvSummary.visibility = View.VISIBLE

                        // Ocultar campos de edición
                        binding.etName.visibility = View.GONE
                        binding.etEmail.visibility = View.GONE
                        binding.etAddress.visibility = View.GONE
                        binding.etPhone.visibility = View.GONE
                    } else {
                        binding.etName.setText(name)
                        binding.etEmail.setText(email)
                        binding.etEmail.isEnabled = false
                        binding.etAddress.setText(address)
                        binding.etPhone.setText(phone)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load user data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteUserAccount() {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        firestore.collection("users").document(user.uid)
            .delete()
            .addOnSuccessListener {
                user.delete()
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Account deleted", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.loginActivity)
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "Error deleting Firebase user", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error deleting user data", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
