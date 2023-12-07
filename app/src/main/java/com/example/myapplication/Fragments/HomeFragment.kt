package com.example.myapplication.Fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentHomeBinding


class HomeFragment : Fragment() {
    lateinit var binding: FragmentHomeBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(layoutInflater,container,false)

        binding.apply {

            setupSpinner()

            materialCardView.setOnClickListener {
                searchview.isIconified = false
            }




        }

        // Inflate the layout for this fragment
        return binding.root
    }

    private fun setupSpinner() {
        val list = resources.getStringArray(R.array.fillter)
        val adapter = ArrayAdapter(requireContext(),android.R.layout.simple_spinner_item,list)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFillter.adapter = adapter
    }

}