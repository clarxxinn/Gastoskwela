package com.clarxxinn.gastoskwela

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.clarxxinn.gastoskwela.data.Allowance
import com.clarxxinn.gastoskwela.data.GastoskwelaDatabase
import com.clarxxinn.gastoskwela.utils.MoneyUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.time.LocalDate

class AllowanceSetupActivity : AppCompatActivity() {

    private var selectedFrequency = "Daily"
    private var currentStep = 1
    private var isSaving = false

    private lateinit var frequencySection: LinearLayout
    private lateinit var detailsSection: LinearLayout
    private lateinit var titleText: TextView
    private lateinit var subtitleText: TextView
    private lateinit var frequencyText: TextView
    private lateinit var nextButton: MaterialButton
    private lateinit var setupScroll: ScrollView

    private lateinit var amountInput: TextInputEditText
    private lateinit var sourceInput: TextInputEditText
    private lateinit var amountLayout: TextInputLayout
    private lateinit var sourceLayout: TextInputLayout

    private lateinit var cards: Map<String, LinearLayout>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_allowance_setup)

        selectedFrequency = savedInstanceState
            ?.getString("selectedFrequency") ?: "Daily"

        currentStep = savedInstanceState
            ?.getInt("currentStep") ?: 1

        frequencySection = findViewById(R.id.setupFrequencySection)
        detailsSection = findViewById(R.id.setupDetailsSection)
        titleText = findViewById(R.id.tvSetupTitle)
        subtitleText = findViewById(R.id.tvSetupSubtitle)
        frequencyText = findViewById(R.id.tvSetupFrequency)
        nextButton = findViewById(R.id.btnSetupNext)
        setupScroll = findViewById(R.id.setupScroll)

        amountInput = findViewById(R.id.etSetupAmount)
        sourceInput = findViewById(R.id.etSetupSource)
        amountLayout = findViewById(R.id.layoutSetupAmount)
        sourceLayout = findViewById(R.id.layoutSetupSource)

        cards = mapOf(
            "Daily" to findViewById(R.id.cardDaily),
            "Weekly" to findViewById(R.id.cardWeekly),
            "Monthly" to findViewById(R.id.cardMonthly),
            "Custom" to findViewById(R.id.cardCustom)
        )

        cards.forEach { (frequency, card) ->
            card.setOnClickListener {
                if (currentStep == 1 && !isSaving) {
                    selectedFrequency = frequency
                    updateSelection()
                }
            }
        }

        findViewById<ImageButton>(R.id.btnSetupBack)
            .setOnClickListener {
                handleBack()
            }

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    handleBack()
                }
            }
        )

        nextButton.setOnClickListener {
            if (isSaving) return@setOnClickListener

            if (currentStep == 1) {
                if (selectedFrequency == "Custom") {
                    currentStep = 2
                    showStep()
                } else {
                    savePresetAllowance()
                }
            } else {
                saveCustomAllowance()
            }
        }

        updateSelection()
        showStep()
    }

    private fun updateSelection() {
        cards.forEach { (frequency, card) ->
            val selected = frequency == selectedFrequency

            card.setBackgroundResource(
                if (selected) {
                    R.drawable.bg_setup_card_selected
                } else {
                    R.drawable.bg_setup_card_unselected
                }
            )

            card.isSelected = selected
        }

        frequencyText.text = when (selectedFrequency) {
            "Daily" -> "Daily allowance selected."
            "Weekly" -> "Weekly allowance selected."
            "Monthly" -> "Monthly allowance selected."
            else -> "Custom / Irregular allowance selected."
        }
    }

    private fun showStep() {
        if (currentStep == 1) {
            frequencySection.visibility = View.VISIBLE
            detailsSection.visibility = View.GONE

            titleText.text = "Set Your Allowance"
            subtitleText.text = "You can change this anytime."
            nextButton.text = "Next"
        } else {
            frequencySection.visibility = View.GONE
            detailsSection.visibility = View.VISIBLE

            titleText.text = "Allowance Details"
            subtitleText.text = "Enter your custom allowance."
            nextButton.text = "Save & Continue"
        }

        setupScroll.post {
            setupScroll.scrollTo(0, 0)
        }
    }

    private fun handleBack() {
        if (isSaving) return

        if (currentStep == 2) {
            currentStep = 1
            showStep()
        } else {
            startActivity(
                Intent(
                    this,
                    OnboardingActivity::class.java
                )
            )
            finish()
        }
    }

    private fun savePresetAllowance() {
        val amountCentavos = when (selectedFrequency) {
            "Daily" -> 20_000L
            "Weekly" -> 100_000L
            "Monthly" -> 400_000L
            else -> return
        }

        saveAllowance(
            amountCentavos = amountCentavos,
            source = "Allowance"
        )
    }

    private fun saveCustomAllowance() {
        amountLayout.error = null
        sourceLayout.error = null

        val amount = MoneyUtils.parseCentavos(
            amountInput.text?.toString().orEmpty()
        )

        val source = sourceInput.text
            ?.toString()
            ?.trim()
            .orEmpty()

        if (amount == null || amount <= 0L) {
            amountLayout.error = "Enter a valid positive amount"
        }

        if (source.isBlank()) {
            sourceLayout.error = "Allowance source is required"
        }

        if (amount == null || amount <= 0L || source.isBlank()) {
            return
        }

        saveAllowance(
            amountCentavos = amount,
            source = source
        )
    }

    private fun saveAllowance(
        amountCentavos: Long,
        source: String
    ) {
        if (isSaving) return

        isSaving = true
        nextButton.isEnabled = false

        lifecycleScope.launch {
            try {
                val database = GastoskwelaDatabase.getDatabase(
                    this@AllowanceSetupActivity
                )

                database.allowanceDao().insertAllowance(
                    Allowance(
                        amountCentavos = amountCentavos,
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
                isSaving = false
                nextButton.isEnabled = true

                Toast.makeText(
                    this@AllowanceSetupActivity,
                    "Unable to save allowance. Please try again.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("selectedFrequency", selectedFrequency)
        outState.putInt("currentStep", currentStep)
        super.onSaveInstanceState(outState)
    }
}