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

    private var currentTabId = R.id.nav_home

    companion object {
        private const val KEY_SELECTED_TAB = "selected_tab"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        bottomNavigation = findViewById(
            R.id.bottomNavigation
        )

        bottomNavigation.setOnItemSelectedListener { item ->
            if (item.itemId == currentTabId &&
                supportFragmentManager.findFragmentById(
                    R.id.fragmentContainer
                ) != null
            ) {
                return@setOnItemSelectedListener true
            }

            val fragment = createFragment(item.itemId)
                ?: return@setOnItemSelectedListener false

            currentTabId = item.itemId

            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                    R.id.fragmentContainer,
                    fragment
                )
                .commit()

            true
        }

        val restoredTab = savedInstanceState?.getInt(
            KEY_SELECTED_TAB,
            R.id.nav_home
        ) ?: R.id.nav_home

        val validTab = if (createFragment(restoredTab) != null) {
            restoredTab
        } else {
            R.id.nav_home
        }

        if (savedInstanceState != null) {
            currentTabId = validTab
        }

        bottomNavigation.selectedItemId = validTab

        if (
            supportFragmentManager.findFragmentById(
                R.id.fragmentContainer
            ) == null
        ) {
            val fragment = createFragment(validTab) ?: HomeFragment()

            supportFragmentManager.beginTransaction()
                .setReorderingAllowed(true)
                .replace(
                    R.id.fragmentContainer,
                    fragment
                )
                .commit()

            currentTabId = validTab
        }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    if (currentTabId != R.id.nav_home) {
                        selectTab(R.id.nav_home)
                    } else {
                        showExitConfirmation()
                    }
                }
            }
        )
    }

    private fun createFragment(itemId: Int): Fragment? {
        return when (itemId) {
            R.id.nav_home -> HomeFragment()
            R.id.nav_expenses -> ExpensesFragment()
            R.id.nav_allowance -> AllowanceFragment()
            R.id.nav_payments -> PaymentsFragment()
            R.id.nav_reports -> ReportsFragment()
            else -> null
        }
    }

    fun selectTab(itemId: Int) {
        if (createFragment(itemId) == null) return

        bottomNavigation.selectedItemId = itemId
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_SELECTED_TAB, currentTabId)
        super.onSaveInstanceState(outState)
    }

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