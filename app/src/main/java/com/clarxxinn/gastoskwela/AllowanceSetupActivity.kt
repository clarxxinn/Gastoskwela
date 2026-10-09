package com.clarxxinn.gastoskwela

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.clarxxinn.gastoskwela.data.Allowance
import com.clarxxinn.gastoskwela.data.GastoskwelaDatabase
import com.clarxxinn.gastoskwela.utils.MoneyUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.time.LocalDate

class AllowanceSetupActivity : AppCompatActivity() {

    private var selectedFrequency = "Daily"

    private val purple = Color.parseColor("#5B3FD9")
    private val selectedBackground = Color.parseColor("#F0ECFF")
    private val unselectedBackground = Color.WHITE
    private val unselectedBorder = Color.parseColor("#E2E5EF")
    private val darkText = Color.parseColor("#1F2937")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_allowance_setup)

        val group = findViewById<MaterialButtonToggleGroup>(
            R.id.allowanceFrequencyGroup
        )

        val daily = findViewById<MaterialButton>(R.id.btnDaily)
        val weekly = findViewById<MaterialButton>(R.id.btnWeekly)
        val monthly = findViewById<MaterialButton>(R.id.btnMonthly)
        val custom = findViewById<MaterialButton>(R.id.btnCustom)

        val frequencyText = findViewById<TextView>(
            R.id.tvSetupFrequency
        )

        val amountInput = findViewById<TextInputEditText>(
            R.id.etSetupAmount
        )

        val sourceInput = findViewById<TextInputEditText>(
            R.id.etSetupSource
        )

        val amountLayout = findViewById<TextInputLayout>(
            R.id.layoutSetupAmount
        )

        val sourceLayout = findViewById<TextInputLayout>(
            R.id.layoutSetupSource
        )

        val saveButton = findViewById<MaterialButton>(
            R.id.btnSaveAllowanceSetup
        )

        val buttons = listOf(daily, weekly, monthly, custom)

        fun updateSelection() {
            buttons.forEach { button ->
                val selected = when (button.id) {
                    R.id.btnDaily -> selectedFrequency == "Daily"
                    R.id.btnWeekly -> selectedFrequency == "Weekly"
                    R.id.btnMonthly -> selectedFrequency == "Monthly"
                    R.id.btnCustom -> selectedFrequency == "Custom"
                    else -> false
                }

                button.backgroundTintList =
                    ColorStateList.valueOf(
                        if (selected) selectedBackground
                        else unselectedBackground
                    )

                button.strokeColor =
                    ColorStateList.valueOf(
                        if (selected) purple
                        else unselectedBorder
                    )

                button.setTextColor(
                    if (selected) purple else darkText
                )

                button.strokeWidth = if (selected) dp(2) else dp(1)
            }

            frequencyText.text = when (selectedFrequency) {
                "Daily" -> "Daily allowance selected."
                "Weekly" -> "Weekly allowance selected."
                "Monthly" -> "Monthly allowance selected."
                else -> "Custom allowance frequency selected."
            }
        }

        group.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            selectedFrequency = when (checkedId) {
                R.id.btnDaily -> "Daily"
                R.id.btnWeekly -> "Weekly"
                R.id.btnMonthly -> "Monthly"
                else -> selectedFrequency
            }

            updateSelection()
        }

        custom.setOnClickListener {
            selectedFrequency = "Custom"
            group.clearChecked()
            updateSelection()
        }

        group.check(R.id.btnDaily)
        updateSelection()

        saveButton.setOnClickListener {
            amountLayout.error = null
            sourceLayout.error = null

            val amount = MoneyUtils.parseCentavos(
                amountInput.text?.toString().orEmpty()
            )

            val source = sourceInput.text
                ?.toString()
                ?.trim()
                .orEmpty()

            if (amount == null) {
                amountLayout.error = "Enter a valid positive amount"
            }

            if (source.isBlank()) {
                sourceLayout.error = "Allowance source is required"
            }

            if (amount == null || source.isBlank()) {
                return@setOnClickListener
            }

            saveButton.isEnabled = false

            lifecycleScope.launch {
                try {
                    val database = GastoskwelaDatabase.getDatabase(
                        this@AllowanceSetupActivity
                    )

                    database.allowanceDao().insertAllowance(
                        Allowance(
                            amountCentavos = amount,
                            source = source,
                            date = LocalDate.now().toString(),
                            notes = "Initial $selectedFrequency allowance"
                        )
                    )

                    getSharedPreferences(
                        "gastoskwela_preferences",
                        MODE_PRIVATE
                    ).edit()
                        .putBoolean("allowance_setup_completed", true)
                        .putString("allowance_frequency", selectedFrequency)
                        .apply()

                    startActivity(
                        Intent(
                            this@AllowanceSetupActivity,
                            MainActivity::class.java
                        )
                    )

                    finish()
                } catch (e: Exception) {
                    saveButton.isEnabled = true
                    amountLayout.error =
                        "Unable to save allowance. Please try again."
                }
            }
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}