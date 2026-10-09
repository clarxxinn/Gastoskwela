package com.clarxxinn.gastoskwela

import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.statusBarColor =
            android.graphics.Color.parseColor("#7057F4")

        window.navigationBarColor =
            android.graphics.Color.parseColor("#4324B7")

        setContentView(R.layout.activity_splash)

        val progressTrack =
            findViewById<FrameLayout>(R.id.splashProgressTrack)

        val progressFill =
            findViewById<View>(R.id.splashProgressFill)

        progressTrack.post {
            val fullWidth = progressTrack.width

            ValueAnimator.ofInt(0, fullWidth).apply {
                duration = 2000L

                addUpdateListener { animator ->
                    val width = animator.animatedValue as Int

                    val params = progressFill.layoutParams
                    params.width = width
                    progressFill.layoutParams = params
                }

                start()
            }
        }

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

            val allowanceSetupCompleted = preferences.getBoolean(
                "allowance_setup_completed",
                false
            )

            val destination = when {
                !onboardingCompleted ->
                    OnboardingActivity::class.java

                !allowanceSetupCompleted ->
                    AllowanceSetupActivity::class.java

                else ->
                    MainActivity::class.java
            }

            startActivity(
                Intent(
                    this@SplashActivity,
                    destination
                )
            )

            finish()
        }
    }
}