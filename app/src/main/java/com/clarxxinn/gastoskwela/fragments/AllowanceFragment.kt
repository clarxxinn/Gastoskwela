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
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate

class AllowanceFragment : Fragment(R.layout.fragment_allowance) {

    private val allowanceDao by lazy {
        GastoskwelaDatabase.getDatabase(requireContext()).allowanceDao()
    }

    private fun formatMoney(centavos: Long): String {
        return MoneyUtils.format(centavos)
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        val totalText = view.findViewById<TextView>(
            R.id.tvTotalAllowance
        )

        val emptyText = view.findViewById<TextView>(
            R.id.tvEmptyAllowance
        )

        val listContainer = view.findViewById<LinearLayout>(
            R.id.allowanceListContainer
        )

        val addButton = view.findViewById<MaterialButton>(
            R.id.btnAddAllowance
        )

        addButton.setOnClickListener {
            showAllowanceDialog()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                launch {
                    allowanceDao.getAllAllowances().collect { allowances ->
                        renderAllowances(
                            listContainer,
                            emptyText,
                            allowances
                        )
                    }
                }

                launch {
                    allowanceDao.getTotalAllowance().collect { total ->
                        totalText.text = formatMoney(total)
                    }
                }
            }
        }
    }

    private fun renderAllowances(
        container: LinearLayout,
        emptyText: TextView,
        allowances: List<Allowance>
    ) {
        container.removeAllViews()

        emptyText.visibility = if (allowances.isEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }

        allowances.forEach { allowance ->
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
            ).text = formatMoney(allowance.amountCentavos)

            val notesView = itemView.findViewById<TextView>(
                R.id.tvNotes
            )

            if (allowance.notes.isNotBlank()) {
                notesView.visibility = View.VISIBLE
                notesView.text = allowance.notes
            } else {
                notesView.visibility = View.GONE
            }

            itemView.findViewById<MaterialButton>(
                R.id.btnEdit
            ).setOnClickListener {
                showAllowanceDialog(allowance)
            }

            itemView.findViewById<MaterialButton>(
                R.id.btnDelete
            ).setOnClickListener {
                confirmDelete(allowance)
            }

            container.addView(itemView)
        }
    }

    private fun showAllowanceDialog(existing: Allowance? = null) {
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

        val dateButton = dialogView.findViewById<MaterialButton>(
            R.id.btnSelectDate
        )

        var selectedDate = existing?.date
            ?: LocalDate.now().toString()

        dateButton.text = selectedDate

        // Populate fields when editing
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

        // Date picker
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