package com.clarxxinn.gastoskwela.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        updateFrequencyLabel(view)

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
                    allowanceDao.getAllAllowances().collect { allowances ->
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

    private fun updateFrequencyLabel(view: View) {
        val preferences = requireContext().getSharedPreferences(
            "gastoskwela_preferences",
            android.content.Context.MODE_PRIVATE
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
            android.content.Context.MODE_PRIVATE
        )

        val currentFrequency = preferences.getString(
            "allowance_frequency",
            "Daily"
        ) ?: "Daily"

        val selectedIndex = frequencies.indexOf(
            currentFrequency
        ).coerceAtLeast(0)

        var selected = selectedIndex

        AlertDialog.Builder(requireContext())
            .setTitle("Allowance Frequency")
            .setSingleChoiceItems(
                frequencies,
                selectedIndex
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

        emptyText.visibility = if (sorted.isEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }

        emptyText.text =
            "No allowance records yet. Tap Add Allowance to begin."

        sorted.forEach { allowance ->
            val itemView = layoutInflater.inflate(
                R.layout.item_allowance,
                container,
                false
            )

            itemView.findViewById<TextView>(
                R.id.tvSource
            ).text = allowance.source

            itemView.findViewById<TextView>(
                R.id.tvDate
            ).text = allowance.date

            itemView.findViewById<TextView>(
                R.id.tvAmount
            ).text = "+${MoneyUtils.format(allowance.amountCentavos)}"

            val notesView = itemView.findViewById<TextView>(
                R.id.tvNotes
            )

            notesView.visibility = if (allowance.notes.isBlank()) {
                View.GONE
            } else {
                View.VISIBLE
            }

            notesView.text = allowance.notes

            itemView.findViewById<View>(
                R.id.btnEdit
            ).setOnClickListener {
                showAllowanceDialog(allowance)
            }

            itemView.findViewById<View>(
                R.id.btnDelete
            ).setOnClickListener {
                confirmDelete(allowance)
            }

            container.addView(itemView)
        }
    }

    private fun showAllowanceDialog(
        existing: Allowance? = null
    ) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_allowance, null)

        val amountInput = dialogView.findViewById<TextInputEditText>(
            R.id.etAmount
        )

        val sourceInput = dialogView.findViewById<TextInputEditText>(
            R.id.etSource
        )

        val notesInput = dialogView.findViewById<TextInputEditText>(
            R.id.etNotes
        )

        val amountLayout = dialogView.findViewById<TextInputLayout>(
            R.id.layoutAmount
        )

        val sourceLayout = dialogView.findViewById<TextInputLayout>(
            R.id.layoutSource
        )

        val dateButton = dialogView.findViewById<TextView>(
            R.id.btnSelectDate
        )

        var selectedDate = existing?.date
            ?: LocalDate.now().toString()

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
                if (existing == null) "Save Allowance" else "Update",
                null
            )
            .create()

        dialog.setOnShowListener {
            dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {

                amountLayout.error = null
                sourceLayout.error = null

                val amountText = amountInput.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

                val source = sourceInput.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

                val notes = notesInput.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

                val centavos = MoneyUtils.parseCentavos(
                    amountText
                )

                var valid = true

                if (centavos == null) {
                    amountLayout.error =
                        "Enter a valid positive amount (max 2 decimals)"
                    valid = false
                }

                if (source.isBlank()) {
                    sourceLayout.error = "Source is required"
                    valid = false
                }

                if (!valid) {
                    return@setOnClickListener
                }

                val allowance = Allowance(
                    id = existing?.id ?: 0,
                    amountCentavos = centavos!!,
                    source = source,
                    date = selectedDate,
                    notes = notes
                )

                viewLifecycleOwner.lifecycleScope.launch {
                    if (existing == null) {
                        allowanceDao.insertAllowance(allowance)
                    } else {
                        allowanceDao.updateAllowance(allowance)
                    }
                }

                dialog.dismiss()
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
                    allowanceDao.deleteAllowance(allowance)
                }
            }
            .show()
    }
}