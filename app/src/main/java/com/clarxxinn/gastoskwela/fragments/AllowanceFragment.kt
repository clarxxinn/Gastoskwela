package com.clarxxinn.gastoskwela.fragments

import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.res.ResourcesCompat
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

    private var selectedTab = 0
    private var selectedPeriod = "Daily"
    private var selectedDate = LocalDate.now()

    private var allowances = emptyList<Allowance>()
    private var expenses = emptyList<Expense>()

    private val periods = arrayOf(
        "Daily",
        "Weekly",
        "Monthly",
        "Yearly"
    )

    private val purple = Color.rgb(101, 71, 237)
    private val green = Color.rgb(22, 163, 74)
    private val red = Color.rgb(220, 38, 38)
    private val dark = Color.rgb(31, 41, 55)
    private val muted = Color.rgb(146, 154, 178)

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        selectedTab = savedInstanceState
            ?.getInt("selectedTab", 0) ?: 0

        selectedPeriod = savedInstanceState
            ?.getString("selectedPeriod")
            ?.takeIf { it in periods }
            ?: "Daily"

        selectedDate = savedInstanceState
            ?.getString("selectedDate")
            ?.let {
                runCatching {
                    LocalDate.parse(it)
                }.getOrNull()
            }
            ?: LocalDate.now()

        setupTabs(view)
        setupHistoryControls(view)

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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                combine(
                    allowanceDao.getAllAllowances(),
                    expenseDao.getAllExpenses()
                ) { allowanceList, expenseList ->
                    allowanceList to expenseList
                }.collect { (allowanceList, expenseList) ->

                    allowances = allowanceList
                    expenses = expenseList

                    renderBalance(view)
                    renderQuickSummary(view)
                    renderAllowanceRecords(view)
                    renderHistory(view)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("selectedTab", selectedTab)
        outState.putString("selectedPeriod", selectedPeriod)
        outState.putString(
            "selectedDate",
            selectedDate.toString()
        )

        super.onSaveInstanceState(outState)
    }

    // =====================================================
    // OVERVIEW / HISTORY TABS
    // =====================================================

    private fun setupTabs(view: View) {

        view.findViewById<View>(
            R.id.tabAllowanceOverview
        ).setOnClickListener {
            switchTab(view, 0)
        }

        view.findViewById<View>(
            R.id.tabAllowanceHistory
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

        val overview = view.findViewById<View>(
            R.id.allowanceOverviewSection
        )

        val history = view.findViewById<View>(
            R.id.allowanceHistorySection
        )

        overview.visibility =
            if (tab == 0) View.VISIBLE else View.GONE

        history.visibility =
            if (tab == 1) View.VISIBLE else View.GONE

        view.findViewById<View>(
            R.id.indicatorAllowanceOverview
        ).visibility =
            if (tab == 0) View.VISIBLE else View.INVISIBLE

        view.findViewById<View>(
            R.id.indicatorAllowanceHistory
        ).visibility =
            if (tab == 1) View.VISIBLE else View.INVISIBLE

        view.findViewById<TextView>(
            R.id.tvAllowanceOverviewTab
        ).setTextColor(
            if (tab == 0) purple else muted
        )

        view.findViewById<TextView>(
            R.id.tvAllowanceHistoryTab
        ).setTextColor(
            if (tab == 1) purple else muted
        )

        if (animate) {
            val active = if (tab == 0) {
                overview
            } else {
                history
            }

            active.alpha = 0f
            active.translationY = dp(8).toFloat()

            active.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(180)
                .start()
        }
    }

    // =====================================================
    // CURRENT BALANCE / TOTAL RECEIVED / TOTAL SPENT
    // =====================================================

    private fun renderBalance(view: View) {

        val today = LocalDate.now()

        val received = allowances
            .filter {
                onOrBefore(it.date, today)
            }
            .sumOf {
                it.amountCentavos
            }

        val spent = expenses
            .filter {
                onOrBefore(it.date, today)
            }
            .sumOf {
                it.amountCentavos
            }

        val balance = received - spent

        view.findViewById<TextView>(
            R.id.tvAllowanceBalance
        ).text = MoneyUtils.format(balance)

        view.findViewById<TextView>(
            R.id.tvTotalAllowance
        ).text = MoneyUtils.format(received)

        view.findViewById<TextView>(
            R.id.tvAllowanceSpent
        ).text = MoneyUtils.format(spent)
    }

    // =====================================================
    // FINANCIAL SUMMARY - 2 X 2 GRID
    // =====================================================

    private fun renderQuickSummary(view: View) {

        val today = LocalDate.now()

        val currentMonday = today.with(
            TemporalAdjusters.previousOrSame(
                DayOfWeek.MONDAY
            )
        )

        val previousMonth = YearMonth
            .from(today)
            .minusMonths(1)

        val previousYear = today.year - 1

        val ranges = listOf(
            Triple("Today", today, today),
            Triple(
                "Last Week",
                currentMonday.minusWeeks(1),
                currentMonday.minusDays(1)
            ),
            Triple(
                "Last Month",
                previousMonth.atDay(1),
                previousMonth.atEndOfMonth()
            ),
            Triple(
                "Last Year",
                LocalDate.of(previousYear, 1, 1),
                LocalDate.of(previousYear, 12, 31)
            )
        )

        val container = view.findViewById<GridLayout>(
            R.id.allowanceQuickSummary
        )

        container.removeAllViews()
        container.columnCount = 2
        container.orientation = GridLayout.HORIZONTAL

        ranges.forEach { (title, start, end) ->

            val received = allowances
                .filter {
                    inRange(it.date, start, end)
                }
                .sumOf {
                    it.amountCentavos
                }

            val spent = expenses
                .filter {
                    inRange(it.date, start, end)
                }
                .sumOf {
                    it.amountCentavos
                }

            val card = LinearLayout(
                requireContext()
            ).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(
                    dp(12), dp(12), dp(12), dp(12)
                )
                background = rounded(
                    Color.WHITE, 14, "#E9EDF5"
                )
                minimumHeight = dp(100)
            }

            card.addView(
                label(title, 12f, dark, true)
            )

            val receivedRow = LinearLayout(
                requireContext()
            ).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            receivedRow.addView(
                iconView(
                    R.drawable.ic_allowance_income, 18
                )
            )

            receivedRow.addView(
                label(
                    MoneyUtils.format(received),
                    12f, green, true
                )
            )

            val spentRow = LinearLayout(
                requireContext()
            ).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            spentRow.addView(
                iconView(
                    R.drawable.ic_allowance_spent, 18
                )
            )

            spentRow.addView(
                label(
                    MoneyUtils.format(spent),
                    12f, red, true
                )
            )

            card.addView(
                receivedRow,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(7)
                }
            )

            card.addView(
                spentRow,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    topMargin = dp(4)
                }
            )

            val params = GridLayout.LayoutParams().apply {
                width = 0
                height = GridLayout.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(
                    GridLayout.UNDEFINED, 1, 1f
                )
                setMargins(
                    dp(4), dp(4), dp(4), dp(4)
                )
            }

            container.addView(card, params)
        }
    }

    // =====================================================
    // HISTORY CONTROLS
    // =====================================================

    private fun setupHistoryControls(view: View) {

        view.findViewById<TextView>(
            R.id.btnOverviewPeriod
        ).setOnClickListener {

            AlertDialog.Builder(requireContext())
                .setTitle("Select Period")
                .setSingleChoiceItems(
                    periods,
                    periods.indexOf(selectedPeriod)
                ) { dialog, index ->
                    selectedPeriod = periods[index]
                    renderHistory(view)
                    dialog.dismiss()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        view.findViewById<View>(
            R.id.btnOverviewPrevious
        ).setOnClickListener {
            selectedDate = shiftDate(-1)
            renderHistory(view)
        }

        view.findViewById<View>(
            R.id.btnOverviewNext
        ).setOnClickListener {
            selectedDate = shiftDate(1)
            renderHistory(view)
        }

        view.findViewById<View>(
            R.id.btnOverviewDate
        ).setOnClickListener {
            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    selectedDate = LocalDate.of(
                        year, month + 1, day
                    )
                    renderHistory(view)
                },
                selectedDate.year,
                selectedDate.monthValue - 1,
                selectedDate.dayOfMonth
            ).show()
        }
    }

    private fun shiftDate(direction: Long): LocalDate {
        return when (selectedPeriod) {
            "Weekly" -> selectedDate.plusWeeks(direction)
            "Monthly" -> selectedDate.plusMonths(direction)
            "Yearly" -> selectedDate.plusYears(direction)
            else -> selectedDate.plusDays(direction)
        }
    }

    private fun historyRange(): Pair<LocalDate, LocalDate> {
        return when (selectedPeriod) {
            "Weekly" -> {
                val monday = selectedDate.with(
                    TemporalAdjusters.previousOrSame(
                        DayOfWeek.MONDAY
                    )
                )
                monday to monday.plusDays(6)
            }

            "Monthly" -> {
                val month = YearMonth.from(selectedDate)
                month.atDay(1) to month.atEndOfMonth()
            }

            "Yearly" -> {
                LocalDate.of(selectedDate.year, 1, 1) to
                        LocalDate.of(selectedDate.year, 12, 31)
            }

            else -> selectedDate to selectedDate
        }
    }

    // =====================================================
    // FINANCIAL HISTORY
    // =====================================================

    private fun renderHistory(view: View) {

        val (start, end) = historyRange()

        val dateFormatter = DateTimeFormatter.ofPattern(
            "MMM d, yyyy",
            Locale.ENGLISH
        )

        val dateText = when (selectedPeriod) {
            "Weekly" -> {
                "${start.format(dateFormatter)} - " +
                        end.format(dateFormatter)
            }

            "Monthly" -> {
                start.format(
                    DateTimeFormatter.ofPattern(
                        "MMMM yyyy", Locale.ENGLISH
                    )
                )
            }

            "Yearly" -> start.year.toString()

            else -> start.format(dateFormatter)
        }

        view.findViewById<TextView>(
            R.id.btnOverviewPeriod
        ).text = "$selectedPeriod  ⌄"

        view.findViewById<TextView>(
            R.id.btnOverviewDate
        ).text = dateText

        val received = allowances
            .filter { inRange(it.date, start, end) }
            .sumOf { it.amountCentavos }

        val periodExpenses = expenses.filter {
            inRange(it.date, start, end)
        }

        val spent = periodExpenses.sumOf {
            it.amountCentavos
        }

        val receivedToDate = allowances
            .filter { onOrBefore(it.date, end) }
            .sumOf { it.amountCentavos }

        val spentToDate = expenses
            .filter { onOrBefore(it.date, end) }
            .sumOf { it.amountCentavos }

        val remaining = receivedToDate - spentToDate
        val opening = remaining - received + spent

        view.findViewById<TextView>(
            R.id.tvHistoryBalance
        ).text = MoneyUtils.format(remaining)

        view.findViewById<TextView>(
            R.id.tvOverviewOpening
        ).text = "Opening balance: ${MoneyUtils.format(opening)}"

        view.findViewById<TextView>(
            R.id.tvHistoryReceived
        ).text = MoneyUtils.format(received)

        view.findViewById<TextView>(
            R.id.tvHistorySpent
        ).text = MoneyUtils.format(spent)

        renderExpenseBreakdown(view, periodExpenses)
    }

    // =====================================================
    // EXPENSE BREAKDOWN - ICONS BESIDE CATEGORY HEADERS
    // =====================================================

    private fun renderExpenseBreakdown(
        view: View,
        periodExpenses: List<Expense>
    ) {

        val container = view.findViewById<LinearLayout>(
            R.id.overviewExpenseList
        )

        val empty = view.findViewById<TextView>(
            R.id.tvOverviewEmpty
        )

        container.removeAllViews()

        empty.visibility =
            if (periodExpenses.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }

        val grouped = periodExpenses
            .sortedWith(
                compareByDescending<Expense> {
                    it.date
                }.thenByDescending {
                    it.id
                }
            )
            .groupBy {
                categoryName(it.category)
            }

        grouped.forEach { (category, items) ->

            val total = items.sumOf {
                it.amountCentavos
            }

            // CATEGORY HEADER
            val categoryHeader = LinearLayout(
                requireContext()
            ).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(
                    dp(4), dp(12), dp(4), dp(9)
                )
            }

            // Same icon size and style as Home Recent Expenses
            val headerIcon = ImageView(
                requireContext()
            ).apply {
                setImageResource(
                    expenseCategoryIcon(category)
                )
                scaleType = ImageView.ScaleType.FIT_CENTER
                setPadding(0, 0, 0, 0)
            }

            categoryHeader.addView(
                headerIcon,
                LinearLayout.LayoutParams(
                    dp(40), dp(40)
                ).apply {
                    marginEnd = dp(10)
                }
            )

            categoryHeader.addView(
                label(
                    category,
                    13f,
                    dark,
                    true
                ),
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )

            categoryHeader.addView(
                label(
                    "-${MoneyUtils.format(total)}",
                    12f,
                    red,
                    true
                )
            )

            container.addView(categoryHeader)

            // EXPENSE RECORDS (NO ICON)
            items.forEach { expense ->

                val row = LinearLayout(
                    requireContext()
                ).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL

                    setPadding(
                        dp(13), dp(12), dp(13), dp(12)
                    )

                    background = rounded(
                        Color.WHITE,
                        12,
                        "#EEF0F7"
                    )
                }

                val details = LinearLayout(
                    requireContext()
                ).apply {
                    orientation = LinearLayout.VERTICAL
                }

                details.addView(
                    label(
                        expense.description.ifBlank {
                            category
                        },
                        12f,
                        dark,
                        true
                    )
                )

                details.addView(
                    label(
                        expense.date,
                        10f,
                        muted,
                        false
                    )
                )

                row.addView(
                    details,
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                )

                row.addView(
                    label(
                        "-${MoneyUtils.format(expense.amountCentavos)}",
                        12f,
                        red,
                        true
                    )
                )

                container.addView(
                    row,
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(7)
                    }
                )
            }
        }
    }

    private fun expenseCategoryIcon(category: String): Int {
        return when (category) {

            "Load/Data", "Load/Internet" ->
                R.drawable.ic_expense_load

            "Pamasahe", "Transportation" ->
                R.drawable.ic_expense_transport

            "Others", "Miscellaneous" ->
                R.drawable.ic_expense_misc

            "Groceries" ->
                R.drawable.ic_expense_groceries

            "Subscriptions" ->
                R.drawable.ic_expense_subscriptions

            "Pagkain", "Food" ->
                R.drawable.ic_expense_food

            else ->
                R.drawable.ic_expense_misc
        }
    }

    private fun categoryName(category: String): String {
        return when (category) {
            "Food" -> "Pagkain"
            "Transportation" -> "Pamasahe"
            "Load/Internet" -> "Load/Data"
            "Miscellaneous" -> "Others"
            else -> category
        }
    }

    // =====================================================
    // RECENT ALLOWANCE - PURPLE WALLET ICON
    // =====================================================

    private fun renderAllowanceRecords(view: View) {

        val container = view.findViewById<LinearLayout>(
            R.id.allowanceListContainer
        )

        val empty = view.findViewById<TextView>(
            R.id.tvEmptyAllowance
        )

        val count = view.findViewById<TextView>(
            R.id.tvAllowanceCount
        )

        container.removeAllViews()

        val sorted = allowances.sortedWith(
            compareByDescending<Allowance> {
                it.date
            }.thenByDescending {
                it.id
            }
        )

        count.text = "${sorted.size} records"

        empty.visibility =
            if (sorted.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }

        sorted.forEach { allowance ->

            val row = LinearLayout(
                requireContext()
            ).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                setPadding(
                    dp(12), dp(12), dp(12), dp(12)
                )

                background = rounded(
                    Color.WHITE,
                    14,
                    "#EDF0F7"
                )

                isClickable = true
                isFocusable = true
            }

            val iconContainer = LinearLayout(
                requireContext()
            ).apply {
                gravity = Gravity.CENTER
                background = rounded(
                    Color.parseColor("#F0EEFF"),
                    12,
                    "#F0EEFF"
                )
            }

            iconContainer.addView(
                iconView(
                    R.drawable.ic_allowance_record,
                    24
                ),
                LinearLayout.LayoutParams(
                    dp(24), dp(24)
                )
            )

            row.addView(
                iconContainer,
                LinearLayout.LayoutParams(
                    dp(42), dp(42)
                ).apply {
                    marginEnd = dp(10)
                }
            )

            val details = LinearLayout(
                requireContext()
            ).apply {
                orientation = LinearLayout.VERTICAL
            }

            details.addView(
                label(
                    allowance.source,
                    12f,
                    dark,
                    true
                )
            )

            details.addView(
                label(
                    allowance.date,
                    10f,
                    muted,
                    false
                )
            )

            row.addView(
                details,
                LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
                )
            )

            row.addView(
                label(
                    "+${MoneyUtils.format(allowance.amountCentavos)}",
                    12f,
                    green,
                    true
                )
            )

            row.setOnClickListener {
                showAllowanceDialog(allowance)
            }

            row.setOnLongClickListener {
                confirmDelete(allowance)
                true
            }

            container.addView(
                row,
                LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = dp(8)
                }
            )
        }
    }

    // =====================================================
    // ADD / EDIT ALLOWANCE
    // =====================================================

    private fun showAllowanceDialog(
        existing: Allowance? = null
    ) {

        val dialogView = LayoutInflater.from(
            requireContext()
        ).inflate(
            R.layout.dialog_allowance,
            null
        )

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

        val dateButton =
            dialogView.findViewById<TextView>(
                R.id.btnSelectDate
            )

        var allowanceDate =
            existing?.date ?: LocalDate.now().toString()

        dateButton.text = allowanceDate

        existing?.let {
            amountInput.setText(
                BigDecimal.valueOf(
                    it.amountCentavos, 2
                ).toPlainString()
            )
            sourceInput.setText(it.source)
            notesInput.setText(it.notes)
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

        val dialog = AlertDialog.Builder(
            requireContext()
        )
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
                if (existing == null) "Save" else "Update",
                null
            )
            .create()

        dialog.setOnShowListener {
            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {
                amountLayout.error = null
                sourceLayout.error = null

                val amount = MoneyUtils.parseCentavos(
                    amountInput.text?.toString().orEmpty()
                )

                val source = sourceInput.text
                    ?.toString()?.trim().orEmpty()

                val notes = notesInput.text
                    ?.toString()?.trim().orEmpty()

                var valid = true

                if (amount == null || amount <= 0L) {
                    amountLayout.error =
                        "Enter a valid positive amount"
                    valid = false
                }

                if (source.isBlank()) {
                    sourceLayout.error = "Source is required"
                    valid = false
                }

                if (!valid) return@setOnClickListener

                val record = Allowance(
                    id = existing?.id ?: 0,
                    amountCentavos = amount!!,
                    source = source,
                    date = allowanceDate,
                    notes = notes
                )

                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        if (existing == null) {
                            allowanceDao.insertAllowance(record)
                        } else {
                            allowanceDao.updateAllowance(record)
                        }
                        dialog.dismiss()
                    } catch (e: Exception) {
                        Toast.makeText(
                            requireContext(),
                            "Unable to save allowance",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        dialog.show()
    }

    // =====================================================
    // DELETE ALLOWANCE
    // =====================================================

    private fun confirmDelete(allowance: Allowance) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Allowance?")
            .setMessage("Delete this allowance record?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        allowanceDao.deleteAllowance(allowance)
                    } catch (e: Exception) {
                        Toast.makeText(
                            requireContext(),
                            "Unable to delete allowance",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
            .show()
    }

    // =====================================================
    // UI HELPERS - POPPINS FONT
    // =====================================================

    private fun label(
        value: String,
        size: Float,
        color: Int,
        bold: Boolean
    ): TextView {
        return TextView(requireContext()).apply {
            text = value
            textSize = size
            setTextColor(color)

            typeface = ResourcesCompat.getFont(
                requireContext(),
                if (bold) {
                    R.font.poppins_semibold
                } else {
                    R.font.poppins_regular
                }
            )

            setPadding(
                dp(3), dp(3), dp(3), dp(3)
            )
        }
    }

    private fun iconView(
        drawableId: Int,
        size: Int
    ): ImageView {
        return ImageView(requireContext()).apply {
            setImageResource(drawableId)
            scaleType = ImageView.ScaleType.FIT_CENTER
            setPadding(
                dp(3), dp(3), dp(3), dp(3)
            )
            layoutParams = LinearLayout.LayoutParams(
                dp(size), dp(size)
            )
        }
    }

    private fun rounded(
        color: Int,
        radius: Int,
        stroke: String
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            setStroke(
                dp(1),
                Color.parseColor(stroke)
            )
        }
    }

    // =====================================================
    // DATE HELPERS
    // =====================================================

    private fun inRange(
        date: String,
        start: LocalDate,
        end: LocalDate
    ): Boolean {
        return runCatching {
            val parsed = LocalDate.parse(date)
            !parsed.isBefore(start) &&
                    !parsed.isAfter(end)
        }.getOrDefault(false)
    }

    private fun onOrBefore(
        date: String,
        end: LocalDate
    ): Boolean {
        return runCatching {
            !LocalDate.parse(date).isAfter(end)
        }.getOrDefault(false)
    }

    private fun dp(value: Int): Int {
        return (
                value * resources.displayMetrics.density
                ).toInt()
    }
}