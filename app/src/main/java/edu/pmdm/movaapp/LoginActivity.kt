package edu.pmdm.movaapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.SignInButton
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var credentialManager: CredentialManager

    private lateinit var editTextEmail: EditText
    private lateinit var editTextPassword: EditText
    private lateinit var buttonLogin: Button
    private lateinit var buttonRegister: Button
    private lateinit var buttonGoogleSignIn: SignInButton
    private lateinit var buttonGuest: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        credentialManager = CredentialManager.create(this)

        initializeUI()

        if (FirebaseAuth.getInstance().currentUser != null) {
            redirectToMainActivity()
            return
        }

        buttonLogin.setOnClickListener { loginUser() }
        buttonRegister.setOnClickListener { registerUser() }
        buttonGoogleSignIn.setOnClickListener { loginWithGoogle() }
        buttonGuest.setOnClickListener { redirectToMainActivity() }
    }

    private fun initializeUI() {
        editTextEmail = findViewById(R.id.etUser)
        editTextPassword = findViewById(R.id.etPassword)
        buttonLogin = findViewById(R.id.btnLogIn)
        buttonRegister = findViewById(R.id.btnSignIn)
        buttonGoogleSignIn = findViewById(R.id.sign_in_button)
    }

    private fun loginWithGoogle() {
        lifecycleScope.launch {
            try {
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setServerClientId(getString(R.string.web_client_id))
                    .setFilterByAuthorizedAccounts(false)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val response = credentialManager.getCredential(this@LoginActivity, request)
                handleSignIn(response.credential)

//            } catch (e: GetCredentialCancellationException) {
//                e.printStackTrace()
//                Toast.makeText(this@LoginActivity, "Inicio de sesión cancelado", Toast.LENGTH_SHORT).show()
            } catch (e: GetCredentialException) {
                e.printStackTrace()
                Toast.makeText(this@LoginActivity, "Error en inicio de sesión", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun handleSignIn(credential: Credential) {
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            authenticateWithFirebase(googleIdTokenCredential.idToken)
        } else {
            Log.w("TAG", "Credential is not of type Google ID!")
        }
    }

    private fun authenticateWithFirebase(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    firebaseAuth.currentUser?.let { user ->
                        saveUserData(user.uid, user.displayName, user.email)
                        redirectToMainActivity()
                    }
                } else {
                    Toast.makeText(this, "Autenticación fallida", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loginUser() {
        val email = editTextEmail.text.toString().trim()
        val password = editTextPassword.text.toString().trim()
        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) return

        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    firebaseAuth.currentUser?.let { user ->
                        saveUserData(user.uid, user.displayName, user.email)
                        redirectToMainActivity()
                    }
                }

                if (!task.isSuccessful) {
                    Toast.makeText(this, "Error al iniciar sesión", Toast.LENGTH_SHORT).show()
                }

            }

    }

    private fun registerUser() {
        val email = editTextEmail.text.toString().trim()
        val password = editTextPassword.text.toString().trim()

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Introduce email y contraseña", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "La contraseña debe tener al menos 6 caracteres", Toast.LENGTH_SHORT).show()
            return
        }

        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    firebaseAuth.currentUser?.let { user ->
                        saveUserData(user.uid, user.displayName, user.email)
                        redirectToMainActivity()
                    }
                }

                if (!task.isSuccessful) {
                    Toast.makeText(this, "Error al iniciar sesión", Toast.LENGTH_SHORT).show()
                }


            }
    }

    private fun saveUserData(userId: String, name: String?, email: String?) {
        val userData = hashMapOf(
            "userId" to userId,
            "email" to email,
            "name" to name,
            "address" to "",
            "phone" to ""
        )

        db.collection("users").document(userId)
            .set(userData)
            .addOnSuccessListener { Log.d("LogInActivity", "Usuario guardado en Firestore") }
            .addOnFailureListener { e -> Log.e("LogInActivity", "Error al guardar usuario", e) }

        val editor = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE).edit()
        editor.putString("userId", userId)
        editor.putString("email", email)
        editor.putString("name", name)
        editor.apply()
    }

    private fun redirectToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

}
