package com.example.myapplication.activities

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {
   lateinit var binding: FragmentLoginBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginBinding.inflate(layoutInflater,container,false)

        binding.apply {

            tvRegister.setOnClickListener{
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.frame_layout,RegisterFragment())
                    .commit()
            }

            tvForgotPass.setOnClickListener {
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.frame_layout,ForgotPassFragment())
                    .commit()
            }
            //Click login
            btnLogin.setOnClickListener {

            }
        }
        // Inflate the layout for this fragment
        return binding.root
    }

}