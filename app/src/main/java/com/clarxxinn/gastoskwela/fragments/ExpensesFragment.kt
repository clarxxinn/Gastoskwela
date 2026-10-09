package com.clarxxinn.gastoskwela.fragments

import android.content.Intent
import com.clarxxinn.gastoskwela.AddExpenseActivity
import android.app.DatePickerDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.clarxxinn.gastoskwela.R
import com.clarxxinn.gastoskwela.data.Expense
import com.clarxxinn.gastoskwela.data.GastoskwelaDatabase
import com.clarxxinn.gastoskwela.utils.MoneyUtils
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

class ExpensesFragment : Fragment(R.layout.fragment_expenses) {

    private val database by lazy {
        GastoskwelaDatabase.getDatabase(requireContext())
    }

    private val expenseDao by lazy {
        database.expenseDao()
    }

    private val categories = listOf(
        "Food",
        "Transportation",
        "School Supplies",
        "Projects",
        "Load/Internet",
        "Others"
    )

    private var allExpenses: List<Expense> = emptyList()
    private var searchQuery = ""
    private var selectedFilter = "All"

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(
            R.id.btnAddExpense
        ).setOnClickListener {
            startActivity(
                Intent(requireContext(), AddExpenseActivity::class.java)
            )
            requireActivity().overridePendingTransition(
                android.R.anim.slide_in_left,
                android.R.anim.fade_out
            )
        }

        val searchInput = view.findViewById<TextInputEditText>(
            R.id.etSearchExpenses
        )

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(
                s: CharSequence?,
                start: Int,
                count: Int,
                after: Int
            ) = Unit

