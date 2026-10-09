package com.clarxxinn.gastoskwela.fragments

import androidx.fragment.app.Fragment
import com.clarxxinn.gastoskwela.R

class ReportsFragment : Fragment(R.layout.fragment_placeholder) {

    override fun onViewCreated(
        view: android.view.View,
        savedInstanceState: android.os.Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<android.widget.TextView>(R.id.tvScreenTitle)
            .text = "Reports"

        view.findViewById<android.widget.TextView>(R.id.tvScreenDescription)
            .text = "View your spending reports and financial insights."
    }
}