package com.clarxxinn.gastoskwela

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_splash)

        lifecycleScope.launch {
            delay(2000L)

            val preferences = getSharedPreferences(
                "gastoskwela_preferences",
                MODE_PRIVATE
            )

            val onboardingCompleted = preferences.getBoolean(
                "onboarding_completed",
                false
            )

            val destination = if (onboardingCompleted) {
                MainActivity::class.java
            } else {
                OnboardingActivity::class.java
            }

            startActivity(
                Intent(this@SplashActivity, destination)
            )

            finish()
        }
    }
}