package com.clarxxinn.gastoskwela

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.clarxxinn.gastoskwela.fragments.AllowanceFragment
import com.clarxxinn.gastoskwela.fragments.ExpensesFragment
import com.clarxxinn.gastoskwela.fragments.HomeFragment
import com.clarxxinn.gastoskwela.fragments.PaymentsFragment
import com.clarxxinn.gastoskwela.fragments.ReportsFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(
            R.id.bottomNavigation
        )

        // Bottom Navigation
        bottomNavigation.setOnItemSelectedListener { item ->

            val fragment: Fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_expenses -> ExpensesFragment()
                R.id.nav_allowance -> AllowanceFragment()
                R.id.nav_payments -> PaymentsFragment()
                R.id.nav_reports -> ReportsFragment()
                else -> return@setOnItemSelectedListener false
            }

            supportFragmentManager.beginTransaction()
                .replace(
                    R.id.fragmentContainer,
                    fragment
                )
                .commit()

            true
        }

        // Default tab
        if (savedInstanceState == null) {
            bottomNavigation.selectedItemId = R.id.nav_home
        }

        // Phase 8: Android Back Button Handling
        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {

                override fun handleOnBackPressed() {

                    val currentTab =
                        bottomNavigation.selectedItemId

                    if (currentTab != R.id.nav_home) {

                        // Return to Home
                        bottomNavigation.selectedItemId =
                            R.id.nav_home

                    } else {

                        // Confirm before exiting
                        showExitConfirmation()
                    }
                }
            }
        )
    }

    // Navigate to a specific tab
    fun selectTab(itemId: Int) {
        bottomNavigation.selectedItemId = itemId
    }

    // Exit confirmation dialog
    private fun showExitConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Exit Gastoskwela?")
            .setMessage(
                "Are you sure you want to exit Gastoskwela?"
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Exit") { _, _ ->
                finish()
            }
            .show()
    }
}