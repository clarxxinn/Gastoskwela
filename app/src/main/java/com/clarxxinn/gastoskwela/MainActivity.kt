package com.clarxxinn.gastoskwela

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val bottomNavigation =
            findViewById<BottomNavigationView>(R.id.bottomNavigation)

        val btnAddExpense =
            findViewById<MaterialButton>(R.id.btnAddExpense)

        val btnAddAllowance =
            findViewById<MaterialButton>(R.id.btnAddAllowance)

        btnAddExpense.setOnClickListener {
            Toast.makeText(
                this,
                "Expense screen coming next!",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnAddAllowance.setOnClickListener {
            Toast.makeText(
                this,
                "Allowance screen coming next!",
                Toast.LENGTH_SHORT
            ).show()
        }

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    true
                }

                R.id.nav_expenses,
                R.id.nav_allowance,
                R.id.nav_payments,
                R.id.nav_reports -> {
                    Toast.makeText(
                        this,
                        "${item.title} screen coming next!",
                        Toast.LENGTH_SHORT
                    ).show()
                    false
                }

                else -> false
            }
        }

        bottomNavigation.selectedItemId = R.id.nav_home
    }
}