package com.clarxxinn.gastoskwela.fragments

import android.app.DatePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
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
import com.clarxxinn.gastoskwela.data.Expense
import com.clarxxinn.gastoskwela.data.GastoskwelaDatabase
import com.clarxxinn.gastoskwela.utils.MoneyUtils
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
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
        "Daily", "Weekly", "Monthly", "Custom"
    )

    private val periods = arrayOf(
        "Daily", "Weekly", "Monthly", "Yearly"
    )

    private var selectedTab = 0
    private var selectedPeriod = 0
    private var selectedDate = LocalDate.now()

    private var allAllowances = emptyList<Allowance>()
    private var allExpenses = emptyList<Expense>()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        selectedTab = savedInstanceState
            ?.getInt("selectedTab") ?: 0

        selectedPeriod = savedInstanceState
            ?.getInt("selectedPeriod") ?: 0

        selectedDate = try {
            LocalDate.parse(
                savedInstanceState?.getString("selectedDate")
                    ?: LocalDate.now().toString()
            )
        } catch (_: Exception) {
            LocalDate.now()
        }

        setupTabs(view)
        setupOverview(view)
        updateFrequencyLabel(view)

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
                combine(
                    allowanceDao.getAllAllowances(),
                    expenseDao.getAllExpenses()
                ) { allowances, expenses ->
                    Pair(allowances, expenses)
                }.collect { (allowances, expenses) ->
                    allAllowances = allowances
                    allExpenses = expenses

                    renderAllowances(view)
                    renderOverview(view)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("selectedTab", selectedTab)
        outState.putInt("selectedPeriod", selectedPeriod)
        outState.putString(
            "selectedDate",
            selectedDate.toString()
        )
        super.onSaveInstanceState(outState)
    }

    private fun setupOverview(view: View) {
        val periodLabel = view.findViewById<TextView>(
            R.id.tvOverviewPeriod
        )

        periodLabel.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Select Overview Period")
                .setSingleChoiceItems(
                    periods,
                    selectedPeriod
                ) { dialog, which ->
                    selectedPeriod = which
                    renderOverview(view)
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        view.findViewById<View>(
            R.id.btnOverviewPrevious
        ).setOnClickListener {
            selectedDate = when (selectedPeriod) {
                0 -> selectedDate.minusDays(1)
                1 -> selectedDate.minusWeeks(1)
                2 -> selectedDate.minusMonths(1)
                else -> selectedDate.minusYears(1)
            }
            renderOverview(view)
        }

        view.findViewById<View>(
            R.id.btnOverviewNext
        ).setOnClickListener {
            selectedDate = when (selectedPeriod) {
                0 -> selectedDate.plusDays(1)
                1 -> selectedDate.plusWeeks(1)
                2 -> selectedDate.plusMonths(1)
                else -> selectedDate.plusYears(1)
            }
            renderOverview(view)
        }

        view.findViewById<View>(
            R.id.tvOverviewDate
        ).setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    selectedDate = LocalDate.of(
                        year, month + 1, day
                    )
                    renderOverview(view)
                },
                selectedDate.year,
                selectedDate.monthValue - 1,
                selectedDate.dayOfMonth
            ).show()
        }

        renderOverview(view)
    }

    private fun getPeriodRange(): Pair<LocalDate, LocalDate> {
        return when (selectedPeriod) {
            0 -> Pair(selectedDate, selectedDate)

            1 -> {
                val start = selectedDate.with(
                    TemporalAdjusters.previousOrSame(
                        DayOfWeek.MONDAY
                    )
                )
                Pair(start, start.plusDays(6))
            }

            2 -> {
                val month = YearMonth.from(selectedDate)
                Pair(month.atDay(1), month.atEndOfMonth())
            }

            else -> {
                Pair(
                    selectedDate.withDayOfYear(1),
                    selectedDate.withDayOfYear(1)
                        .plusYears(1).minusDays(1)
                )
            }
        }
    }

    private fun renderOverview(view: View) {
        val (start, end) = getPeriodRange()

        view.findViewById<TextView>(
            R.id.tvOverviewPeriod
        ).text = "${periods[selectedPeriod]}  ▾"

        val formatter = DateTimeFormatter.ofPattern(
            "MMM d, yyyy",
            Locale.ENGLISH
        )

        val dateLabel = when (selectedPeriod) {
            0 -> start.format(formatter)
            1 -> "${start.format(formatter)} - ${end.format(formatter)}"
            2 -> start.format(
                DateTimeFormatter.ofPattern(
                    "MMMM yyyy", Locale.ENGLISH
                )
            )
            else -> start.year.toString()
        }

        view.findViewById<TextView>(
            R.id.tvOverviewDate
        ).text = dateLabel

        val periodAllowances = allAllowances.filter {
            isWithinPeriod(it.date, start, end)
        }

        val periodExpenses = allExpenses.filter {
            isWithinPeriod(it.date, start, end)
        }

        val received = periodAllowances.sumOf {
            it.amountCentavos
        }

        val spent = periodExpenses.sumOf {
            it.amountCentavos
        }

        // Cumulative balance through the selected period's end.
        val accumulatedAllowance = allAllowances
            .filter { isOnOrBefore(it.date, end) }
            .sumOf { it.amountCentavos }

        val accumulatedExpenses = allExpenses
            .filter { isOnOrBefore(it.date, end) }
            .sumOf { it.amountCentavos }

        val balance = accumulatedAllowance -
                accumulatedExpenses

        val openingBalance = balance - received + spent

        view.findViewById<TextView>(
            R.id.tvTotalAllowance
        ).text = MoneyUtils.format(received)

        view.findViewById<TextView>(
            R.id.tvAllowanceSpent
        ).text = MoneyUtils.format(spent)

        view.findViewById<TextView>(
            R.id.tvAllowanceBalance
        ).text = MoneyUtils.format(balance)

        view.findViewById<TextView>(
            R.id.tvOpeningBalance
        ).text = MoneyUtils.format(openingBalance)

        view.findViewById<TextView>(
            R.id.tvAllowanceSummary
        ).text = when {
            periodAllowances.isEmpty() &&
                    periodExpenses.isEmpty() ->
                "No transactions recorded for this period."

            balance < 0L ->
                "Your recorded expenses exceed your allowance."

            balance == 0L ->
                "You have used your available allowance."

            else ->
                "Your remaining allowance carries over to the next period."
        }

        renderExpenseBreakdown(view, periodExpenses)
    }

    private fun isWithinPeriod(
        dateText: String,
        start: LocalDate,
        end: LocalDate
    ): Boolean {
        return try {
            val date = LocalDate.parse(dateText)
            !date.isBefore(start) && !date.isAfter(end)
        } catch (_: Exception) {
            false
        }
    }

    private fun isOnOrBefore(
        dateText: String,
        end: LocalDate
    ): Boolean {
        return try {
            !LocalDate.parse(dateText).isAfter(end)
        } catch (_: Exception) {
            false
        }
    }

    private fun displayCategory(category: String): String {
        return when (category) {
            "Food" -> "Pagkain"
            "Transportation" -> "Pamasahe"
            "Load/Internet" -> "Load/Data"
            "Miscellaneous" -> "Others"
            else -> category
        }
    }

    private fun renderExpenseBreakdown(
        view: View,
        expenses: List<Expense>
    ) {
        val container = view.findViewById<LinearLayout>(
            R.id.allowanceExpenseContainer
        )

        val emptyText = view.findViewById<TextView>(
            R.id.tvEmptyAllowanceExpenses
        )

        container.removeAllViews()

        emptyText.visibility =
            if (expenses.isEmpty()) View.VISIBLE else View.GONE

        if (expenses.isEmpty()) return

        val grouped = expenses.groupBy {
            displayCategory(it.category)
        }

        grouped.toSortedMap().forEach { (category, items) ->
            val total = items.sumOf {
                it.amountCentavos
            }

            val header = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(13), 0, dp(7))
            }

            val categoryText = TextView(requireContext()).apply {
                text = category
                textSize = 12f
                setTextColor(0xFF1F2937.toInt())
                typeface = resources.getFont(
                    R.font.poppins_semibold
                )
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            }

            val totalText = TextView(requireContext()).apply {
                text = "-${MoneyUtils.format(total)}"
                textSize = 12f
                setTextColor(0xFFEF4444.toInt())
                typeface = resources.getFont(
                    R.font.poppins_semibold
                )
            }

            header.addView(categoryText)
            header.addView(totalText)
            container.addView(header)

            items.sortedWith(
                compareByDescending<Expense> { it.date }
                    .thenByDescending { it.id }
            ).forEach { expense ->

                val row = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(dp(10), dp(9), dp(4), dp(9))
                }

                val details = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                }

                val description = TextView(requireContext()).apply {
                    text = expense.description.ifBlank {
                        category
                    }
                    textSize = 11f
                    setTextColor(0xFF374151.toInt())
                    typeface = resources.getFont(
                        R.font.poppins_medium
                    )
                }

                val date = TextView(requireContext()).apply {
                    text = expense.date
                    textSize = 10f
                    setTextColor(0xFF929AB2.toInt())
                    typeface = resources.getFont(
                        R.font.poppins_regular
                    )
                }

                val amount = TextView(requireContext()).apply {
                    text = "-${MoneyUtils.format(
                        expense.amountCentavos
                    )}"
                    textSize = 11f
                    setTextColor(0xFFEF4444.toInt())
                    typeface = resources.getFont(
                        R.font.poppins_medium
                    )
                }

                details.addView(description)
                details.addView(date)

                row.addView(details)
                row.addView(amount)
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

        switchTab(view, selectedTab, false)
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

        history.visibility =
            if (tab == 0) View.VISIBLE else View.GONE

        settings.visibility =
            if (tab == 1) View.VISIBLE else View.GONE

        view.findViewById<View>(
            R.id.indicatorAllowanceHistory
        ).visibility =
            if (tab == 0) View.VISIBLE else View.INVISIBLE

        view.findViewById<View>(
            R.id.indicatorAllowanceSettings
        ).visibility =
            if (tab == 1) View.VISIBLE else View.INVISIBLE

        view.findViewById<TextView>(
            R.id.tvAllowanceHistoryTab
        ).setTextColor(
            if (tab == 0) 0xFF6547ED.toInt()
            else 0xFF6B7280.toInt()
        )

        view.findViewById<TextView>(
            R.id.tvAllowanceSettingsTab
        ).setTextColor(
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

        view.findViewById<TextView>(
            R.id.tvAllowanceFrequency
        ).text = preferences.getString(
            "allowance_frequency", "Daily"
        ) ?: "Daily"
    }

    private fun showFrequencyDialog(view: View) {
        val preferences = requireContext().getSharedPreferences(
            "gastoskwela_preferences",
            Context.MODE_PRIVATE
        )

        val current = preferences.getString(
            "allowance_frequency", "Daily"
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

    private fun renderAllowances(view: View) {
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

        val sorted = allAllowances.sortedWith(
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
                gravity = Gravity.CENTER_VERTICAL
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
                gravity = Gravity.CENTER
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
                ellipsize =
                    android.text.TextUtils.TruncateAt.END
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

        var allowanceDate =
            existing?.date ?: LocalDate.now().toString()

        dateButton.text = allowanceDate

        existing?.let { allowance ->
            amountInput.setText(
                BigDecimal.valueOf(
                    allowance.amountCentavos, 2
                ).toPlainString()
            )

            sourceInput.setText(allowance.source)
            notesInput.setText(allowance.notes)
        }

        dateButton.setOnClickListener {
            val date = LocalDate.parse(allowanceDate)

            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    allowanceDate = LocalDate.of(
                        year, month + 1, day
                    ).toString()

                    dateButton.text = allowanceDate
                },
                date.year,
                date.monthValue - 1,
                date.dayOfMonth
            ).show()
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle(
                if (existing == null) "Add Allowance"
                else "Edit Allowance"
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
                    date = allowanceDate,
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

    private fun dp(value: Int): Int {
        return (
                value * resources.displayMetrics.density
                ).toInt()
    }
}