            override fun onTextChanged(
                s: CharSequence?,
                start: Int,
                before: Int,
                count: Int
            ) {
                searchQuery = s?.toString()?.trim().orEmpty()
                renderExpenses(view)
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        view.findViewById<ChipGroup>(
            R.id.expenseFilterGroup
        ).setOnCheckedStateChangeListener { _, checkedIds ->

            selectedFilter = when (checkedIds.firstOrNull()) {
                R.id.chipFood -> "Food"
                R.id.chipTransportation -> "Transportation"
                R.id.chipSchool -> "School"
                R.id.chipOthers -> "Others"
                else -> "All"
            }

            renderExpenses(view)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                expenseDao.getAllExpenses().collect { expenses ->
                    allExpenses = expenses
                    renderExpenses(view)
                }
            }
        }
    }

    private fun matchesFilter(expense: Expense): Boolean {
        return when (selectedFilter) {
            "Food" ->
                expense.category == "Food" ||
                        expense.category == "Pagkain"

            "Transportation" ->
                expense.category == "Transportation" ||
                        expense.category == "Pamasahe"

            "School" ->
                expense.category in listOf(
                    "School Supplies",
                    "Projects",
                    "School"
                )

            "Others" ->
                expense.category in listOf(
                    "Load/Internet",
                    "Load/Data",
                    "Others",
                    "Miscellaneous"
                )

            else -> true
        }
    }

    private fun displayCategory(category: String): String {
        return when (category) {
            "Food" -> "Pagkain"
            "Transportation" -> "Pamasahe"
            "Load/Internet" -> "Load/Data"
            "Others" -> "Miscellaneous"
            else -> category
        }
    }

    private fun renderExpenses(view: View) {
        val container = view.findViewById<LinearLayout>(
            R.id.expenseListContainer
        )

        val emptyText = view.findViewById<TextView>(
            R.id.tvEmptyExpenses
        )

        val countText = view.findViewById<TextView>(
            R.id.tvExpenseCount
        )

        container.removeAllViews()

        val filtered = allExpenses
            .filter { expense ->
                matchesFilter(expense) &&
                        (
                                expense.description.contains(
                                    searchQuery,
                                    ignoreCase = true
                                ) ||
                                        expense.category.contains(
                                            searchQuery,
                                            ignoreCase = true
                                        ) ||
                                        displayCategory(expense.category).contains(
                                            searchQuery,
                                            ignoreCase = true
                                        ) ||
                                        expense.notes.contains(
                                            searchQuery,
                                            ignoreCase = true
                                        ) ||
                                        expense.date.contains(
                                            searchQuery,
                                            ignoreCase = true
                                        )
                                )
            }
            .sortedWith(
                compareByDescending<Expense> { it.date }
                    .thenByDescending { it.id }
            )

        countText.text = when (filtered.size) {
            1 -> "1 record"
            else -> "${filtered.size} records"
        }

        emptyText.visibility =
            if (filtered.isEmpty()) View.VISIBLE else View.GONE

        emptyText.text = if (allExpenses.isEmpty()) {
            "No expenses yet. Tap + to add your first expense."
        } else {
            "No expenses match your search or filter."
        }

        val monthFormatter = DateTimeFormatter.ofPattern(
            "MMMM yyyy",
            Locale.ENGLISH
        )

        val dateFormatter = DateTimeFormatter.ofPattern(
            "MMM d, yyyy",
            Locale.ENGLISH
        )

        var lastMonth = ""

        filtered.forEach { expense ->
            val monthKey = expense.date.take(7)

            if (monthKey != lastMonth) {
                lastMonth = monthKey

                val heading = TextView(requireContext()).apply {
                    text = try {
                        YearMonth.parse(monthKey)
                            .format(monthFormatter)
                    } catch (_: Exception) {
                        monthKey
                    }

                    textSize = 13f
                    setTextColor(0xFF777C98.toInt())
                    typeface = resources.getFont(
                        R.font.poppins_medium
                    )
                    setPadding(2, dp(15), 0, dp(11))
                }

                container.addView(heading)
            }

            val itemView = layoutInflater.inflate(
                R.layout.item_expense,
                container,
                false
            )

            val (icon, color) = when (expense.category) {
                "Food", "Pagkain" ->
                    "🍴" to 0xFFFF8A20.toInt()

                "Transportation", "Pamasahe" ->
                    "🚌" to 0xFF397CF5.toInt()

                "School Supplies", "School" ->
                    "📘" to 0xFF4AA2F8.toInt()

                "Projects" ->
                    "📋" to 0xFF8C67F1.toInt()

                "Load/Internet", "Load/Data" ->
                    "📱" to 0xFF1AB5B2.toInt()

                else ->
                    "●" to 0xFF777777.toInt()
            }

            val iconView = itemView.findViewById<TextView>(
                R.id.tvExpenseIcon
            )

            iconView.text = icon

            val circle = itemView.findViewById<View>(
                R.id.expenseIconBackground
            )

            circle.background.mutate().setTint(color)

            itemView.findViewById<TextView>(
                R.id.tvExpenseCategory
            ).text = displayCategory(expense.category)

            itemView.findViewById<TextView>(
                R.id.tvExpenseDate
            ).text = try {
                LocalDate.parse(expense.date).format(dateFormatter)
            } catch (_: Exception) {
                expense.date
            }

            itemView.findViewById<TextView>(
                R.id.tvExpenseAmount
            ).text = "-${MoneyUtils.format(expense.amountCentavos)}"

            // Tap to edit, long-press to delete.
            itemView.findViewById<View>(
                R.id.expenseRow
            ).apply {
                setOnClickListener {
                    showExpenseDialog(expense)
                }

                setOnLongClickListener {
                    confirmDelete(expense)
                    true
                }
            }

            container.addView(itemView)
        }
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun showExpenseDialog(existing: Expense? = null) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_expense, null)

        val amountInput = dialogView.findViewById<TextInputEditText>(
            R.id.etExpenseAmount
        )

        val categoryInput = dialogView.findViewById<AutoCompleteTextView>(
            R.id.etExpenseCategory
        )

        val descriptionInput = dialogView.findViewById<TextInputEditText>(
            R.id.etExpenseDescription
        )

        val notesInput = dialogView.findViewById<TextInputEditText>(
            R.id.etExpenseNotes
        )

        val amountLayout = dialogView.findViewById<TextInputLayout>(
            R.id.layoutExpenseAmount
        )

        val categoryLayout = dialogView.findViewById<TextInputLayout>(
            R.id.layoutExpenseCategory
        )

        val descriptionLayout = dialogView.findViewById<TextInputLayout>(
            R.id.layoutExpenseDescription
        )

        val dateButton = dialogView.findViewById<TextView>(
            R.id.btnExpenseDate
        )

        categoryInput.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                categories
            )
        )

        categoryInput.setOnClickListener {
            categoryInput.showDropDown()
        }

        var selectedDate = existing?.date
            ?: LocalDate.now().toString()

        dateButton.text = selectedDate

        existing?.let { expense ->
            amountInput.setText(
                BigDecimal.valueOf(
                    expense.amountCentavos,
                    2
                ).toPlainString()
            )

            categoryInput.setText(expense.category, false)
            descriptionInput.setText(expense.description)
            notesInput.setText(expense.notes)
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
                    "Add Expense"
                } else {
                    "Edit Expense"
                }
            )
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton(
                if (existing == null) "Save Expense" else "Update",
                null
            )
            .create()

        dialog.setOnShowListener {
            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                amountLayout.error = null
                categoryLayout.error = null
                descriptionLayout.error = null

                val amountText = amountInput.text
                    ?.toString()?.trim().orEmpty()

                val category = categoryInput.text
                    ?.toString()?.trim().orEmpty()

                val description = descriptionInput.text
                    ?.toString()?.trim().orEmpty()

                val notes = notesInput.text
                    ?.toString()?.trim().orEmpty()

                val centavos = MoneyUtils.parseCentavos(
                    amountText
                )

                var valid = true

                if (centavos == null || centavos <= 0L) {
                    amountLayout.error =
                        "Enter a valid positive amount"
                    valid = false
                }

                if (category !in categories) {
                    categoryLayout.error =
                        "Select a valid category"
                    valid = false
                }

                if (description.isBlank()) {
                    descriptionLayout.error =
                        "Description is required"
                    valid = false
                }

                if (!valid) return@setOnClickListener

                val expense = Expense(
                    id = existing?.id ?: 0,
                    amountCentavos = centavos!!,
                    category = category,
                    description = description,
                    date = selectedDate,
                    notes = notes
                )

                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        if (existing == null) {
                            expenseDao.insertExpense(expense)
                        } else {
                            expenseDao.updateExpense(expense)
                        }
                        dialog.dismiss()
                    } catch (_: Exception) {
                        Toast.makeText(
                            requireContext(),
                            "Unable to save expense.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun confirmDelete(expense: Expense) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Expense?")
            .setMessage(
                "Are you sure you want to delete this expense record?"
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        expenseDao.deleteExpense(expense)
                    } catch (_: Exception) {
                        Toast.makeText(
                            requireContext(),
                            "Unable to delete expense.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .show()
    }
}