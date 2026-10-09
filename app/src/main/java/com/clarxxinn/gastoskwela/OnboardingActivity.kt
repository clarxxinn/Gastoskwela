package com.clarxxinn.gastoskwela

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton

class OnboardingActivity : AppCompatActivity() {

    private lateinit var illustration: ImageView
    private lateinit var titleText: TextView
    private lateinit var descriptionText: TextView
    private lateinit var nextButton: MaterialButton
    private lateinit var skipButton: TextView
    private lateinit var indicators: List<View>

    private var currentPage = 0

    private val titles = listOf(
        "Track Your\nStudent Life Expenses",
        "Understand\nYour Spending",
        "Reach Your\nFinancial Goals"
    )

    private val descriptions = listOf(
        "Monitor your baon, pamasahe, pagkain, school payments, and more.",
        "View reports and charts to see where your money goes.",
        "Set budgets, save for your goals, and manage your gastos wisely."
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        illustration = findViewById(R.id.imgOnboarding)
        titleText = findViewById(R.id.tvOnboardingTitle)
        descriptionText = findViewById(R.id.tvOnboardingDescription)
        nextButton = findViewById(R.id.btnOnboardingNext)
        skipButton = findViewById(R.id.btnOnboardingSkip)

        indicators = listOf(
            findViewById(R.id.indicator1),
            findViewById(R.id.indicator2),
            findViewById(R.id.indicator3)
        )

        currentPage = savedInstanceState?.getInt("currentPage") ?: 0

        showPage()

        nextButton.setOnClickListener {
            if (currentPage < 2) {
                currentPage++
                showPage()
            } else {
                finishOnboarding()
            }
        }

        skipButton.setOnClickListener {
            finishOnboarding()
        }
    }

    private fun showPage() {
        titleText.text = titles[currentPage]
        descriptionText.text = descriptions[currentPage]

        illustration.setImageResource(R.mipmap.ic_launcher)

        indicators.forEachIndexed { index, indicator ->
            indicator.setBackgroundResource(
                if (index == currentPage) {
                    R.drawable.bg_onboarding_indicator_active
                } else {
                    R.drawable.bg_onboarding_indicator_inactive
                }
            )
        }

        nextButton.text = if (currentPage == 2) {
            "Get Started"
        } else {
            "Next"
        }

        skipButton.visibility = if (currentPage == 2) {
            View.INVISIBLE
        } else {
            View.VISIBLE
        }
    }

    private fun finishOnboarding() {
        getSharedPreferences(
            "gastoskwela_preferences",
            MODE_PRIVATE
        ).edit()
            .putBoolean("onboarding_completed", true)
            .apply()

        startActivity(
            Intent(
                this,
                AllowanceSetupActivity::class.java
            )
        )

        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("currentPage", currentPage)
        super.onSaveInstanceState(outState)
    }
}