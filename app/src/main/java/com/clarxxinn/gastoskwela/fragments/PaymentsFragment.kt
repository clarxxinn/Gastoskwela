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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.clarxxinn.gastoskwela.R
import com.clarxxinn.gastoskwela.data.GastoskwelaDatabase
import com.clarxxinn.gastoskwela.data.SchoolPayment
import com.clarxxinn.gastoskwela.utils.MoneyUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate

class PaymentsFragment : Fragment(R.layout.fragment_payments) {

    private val paymentDao by lazy {
        GastoskwelaDatabase.getDatabase(requireContext())
            .schoolPaymentDao()
    }

    private val categories = listOf(
        "Tuition",
        "Projects",
        "School Supplies",
        "School Fees",
        "Events",
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

        val totalDue = view.findViewById<TextView>(
            R.id.tvTotalPaymentDue
        )

        val totalPaid = view.findViewById<TextView>(
            R.id.tvTotalPaymentPaid
        )

        val outstanding = view.findViewById<TextView>(
            R.id.tvTotalPaymentOutstanding
        )

        val emptyText = view.findViewById<TextView>(
            R.id.tvEmptyPayments
        )

        val container = view.findViewById<LinearLayout>(
            R.id.paymentListContainer
        )

        view.findViewById<MaterialButton>(
            R.id.btnAddPayment
        ).setOnClickListener {
            showPaymentDialog()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                launch {
                    paymentDao.getAllPayments().collect { payments ->
                        renderPayments(
                            container,
                            emptyText,
                            payments
                        )
                    }
                }

                launch {
                    paymentDao.getTotalDue().collect { amount ->
                        totalDue.text = formatMoney(amount)
                    }
                }

                launch {
                    paymentDao.getTotalPaid().collect { amount ->
                        totalPaid.text = formatMoney(amount)
                    }
                }

                launch {
                    paymentDao.getTotalOutstanding().collect { amount ->
                        outstanding.text = formatMoney(amount)
                    }
                }
            }
        }
    }

    private fun renderPayments(
        container: LinearLayout,
        emptyText: TextView,
        payments: List<SchoolPayment>
    ) {
        container.removeAllViews()

        emptyText.visibility = if (payments.isEmpty()) {
            View.VISIBLE
        } else {
            View.GONE
        }

        val today = LocalDate.now().toString()

        payments.forEach { payment ->
            val itemView = layoutInflater.inflate(
                R.layout.item_payment,
                container,
                false
            )

            itemView.findViewById<TextView>(
                R.id.tvPaymentTitle
            ).text = payment.title

            itemView.findViewById<TextView>(
                R.id.tvPaymentCategory
            ).text = payment.category

            // Phase 8: Overdue highlighting
            val isOverdue =
                payment.remainingCentavos > 0L &&
                        payment.dueDate < today

            val dueDateView = itemView.findViewById<TextView>(
                R.id.tvPaymentDueDate
            )

            dueDateView.text = if (isOverdue) {
                "Overdue since: ${payment.dueDate}"
            } else {
                "Due: ${payment.dueDate}"
            }

            val statusView = itemView.findViewById<TextView>(
                R.id.tvPaymentStatus
            )

            statusView.text = if (isOverdue) {
                "Overdue · ${payment.status}"
            } else {
                payment.status
            }

            val statusColor = when {
                isOverdue -> R.color.expense_red
                payment.status == "Paid" -> R.color.success_green
                else -> R.color.purple_primary
            }

            statusView.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    statusColor
                )
            )

            itemView.findViewById<TextView>(
                R.id.tvPaymentAmount
            ).text = "Total: ${formatMoney(payment.amountCentavos)}"

            itemView.findViewById<TextView>(
                R.id.tvPaymentPaid
            ).text = "Paid: ${formatMoney(payment.paidCentavos)}"

            itemView.findViewById<TextView>(
                R.id.tvPaymentRemaining
            ).text = "Remaining: ${formatMoney(payment.remainingCentavos)}"

            val notesView = itemView.findViewById<TextView>(
                R.id.tvPaymentNotes
            )

            if (payment.notes.isNotBlank()) {
                notesView.visibility = View.VISIBLE
                notesView.text = payment.notes
            } else {
                notesView.visibility = View.GONE
            }

            itemView.findViewById<MaterialButton>(
                R.id.btnEditPayment
            ).setOnClickListener {
                showPaymentDialog(payment)
            }

            itemView.findViewById<MaterialButton>(
                R.id.btnDeletePayment
            ).setOnClickListener {
                confirmDelete(payment)
            }

            container.addView(itemView)
        }
    }

    private fun showPaymentDialog(
        existing: SchoolPayment? = null
    ) {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_payment, null)

        val titleInput = dialogView.findViewById<TextInputEditText>(
            R.id.etPaymentTitle
        )

        val categoryInput = dialogView.findViewById<AutoCompleteTextView>(
            R.id.etPaymentCategory
        )

        val amountInput = dialogView.findViewById<TextInputEditText>(
            R.id.etPaymentAmount
        )

        val paidInput = dialogView.findViewById<TextInputEditText>(
            R.id.etPaymentPaid
        )

        val notesInput = dialogView.findViewById<TextInputEditText>(
            R.id.etPaymentNotes
        )

        val titleLayout = dialogView.findViewById<TextInputLayout>(
            R.id.layoutPaymentTitle
        )

        val categoryLayout = dialogView.findViewById<TextInputLayout>(
            R.id.layoutPaymentCategory
        )

        val amountLayout = dialogView.findViewById<TextInputLayout>(
            R.id.layoutPaymentAmount
        )

        val paidLayout = dialogView.findViewById<TextInputLayout>(
            R.id.layoutPaymentPaid
        )

        val dateButton = dialogView.findViewById<MaterialButton>(
            R.id.btnPaymentDueDate
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

        var selectedDate = existing?.dueDate
            ?: LocalDate.now().toString()

        dateButton.text = selectedDate

        // Populate fields when editing
        existing?.let { payment ->
            titleInput.setText(payment.title)

            categoryInput.setText(
                payment.category,
                false
            )

            amountInput.setText(
                BigDecimal.valueOf(
                    payment.amountCentavos,
                    2
                ).toPlainString()
            )

            paidInput.setText(
                BigDecimal.valueOf(
                    payment.paidCentavos,
                    2
                ).toPlainString()
            )

            notesInput.setText(payment.notes)
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
                    "Add School Payment"
                } else {
                    "Edit School Payment"
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

                titleLayout.error = null
                categoryLayout.error = null
                amountLayout.error = null
                paidLayout.error = null

                val title = titleInput.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

                val category = categoryInput.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

                val notes = notesInput.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

                // Phase 8: Reusable money validation
                val amount = MoneyUtils.parseCentavos(
                    amountInput.text?.toString().orEmpty()
                )

                val paid = MoneyUtils.parseCentavos(
                    paidInput.text?.toString()
                        ?.ifBlank { "0" }
                        .orEmpty(),
                    allowZero = true
                )

                var valid = true

                if (title.isBlank()) {
                    titleLayout.error = "Title is required"
                    valid = false
                }

                if (category !in categories) {
                    categoryLayout.error =
                        "Select a valid category"
                    valid = false
                }

                if (amount == null) {
                    amountLayout.error =
                        "Enter a valid positive amount (max 2 decimals)"
                    valid = false
                }

                if (
                    paid == null ||
                    (amount != null && paid > amount)
                ) {
                    paidLayout.error =
                        "Paid amount must be between 0 and total amount"
                    valid = false
                }

                if (!valid) {
                    return@setOnClickListener
                }

                val payment = SchoolPayment(
                    id = existing?.id ?: 0,
                    title = title,
                    category = category,
                    amountCentavos = amount!!,
                    paidCentavos = paid!!,
                    dueDate = selectedDate,
                    notes = notes
                )

                viewLifecycleOwner.lifecycleScope.launch {
                    if (existing == null) {
                        paymentDao.insertPayment(payment)
                    } else {
                        paymentDao.updatePayment(payment)
                    }
                }

                dialog.dismiss()
            }
        }

        dialog.show()
    }

    private fun confirmDelete(payment: SchoolPayment) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete School Payment?")
            .setMessage(
                "Are you sure you want to delete this school payment?"
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    paymentDao.deletePayment(payment)
                }
            }
            .show()
    }
}