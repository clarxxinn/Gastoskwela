package com.clarxxinn.gastoskwela.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.clarxxinn.gastoskwela.R
import com.clarxxinn.gastoskwela.data.Expense
import com.clarxxinn.gastoskwela.data.GastoskwelaDatabase
import com.clarxxinn.gastoskwela.utils.MoneyUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate

class ExpensesFragment : Fragment(R.layout.fragment_expenses) {

    private val database by lazy {
        GastoskwelaDatabase.getDatabase(requireContext())
    }

    private val expenseDao by lazy {
        database.expenseDao()
    }

    private val allowanceDao by lazy {
        database.allowanceDao()
    }

    private val categories = listOf(
        "Food",
        "Transportation",
        "School Supplies",
        "Projects",
        "Load/Internet",
        "Others"
    )

    private fun formatMoney(centavos: Long): String {
        return MoneyUtils.format(centavos)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val totalText = view.findViewById<TextView>(
            R.id.tvTotalExpenses
        )

        val balanceText = view.findViewById<TextView>(
            R.id.tvRemainingBalance
        )

        val emptyText = view.findViewById<TextView>(
            R.id.tvEmptyExpenses
        )

        val container = view.findViewById<LinearLayout>(
            R.id.expenseListContainer
        )

        view.findViewById<MaterialButton>(
            R.id.btnAddExpense
        ).setOnClickListener {
            showExpenseDialog()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                launch {
                    expenseDao.getAllExpenses().collect { expenses ->
                        renderExpenses(
                            container,
                            emptyText,
                            expenses
                        )
                    }
                }

                launch {
                    combine(
                        allowanceDao.getTotalAllowance(),
                        expenseDao.getTotalExpenses()
                    ) { allowance, expenses ->
                        Pair(allowance, expenses)
                    }.collect { (allowance, expenses) ->
                        totalText.text = formatMoney(expenses)

                        balanceText.text = formatMoney(
                            allowance - expenses
                        )
                    }
                }
            }
        }
    }

    private fun renderExpenses(
        container: LinearLayout,
        emptyText: TextView,
        expenses: List<Expense>
    ) {
        container.removeAllViews()

        emptyText.visibility = if (expenses.isEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }

        expenses.forEach { expense ->
            val itemView = layoutInflater.inflate(
                R.layout.item_expense,
                container,
                false
            )

            itemView.findViewById<TextView>(
                R.id.tvExpenseCategory
            ).text = expense.category

            itemView.findViewById<TextView>(
                R.id.tvExpenseDescription
            ).text = expense.description

            itemView.findViewById<TextView>(
                R.id.tvExpenseDate
            ).text = expense.date

            itemView.findViewById<TextView>(
                R.id.tvExpenseAmount
            ).text = "-${formatMoney(expense.amountCentavos)}"

            val notesView = itemView.findViewById<TextView>(
                R.id.tvExpenseNotes
            )

            if (expense.notes.isNotBlank()) {
                notesView.visibility = View.VISIBLE
                notesView.text = expense.notes
            } else {
                notesView.visibility = View.GONE
            }

            itemView.findViewById<MaterialButton>(
                R.id.btnEditExpense
            ).setOnClickListener {
                showExpenseDialog(expense)
            }

            itemView.findViewById<MaterialButton>(
                R.id.btnDeleteExpense
            ).setOnClickListener {
                confirmDelete(expense)
            }

            container.addView(itemView)
        }
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

        val dateButton = dialogView.findViewById<MaterialButton>(
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

        // Populate fields when editing
        existing?.let { expense ->
            amountInput.setText(
                BigDecimal.valueOf(
                    expense.amountCentavos,
                    2
                ).toPlainString()
            )

            categoryInput.setText(
                expense.category,
                false
            )

            descriptionInput.setText(expense.description)
            notesInput.setText(expense.notes)
        }

        // Date Picker
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
                if (existing == null) "Save" else "Update",
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
                    ?.toString()
                    ?.trim()
                    .orEmpty()

                val category = categoryInput.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

                val description = descriptionInput.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

                val notes = notesInput.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

                // Phase 8: Reusable money validation
                val centavos = MoneyUtils.parseCentavos(
                    amountText
                )

                var valid = true

                if (centavos == null) {
                    amountLayout.error =
                        "Enter a valid positive amount (max 2 decimals)"
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

                if (!valid) {
                    return@setOnClickListener
                }

                val expense = Expense(
                    id = existing?.id ?: 0,
                    amountCentavos = centavos!!,
                    category = category,
                    description = description,
                    date = selectedDate,
                    notes = notes
                )

                viewLifecycleOwner.lifecycleScope.launch {
                    if (existing == null) {
                        expenseDao.insertExpense(expense)
                    } else {
                        expenseDao.updateExpense(expense)
                    }
                }

                dialog.dismiss()
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
                    expenseDao.deleteExpense(expense)
                }
            }
            .show()
    }
}