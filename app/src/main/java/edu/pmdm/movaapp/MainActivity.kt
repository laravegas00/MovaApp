package edu.pmdm.movaapp

import android.os.Bundle
import android.view.Menu
import android.widget.Button
import android.widget.TextView
import android.content.Intent
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.pmdm.movaapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.flightFragment, R.id.hotelFragment, R.id.reservationFragment
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        val headerView = navView.getHeaderView(0)

        val btnEditUser = headerView.findViewById<Button>(R.id.btnEditUser)
        btnEditUser.setOnClickListener {
            editUser()
        }

        val btnLogOut = headerView.findViewById<Button>(R.id.btnLogOut)
        btnLogOut.setOnClickListener {
            logout()
        }

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_user_guide -> {
                showUserManual()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showUserManual() {
        AlertDialog.Builder(this)
            .setTitle("User guide")
            .setMessage("""
            Welcome to MovaApp User Guide!
            
            • Search flight.
            • Search hotel.
            • Select flight and hotel.
            • Edit your user profile.
            • Make a reservation.
            • View your reservations.

            Thank you for using MovaApp!
        """.trimIndent())
            .setPositiveButton("Close", null)
            .show()
    }


    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun editUser (){
        findNavController(R.id.nav_host_fragment_content_main).navigate(R.id.userDataFragment)
        binding.drawerLayout.closeDrawers()
    }

    private fun logout (){
        FirebaseAuth.getInstance().signOut()
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        updateUserHeader()
    }

    private fun updateUserHeader() {
        val headerView = binding.navView.getHeaderView(0)
        val tvName = headerView.findViewById<TextView>(R.id.tvName)
        val tvEmail = headerView.findViewById<TextView>(R.id.tvEmail)

        val user = FirebaseAuth.getInstance().currentUser
        tvEmail.text = user?.email ?: ""

        FirebaseFirestore.getInstance().collection("users")
            .document(user?.uid ?: return)
            .get()
            .addOnSuccessListener { doc ->
                val encryptedName = doc.getString("name") ?: ""
                val decryptedName = encryptHelper.decrypt(encryptedName)
                tvName.text = decryptedName
            }
    }



}