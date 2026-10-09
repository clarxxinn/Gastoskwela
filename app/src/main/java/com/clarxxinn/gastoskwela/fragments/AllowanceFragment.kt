package com.clarxxinn.gastoskwela.fragments

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.clarxxinn.gastoskwela.MainActivity
import com.clarxxinn.gastoskwela.R
import com.clarxxinn.gastoskwela.data.Allowance
import com.clarxxinn.gastoskwela.data.GastoskwelaDatabase
import com.clarxxinn.gastoskwela.utils.MoneyUtils
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class AllowanceFragment : Fragment(R.layout.fragment_allowance) {

    private val database by lazy {
        GastoskwelaDatabase.getDatabase(requireContext())
    }

    private val allowanceDao by lazy {
        database.allowanceDao()
    }

    private val expenseDao by lazy {
        database.expenseDao()
    }

    private val frequencies = arrayOf(
        "Daily",
        "Weekly",
        "Monthly",
        "Custom"
    )

    private var selectedTab = 0

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        selectedTab = savedInstanceState
            ?.getInt("selectedTab") ?: 0

        updateFrequencyLabel(view)
        setupTabs(view)

        view.findViewById<View>(
            R.id.btnAllowanceBack
        ).setOnClickListener {
            (requireActivity() as MainActivity)
                .selectTab(R.id.nav_home)
        }

        view.findViewById<View>(
            R.id.btnAddAllowance
        ).setOnClickListener {
            showAllowanceDialog()
        }

        view.findViewById<View>(
            R.id.btnChangeAllowanceFrequency
        ).setOnClickListener {
            showFrequencyDialog(view)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                launch {
                    allowanceDao.getAllAllowances()
                        .collect { allowances ->
                            renderAllowances(view, allowances)
                        }
                }

                launch {
                    combine(
                        allowanceDao.getTotalAllowance(),
                        expenseDao.getTotalExpenses()
                    ) { allowance, expenses ->
                        allowance to expenses
                    }.collect { (allowance, expenses) ->

                        val balance = allowance - expenses

                        view.findViewById<TextView>(
                            R.id.tvTotalAllowance
                        ).text = MoneyUtils.format(allowance)

                        view.findViewById<TextView>(
                            R.id.tvAllowanceBalance
                        ).text = MoneyUtils.format(balance)

                        view.findViewById<TextView>(
                            R.id.tvAllowanceSpent
                        ).text = MoneyUtils.format(expenses)

                        view.findViewById<TextView>(
                            R.id.tvAllowanceSummary
                        ).text = when {
                            allowance <= 0L ->
                                "Add your first allowance to get started."

                            balance < 0L ->
                                "Your expenses have exceeded your allowance."

                            balance == 0L ->
                                "You have used your available allowance."

                            else ->
                                "Keep tracking your baon and gastos!"
                        }
                    }
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("selectedTab", selectedTab)
        super.onSaveInstanceState(outState)
    }

    private fun setupTabs(view: View) {
        view.findViewById<View>(
            R.id.tabAllowanceHistory
        ).setOnClickListener {
            switchTab(view, 0)
        }

        view.findViewById<View>(
            R.id.tabAllowanceSettings
        ).setOnClickListener {
            switchTab(view, 1)
        }

        switchTab(view, selectedTab, animate = false)
    }

    private fun switchTab(
        view: View,
        tab: Int,
        animate: Boolean = true
    ) {
        if (tab == selectedTab && animate) return

        selectedTab = tab

        val history = view.findViewById<View>(
            R.id.allowanceHistorySection
        )

        val settings = view.findViewById<View>(
            R.id.allowanceSettingsSection
        )

        val historyIndicator = view.findViewById<View>(
            R.id.indicatorAllowanceHistory
        )

        val settingsIndicator = view.findViewById<View>(
            R.id.indicatorAllowanceSettings
        )

        val historyLabel = view.findViewById<TextView>(
            R.id.tvAllowanceHistoryTab
        )

        val settingsLabel = view.findViewById<TextView>(
            R.id.tvAllowanceSettingsTab
        )

        history.visibility =
            if (tab == 0) View.VISIBLE else View.GONE

        settings.visibility =
            if (tab == 1) View.VISIBLE else View.GONE

        historyIndicator.visibility =
            if (tab == 0) View.VISIBLE else View.INVISIBLE

        settingsIndicator.visibility =
            if (tab == 1) View.VISIBLE else View.INVISIBLE

        historyLabel.setTextColor(
            if (tab == 0) 0xFF6547ED.toInt()
            else 0xFF6B7280.toInt()
        )

        settingsLabel.setTextColor(
            if (tab == 1) 0xFF6547ED.toInt()
            else 0xFF6B7280.toInt()
        )

        if (animate) {
            val active = if (tab == 0) history else settings

            active.alpha = 0f
            active.translationX =
                if (tab == 0) -24f else 24f

            active.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(220)
                .start()
        }
    }

    private fun updateFrequencyLabel(view: View) {
        val preferences = requireContext().getSharedPreferences(
            "gastoskwela_preferences",
            Context.MODE_PRIVATE
        )

        val frequency = preferences.getString(
            "allowance_frequency",
            "Daily"
        ) ?: "Daily"

        view.findViewById<TextView>(
            R.id.tvAllowanceFrequency
        ).text = frequency
    }

    private fun showFrequencyDialog(view: View) {
        val preferences = requireContext().getSharedPreferences(
            "gastoskwela_preferences",
            Context.MODE_PRIVATE
        )

        val current = preferences.getString(
            "allowance_frequency",
            "Daily"
        ) ?: "Daily"

        var selected = frequencies.indexOf(current)
            .coerceAtLeast(0)

        AlertDialog.Builder(requireContext())
            .setTitle("Allowance Frequency")
            .setSingleChoiceItems(
                frequencies,
                selected
            ) { _, which ->
                selected = which
            }
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save") { _, _ ->
                preferences.edit()
                    .putString(
                        "allowance_frequency",
                        frequencies[selected]
                    )
                    .apply()

                updateFrequencyLabel(view)
            }
            .show()
    }

    private fun renderAllowances(
        view: View,
        allowances: List<Allowance>
    ) {
        val container = view.findViewById<LinearLayout>(
            R.id.allowanceListContainer
        )

        val emptyText = view.findViewById<TextView>(
            R.id.tvEmptyAllowance
        )

        val countText = view.findViewById<TextView>(
            R.id.tvAllowanceCount
        )

        container.removeAllViews()

        val sorted = allowances.sortedWith(
            compareByDescending<Allowance> { it.date }
                .thenByDescending { it.id }
        )

        countText.text = if (sorted.size == 1) {
            "1 record"
        } else {
            "${sorted.size} records"
        }

        emptyText.visibility =
            if (sorted.isEmpty()) View.VISIBLE else View.GONE

        emptyText.text =
            "No allowance records yet. Tap Add Allowance to begin."

        val formatter = DateTimeFormatter.ofPattern(
            "MMM d, yyyy",
            Locale.ENGLISH
        )

        sorted.forEach { allowance ->

            val row = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(dp(7), dp(13), dp(7), dp(13))
                background = android.graphics.drawable
                    .GradientDrawable().apply {
                        setColor(0xFFFFFFFF.toInt())
                        cornerRadius = dp(12).toFloat()
                    }
                isClickable = true
                isFocusable = true
            }

            val circle = TextView(requireContext()).apply {
                text = "○"
                textSize = 26f
                gravity = android.view.Gravity.CENTER
                setTextColor(0xFF10B981.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    dp(35), dp(40)
                )
            }

            row.addView(circle)

            val details = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            val source = TextView(requireContext()).apply {
                text = allowance.source
                textSize = 12f
                setTextColor(0xFF1F2937.toInt())
                typeface = resources.getFont(
                    R.font.poppins_semibold
                )
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }

            val date = TextView(requireContext()).apply {
                text = try {
                    LocalDate.parse(allowance.date)
                        .format(formatter)
                } catch (_: Exception) {
                    allowance.date
                }

                textSize = 10f
                setTextColor(0xFF929AB2.toInt())
                typeface = resources.getFont(
                    R.font.poppins_regular
                )
            }

            details.addView(source)
            details.addView(date)
            row.addView(details)

            val amount = TextView(requireContext()).apply {
                text = "+${MoneyUtils.format(
                    allowance.amountCentavos
                )}"

                textSize = 12f
                setTextColor(0xFF22C55E.toInt())
                typeface = resources.getFont(
                    R.font.poppins_semibold
                )
                setPadding(dp(5), 0, 0, 0)
            }

            row.addView(amount)

            row.setOnClickListener {
                showAllowanceDialog(allowance)
            }

            row.setOnLongClickListener {
                confirmDelete(allowance)
                true
            }

            container.addView(row)

            val divider = View(requireContext()).apply {
                setBackgroundColor(0xFFF0F2F8.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(1)
                )
            }

            container.addView(divider)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun showAllowanceDialog(
        existing: Allowance? = null
    ) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_allowance, null)

        val amountInput =
            dialogView.findViewById<TextInputEditText>(
                R.id.etAmount
            )

        val sourceInput =
            dialogView.findViewById<TextInputEditText>(
                R.id.etSource
            )

        val notesInput =
            dialogView.findViewById<TextInputEditText>(
                R.id.etNotes
            )

        val amountLayout =
            dialogView.findViewById<TextInputLayout>(
                R.id.layoutAmount
            )

        val sourceLayout =
            dialogView.findViewById<TextInputLayout>(
                R.id.layoutSource
            )

        val dateButton = dialogView.findViewById<TextView>(
            R.id.btnSelectDate
        )

        var selectedDate =
            existing?.date ?: LocalDate.now().toString()

        dateButton.text = selectedDate

        existing?.let { allowance ->
            amountInput.setText(
                BigDecimal.valueOf(
                    allowance.amountCentavos,
                    2
                ).toPlainString()
            )

            sourceInput.setText(allowance.source)
            notesInput.setText(allowance.notes)
        }

        dateButton.setOnClickListener {
            val date = LocalDate.parse(selectedDate)

            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    selectedDate = LocalDate.of(
                        year,
                        month + 1,
                        day
                    ).toString()

                    dateButton.text = selectedDate
                },
                date.year,
                date.monthValue - 1,
                date.dayOfMonth
            ).show()
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(
                if (existing == null) {
                    "Add Allowance"
                } else {
                    "Edit Allowance"
                }
            )
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton(
                if (existing == null) "Save Allowance"
                else "Update",
                null
            )
            .create()

        dialog.setOnShowListener {
            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                amountLayout.error = null
                sourceLayout.error = null

                val centavos = MoneyUtils.parseCentavos(
                    amountInput.text?.toString().orEmpty()
                )

                val source = sourceInput.text
                    ?.toString()?.trim().orEmpty()

                val notes = notesInput.text
                    ?.toString()?.trim().orEmpty()

                var valid = true

                if (centavos == null || centavos <= 0L) {
                    amountLayout.error =
                        "Enter a valid positive amount"
                    valid = false
                }

                if (source.isBlank()) {
                    sourceLayout.error = "Source is required"
                    valid = false
                }

                if (!valid) return@setOnClickListener

                val allowance = Allowance(
                    id = existing?.id ?: 0,
                    amountCentavos = centavos!!,
                    source = source,
                    date = selectedDate,
                    notes = notes
                )

                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        if (existing == null) {
                            allowanceDao.insertAllowance(allowance)
                        } else {
                            allowanceDao.updateAllowance(allowance)
                        }

                        dialog.dismiss()
                    } catch (_: Exception) {
                        Toast.makeText(
                            requireContext(),
                            "Unable to save allowance.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun confirmDelete(allowance: Allowance) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Allowance?")
            .setMessage(
                "Are you sure you want to delete this allowance record?"
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        allowanceDao.deleteAllowance(allowance)
                    } catch (_: Exception) {
                        Toast.makeText(
                            requireContext(),
                            "Unable to delete allowance.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .show()
    }
}