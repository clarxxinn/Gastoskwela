package com.clarxxinn.gastoskwela.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.clarxxinn.gastoskwela.R
import com.google.android.material.button.MaterialButton

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<MaterialButton>(R.id.btnAddExpense)
            .setOnClickListener {
                (requireActivity() as com.clarxxinn.gastoskwela.MainActivity)
                    .selectTab(R.id.nav_expenses)
            }

        view.findViewById<MaterialButton>(R.id.btnAddAllowance)
            .setOnClickListener {
                (requireActivity() as com.clarxxinn.gastoskwela.MainActivity)
                    .selectTab(R.id.nav_allowance)
            }
    }
}