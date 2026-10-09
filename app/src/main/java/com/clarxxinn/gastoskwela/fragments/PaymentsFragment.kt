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
import com.clarxxinn.gastoskwela.data.GastoskwelaDatabase
import com.clarxxinn.gastoskwela.data.SchoolPayment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

class PaymentsFragment : Fragment(R.layout.fragment_payments) {

    private val paymentDao by lazy {
        GastoskwelaDatabase.getDatabase(requireContext()).schoolPaymentDao()
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
        val formatter = NumberFormat.getCurrencyInstance(
            Locale.Builder().setLanguage("en").setRegion("PH").build()
        )
        return formatter.format(BigDecimal.valueOf(centavos, 2))
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val totalDue = view.findViewById<TextView>(R.id.tvTotalPaymentDue)
        val totalPaid = view.findViewById<TextView>(R.id.tvTotalPaymentPaid)
        val outstanding = view.findViewById<TextView>(
            R.id.tvTotalPaymentOutstanding
        )
        val emptyText = view.findViewById<TextView>(R.id.tvEmptyPayments)
        val container = view.findViewById<LinearLayout>(
            R.id.paymentListContainer
        )

        view.findViewById<MaterialButton>(R.id.btnAddPayment)
            .setOnClickListener {
                showPaymentDialog()
            }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    paymentDao.getAllPayments().collect { payments ->
                        renderPayments(container, emptyText, payments)
                    }
                }

                launch {
                    paymentDao.getTotalDue().collect {
                        totalDue.text = formatMoney(it)
                    }
                }

                launch {
                    paymentDao.getTotalPaid().collect {
                        totalPaid.text = formatMoney(it)
                    }
                }

                launch {
                    paymentDao.getTotalOutstanding().collect {
                        outstanding.text = formatMoney(it)
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

        emptyText.visibility =
            if (payments.isEmpty()) View.VISIBLE else View.GONE

        payments.forEach { payment ->
            val itemView = layoutInflater.inflate(
                R.layout.item_payment,
                container,
                false
            )

            itemView.findViewById<TextView>(R.id.tvPaymentTitle).text =
                payment.title

            itemView.findViewById<TextView>(R.id.tvPaymentCategory).text =
                payment.category

            itemView.findViewById<TextView>(R.id.tvPaymentDueDate).text =
                "Due: ${payment.dueDate}"

            itemView.findViewById<TextView>(R.id.tvPaymentStatus).text =
                payment.status

            itemView.findViewById<TextView>(R.id.tvPaymentAmount).text =
                "Total: ${formatMoney(payment.amountCentavos)}"

            itemView.findViewById<TextView>(R.id.tvPaymentPaid).text =
                "Paid: ${formatMoney(payment.paidCentavos)}"

            itemView.findViewById<TextView>(R.id.tvPaymentRemaining).text =
                "Remaining: ${formatMoney(payment.remainingCentavos)}"

            val notesView = itemView.findViewById<TextView>(
                R.id.tvPaymentNotes
            )

            if (payment.notes.isNotBlank()) {
                notesView.visibility = View.VISIBLE
                notesView.text = payment.notes
            } else {
                notesView.visibility = View.GONE
            }

            itemView.findViewById<MaterialButton>(R.id.btnEditPayment)
                .setOnClickListener {
                    showPaymentDialog(payment)
                }

            itemView.findViewById<MaterialButton>(R.id.btnDeletePayment)
                .setOnClickListener {
                    confirmDelete(payment)
                }

            container.addView(itemView)
        }
    }

    private fun showPaymentDialog(existing: SchoolPayment? = null) {
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

        var selectedDate = existing?.dueDate ?: LocalDate.now().toString()
        dateButton.text = selectedDate

        existing?.let {
            titleInput.setText(it.title)
            categoryInput.setText(it.category, false)
            amountInput.setText(
                BigDecimal.valueOf(it.amountCentavos, 2).toPlainString()
            )
            paidInput.setText(
                BigDecimal.valueOf(it.paidCentavos, 2).toPlainString()
            )
            notesInput.setText(it.notes)
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
                if (existing == null) "Add School Payment"
                else "Edit School Payment"
            )
            .setView(dialogView)
            .setNegativeButton("Cancel", null)
            .setPositiveButton(
                if (existing == null) "Save" else "Update",
                null
            )
            .create()

        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            .setOnClickListener {
                titleLayout.error = null
                categoryLayout.error = null
                amountLayout.error = null
                paidLayout.error = null

                val title = titleInput.text?.toString()?.trim().orEmpty()
                val category = categoryInput.text?.toString()?.trim().orEmpty()
                val notes = notesInput.text?.toString()?.trim().orEmpty()

                fun parseCentavos(text: String): Long? {
                    return try {
                        BigDecimal(text)
                            .movePointRight(2)
                            .longValueExact()
                    } catch (_: Exception) {
                        null
                    }
                }

                val amount = parseCentavos(
                    amountInput.text?.toString()?.trim().orEmpty()
                )
                val paid = parseCentavos(
                    paidInput.text?.toString()?.trim().orEmpty()
                        .ifBlank { "0" }
                )

                var valid = true

                if (title.isBlank()) {
                    titleLayout.error = "Title is required"
                    valid = false
                }

                if (category !in categories) {
                    categoryLayout.error = "Select a valid category"
                    valid = false
                }

                if (amount == null || amount <= 0) {
                    amountLayout.error = "Enter a valid positive amount"
                    valid = false
                }

                if (paid == null || paid < 0 ||
                    (amount != null && paid > amount)
                ) {
                    paidLayout.error =
                        "Paid amount must be between 0 and total amount"
                    valid = false
                }

                if (!valid) return@setOnClickListener

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