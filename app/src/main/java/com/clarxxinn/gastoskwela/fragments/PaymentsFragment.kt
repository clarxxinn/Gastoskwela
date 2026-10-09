package com.clarxxinn.gastoskwela.fragments

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.clarxxinn.gastoskwela.R
import com.clarxxinn.gastoskwela.data.GastoskwelaDatabase
import com.clarxxinn.gastoskwela.data.SchoolPayment
import com.clarxxinn.gastoskwela.utils.MoneyUtils
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.format.DateTimeFormatter
import java.util.Locale

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

    private var allPayments: List<SchoolPayment> = emptyList()
    private var selectedTab = 0

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        selectedTab = savedInstanceState?.getInt(
            "selectedPaymentTab", 0
        ) ?: 0

        view.findViewById<View>(
            R.id.btnAddPayment
        ).setOnClickListener {
            showPaymentDialog()
        }

        view.findViewById<View>(
            R.id.tabUpcomingPayments
        ).setOnClickListener {
            switchTab(view, 0)
        }

        view.findViewById<View>(
            R.id.tabCompletedPayments
        ).setOnClickListener {
            switchTab(view, 1)
        }

        switchTab(view, selectedTab, animate = false)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                paymentDao.getAllPayments().collect { payments ->
                    allPayments = payments
                    renderPayments(view)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("selectedPaymentTab", selectedTab)
        super.onSaveInstanceState(outState)
    }

    private fun switchTab(
        view: View,
        tab: Int,
        animate: Boolean = true
    ) {
        if (selectedTab == tab && animate) return

        selectedTab = tab

        view.findViewById<View>(
            R.id.indicatorUpcoming
        ).visibility = if (tab == 0) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }

        view.findViewById<View>(
            R.id.indicatorCompleted
        ).visibility = if (tab == 1) {
            View.VISIBLE
        } else {
            View.INVISIBLE
        }

        view.findViewById<TextView>(
            R.id.tvUpcomingTab
        ).setTextColor(
            if (tab == 0) 0xFF6547ED.toInt()
            else 0xFF6B7280.toInt()
        )

        view.findViewById<TextView>(
            R.id.tvCompletedTab
        ).setTextColor(
            if (tab == 1) 0xFF6547ED.toInt()
            else 0xFF6B7280.toInt()
        )

        renderPayments(view)

        if (animate) {
            val container = view.findViewById<View>(
                R.id.paymentListContainer
            )

            container.alpha = 0f
            container.translationX =
                if (tab == 0) -18f else 18f

            container.animate()
                .alpha(1f)
                .translationX(0f)
                .setDuration(200)
                .start()
        }
    }

    private fun renderPayments(view: View) {
        val container = view.findViewById<LinearLayout>(
            R.id.paymentListContainer
        )

        val emptyText = view.findViewById<TextView>(
            R.id.tvEmptyPayments
        )

        container.removeAllViews()

        val filtered = allPayments.filter { payment ->
            val completed =
                payment.remainingCentavos <= 0L ||
                        payment.status == "Paid"

            if (selectedTab == 0) !completed else completed
        }.sortedWith(
            compareBy<SchoolPayment> { it.dueDate }
                .thenByDescending { it.id }
        )

        emptyText.visibility =
            if (filtered.isEmpty()) View.VISIBLE else View.GONE

        emptyText.text = if (selectedTab == 0) {
            "No upcoming school payments.\nTap + to add one."
        } else {
            "No completed school payments yet."
        }

        val dateFormatter = DateTimeFormatter.ofPattern(
            "MMM d, yyyy",
            Locale.ENGLISH
        )

        filtered.forEach { payment ->
            val itemView = layoutInflater.inflate(
                R.layout.item_payment,
                container,
                false
            )

            val iconBackground = itemView.findViewById<View>(
                R.id.paymentIconBackground
            )

            val icon = itemView.findViewById<ImageView>(
                R.id.ivPaymentIcon
            )

            val (iconColor, backgroundColor) =
                categoryColors(payment.category)

            iconBackground.background.mutate().setTint(
                backgroundColor
            )

            icon.setColorFilter(iconColor)

            icon.setImageResource(
                when (payment.category) {
                    "Tuition", "School Fees" ->
                        R.drawable.ic_payment_school
                    "Projects" ->
                        R.drawable.ic_payment_project
                    "School Supplies" ->
                        R.drawable.ic_payment_print
                    "Events" ->
                        R.drawable.ic_payment_calendar
                    else ->
                        R.drawable.ic_payment_school
                }
            )

            itemView.findViewById<TextView>(
                R.id.tvPaymentTitle
            ).text = payment.title

            itemView.findViewById<TextView>(
                R.id.tvPaymentAmount
            ).text = MoneyUtils.format(payment.amountCentavos)

            val dueDate = try {
                LocalDate.parse(payment.dueDate)
            } catch (_: Exception) {
                null
            }

            itemView.findViewById<TextView>(
                R.id.tvPaymentDueDate
            ).text = dueDate?.format(dateFormatter)
                ?: payment.dueDate

            val statusView = itemView.findViewById<TextView>(
                R.id.tvPaymentStatus
            )

            val completed =
                payment.remainingCentavos <= 0L ||
                        payment.status == "Paid"

            if (completed) {
                statusView.text = "Completed"
                statusView.setTextColor(
                    0xFF22C55E.toInt()
                )
            } else if (dueDate != null) {
                val days = ChronoUnit.DAYS.between(
                    LocalDate.now(), dueDate
                )

                statusView.text = when {
                    days < 0L ->
                        "${-days} days overdue"
                    days == 0L ->
                        "Due today"
                    days == 1L ->
                        "1 day left"
                    else ->
                        "$days days left"
                }

                statusView.setTextColor(
                    when {
                        days <= 5L ->
                            0xFFEF4444.toInt()
                        days <= 10L ->
                            0xFFF97316.toInt()
                        days <= 20L ->
                            0xFF6547ED.toInt()
                        else ->
                            0xFFD4A23C.toInt()
                    }
                )
            } else {
                statusView.text = payment.status
                statusView.setTextColor(
                    0xFF6B7280.toInt()
                )
            }

            val card = itemView.findViewById<MaterialCardView>(
                R.id.paymentCard
            )

            card.setOnClickListener {
                showPaymentDialog(payment)
            }

            card.setOnLongClickListener {
                confirmDelete(payment)
                true
            }

            container.addView(itemView)
        }
    }

    private fun categoryColors(category: String): Pair<Int, Int> {
        return when (category) {
            "Projects" ->
                0xFFEF4444.toInt() to 0xFFFEE2E2.toInt()
            "School Supplies" ->
                0xFF3B82F6.toInt() to 0xFFDBEAFE.toInt()
            "Tuition" ->
                0xFFF59E0B.toInt() to 0xFFFEF3C7.toInt()
            "School Fees" ->
                0xFF22C55E.toInt() to 0xFFDCFCE7.toInt()
            "Events" ->
                0xFF8B5CF6.toInt() to 0xFFEDE9FE.toInt()
            else ->
                0xFF64748B.toInt() to 0xFFE2E8F0.toInt()
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

        existing?.let { payment ->
            titleInput.setText(payment.title)

            categoryInput.setText(payment.category, false)

            amountInput.setText(
                BigDecimal.valueOf(
                    payment.amountCentavos, 2
                ).toPlainString()
            )

            paidInput.setText(
                BigDecimal.valueOf(
                    payment.paidCentavos, 2
                ).toPlainString()
            )

            notesInput.setText(payment.notes)
        }

        dateButton.setOnClickListener {
            val date = try {
                LocalDate.parse(selectedDate)
            } catch (_: Exception) {
                LocalDate.now()
            }

            DatePickerDialog(
                requireContext(),
                { _, year, month, day ->
                    selectedDate = LocalDate.of(
                        year, month + 1, day
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
                if (existing == null)
                    "Add School Payment"
                else
                    "Edit School Payment"
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
                    ?.toString()?.trim().orEmpty()

                val category = categoryInput.text
                    ?.toString()?.trim().orEmpty()

                val notes = notesInput.text
                    ?.toString()?.trim().orEmpty()

                val amount = MoneyUtils.parseCentavos(
                    amountInput.text?.toString().orEmpty()
                )

                val paid = MoneyUtils.parseCentavos(
                    paidInput.text?.toString()
                        ?.ifBlank { "0" }.orEmpty(),
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

                if (amount == null || amount <= 0L) {
                    amountLayout.error =
                        "Enter a valid positive amount"
                    valid = false
                }

                if (
                    paid == null ||
                    (amount != null && paid > amount)
                ) {
                    paidLayout.error =
                        "Paid must be between 0 and total"
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

                dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
                ).isEnabled = false

                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        if (existing == null) {
                            paymentDao.insertPayment(payment)
                        } else {
                            paymentDao.updatePayment(payment)
                        }
                        dialog.dismiss()
                    } catch (_: Exception) {
                        dialog.getButton(
                            AlertDialog.BUTTON_POSITIVE
                        ).isEnabled = true

                        Toast.makeText(
                            requireContext(),
                            "Unable to save payment.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        dialog.show()
    }

    private fun confirmDelete(payment: SchoolPayment) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete School Payment?")
            .setMessage(
                "Are you sure you want to delete this payment?"
            )
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        paymentDao.deletePayment(payment)
                    } catch (_: Exception) {
                        Toast.makeText(
                            requireContext(),
                            "Unable to delete payment.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .show()
    }
}