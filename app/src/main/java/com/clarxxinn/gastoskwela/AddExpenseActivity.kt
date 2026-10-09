package com.clarxxinn.gastoskwela

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.clarxxinn.gastoskwela.data.Expense
import com.clarxxinn.gastoskwela.data.GastoskwelaDatabase
import com.clarxxinn.gastoskwela.utils.MoneyUtils
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

class AddExpenseActivity : AppCompatActivity() {

    private var selectedDate = LocalDate.now()
    private var isSaving = false

    private val categories = listOf(
        "Food", "Transportation", "School Supplies",
        "Projects", "Load/Internet", "Others"
    )

    private val paymentMethods = listOf(
        "Cash", "GCash", "Maya", "Bank Transfer", "Other"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_expense)

        selectedDate = savedInstanceState
            ?.getString("selectedDate")
            ?.let { LocalDate.parse(it) }
            ?: LocalDate.now()

        findViewById<android.view.View>(
            R.id.btnAddExpenseBack
        ).setOnClickListener {
            finish()
        }

        val amount = findViewById<TextInputEditText>(
            R.id.etAddExpenseAmount
        )

        val amountLayout = findViewById<TextInputLayout>(
            R.id.layoutAddExpenseAmount
        )

        val category = findViewById<AutoCompleteTextView>(
            R.id.etAddExpenseCategory
        )

        val categoryLayout = findViewById<TextInputLayout>(
            R.id.layoutAddExpenseCategory
        )

        val description = findViewById<TextInputEditText>(
            R.id.etAddExpenseDescription
        )

        val paymentMethod = findViewById<AutoCompleteTextView>(
            R.id.etAddExpensePaymentMethod
        )

        val dateButton = findViewById<MaterialButton>(
            R.id.btnAddExpenseDate
        )

        val saveButton = findViewById<MaterialButton>(
            R.id.btnSaveAddExpense
        )

        category.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                categories.map { displayCategory(it) }
            )
        )

        category.setText("Pagkain", false)
        category.setOnClickListener { category.showDropDown() }

        paymentMethod.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                paymentMethods
            )
        )

        paymentMethod.setText("Cash", false)
        paymentMethod.setOnClickListener {
            paymentMethod.showDropDown()
        }

        fun updateDateLabel() {
            dateButton.text = selectedDate.format(
                DateTimeFormatter.ofPattern(
                    "MMM d, yyyy",
                    Locale.ENGLISH
                )
            )
        }

        updateDateLabel()

        dateButton.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, day ->
                    selectedDate = LocalDate.of(year, month + 1, day)
                    updateDateLabel()
                },
                selectedDate.year,
                selectedDate.monthValue - 1,
                selectedDate.dayOfMonth
            ).show()
        }

        saveButton.setOnClickListener {
            if (isSaving) return@setOnClickListener

            amountLayout.error = null
            categoryLayout.error = null

            val centavos = MoneyUtils.parseCentavos(
                amount.text?.toString().orEmpty()
            )

            val categoryText = category.text?.toString()
                ?.trim().orEmpty()

            val selectedCategory = categories.firstOrNull {
                displayCategory(it) == categoryText
            }

            if (centavos == null || centavos <= 0L) {
                amountLayout.error = "Enter a valid amount"
                return@setOnClickListener
            }

            if (selectedCategory == null) {
                categoryLayout.error = "Select a category"
                return@setOnClickListener
            }

            val descriptionText = description.text
                ?.toString()?.trim().orEmpty()

            val method = paymentMethod.text
                ?.toString()?.trim().orEmpty()

            if (method.isNotEmpty() && method !in paymentMethods) {
                paymentMethod.error = "Select a valid payment method"
                return@setOnClickListener
            }

            isSaving = true
            saveButton.isEnabled = false

            lifecycleScope.launch {
                try {
                    val expense = Expense(
                        amountCentavos = centavos,
                        category = selectedCategory,
                        description = descriptionText,
                        date = selectedDate.toString(),
                        notes = ""
                    )

                    GastoskwelaDatabase.getDatabase(
                        this@AddExpenseActivity
                    ).expenseDao().insertExpense(expense)

                    setResult(RESULT_OK)
                    finish()

                } catch (_: Exception) {
                    isSaving = false
                    saveButton.isEnabled = true

                    Toast.makeText(
                        this@AddExpenseActivity,
                        "Unable to save expense.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
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

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("selectedDate", selectedDate.toString())
        super.onSaveInstanceState(outState)
    }
}