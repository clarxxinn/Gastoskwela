package com.clarxxinn.gastoskwela

import android.content.Intent
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_allowance_setup)

        val frequencyGroup = findViewById<MaterialButtonToggleGroup>(
            R.id.allowanceFrequencyGroup
        )

        val customButton = findViewById<MaterialButton>(
            R.id.btnCustom
        )

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

        frequencyGroup.check(R.id.btnDaily)

        frequencyGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener

            selectedFrequency = when (checkedId) {
                R.id.btnDaily -> "Daily"
                R.id.btnWeekly -> "Weekly"
                R.id.btnMonthly -> "Monthly"
                else -> "Daily"
            }

            customButton.isChecked = false
            updateFrequencyText(frequencyText)
        }

        customButton.isCheckable = true

        customButton.setOnClickListener {
            frequencyGroup.clearChecked()
            selectedFrequency = "Custom"
            customButton.isChecked = true
            updateFrequencyText(frequencyText)
        }

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

            var valid = true

            if (amount == null) {
                amountLayout.error = "Enter a valid positive amount"
                valid = false
            }

            if (source.isBlank()) {
                sourceLayout.error = "Allowance source is required"
                valid = false
            }

            if (!valid) return@setOnClickListener

            saveButton.isEnabled = false

            lifecycleScope.launch {
                try {
                    val database = GastoskwelaDatabase.getDatabase(
                        this@AllowanceSetupActivity
                    )

                    database.allowanceDao().insertAllowance(
                        Allowance(
                            amountCentavos = amount!!,
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

    private fun updateFrequencyText(textView: TextView) {
        textView.text = when (selectedFrequency) {
            "Daily" ->
                "Your allowance will be recorded as a daily allowance."

            "Weekly" ->
                "Your allowance will be recorded as a weekly allowance."

            "Monthly" ->
                "Your allowance will be recorded as a monthly allowance."

            else ->
                "Your allowance will be recorded with a custom frequency."
        }
    }
}