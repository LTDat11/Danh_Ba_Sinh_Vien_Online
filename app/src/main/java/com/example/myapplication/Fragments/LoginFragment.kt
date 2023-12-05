package com.example.myapplication.Fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth

class LoginFragment : Fragment() {
   lateinit var binding: FragmentLoginBinding
   private lateinit var auth: FirebaseAuth
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLoginBinding.inflate(layoutInflater,container,false)
        auth = FirebaseAuth.getInstance()

        binding.apply {

            // Kiểm tra trạng thái đăng nhập
            val currentUser = auth.currentUser
            if(currentUser != null){
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.frame_layout, MainPageFragment())
                    .commit()
            }

            tvRegister.setOnClickListener{
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.frame_layout, RegisterFragment())
                    .commit()
            }

            tvForgotPass.setOnClickListener {
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.frame_layout, ForgotPassFragment())
                    .commit()
            }
            //Click login
            btnLogin.setOnClickListener {
                binding.progressBar.visibility = View.VISIBLE
                val email = emailInput.text.toString().trim()
                val password = passwordInput.text.toString().trim()

                if (email.isEmpty() || password.isEmpty()){
                    binding.progressBar.visibility = View.GONE
                    showMessage("Vui lòng nhập đủ thông tin")
                    return@setOnClickListener
                }

                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(requireActivity()) { task ->
                        if (task.isSuccessful) {
                            binding.progressBar.visibility = View.GONE
                            // Đăng nhập thành công,Chuyển tới trang MainPage
                            requireActivity().supportFragmentManager.beginTransaction()
                                .replace(R.id.frame_layout,MainPageFragment())
                                .commit()

                        } else {
                            binding.progressBar.visibility = View.GONE
                            // Đăng nhập thất bại
                            val errorMessage = task.exception?.message ?: "Đăng nhập thất bại"
                            showMessage(errorMessage)
                        }
                    }

            }
        }
        // Inflate the layout for this fragment
        return binding.root
    }

    private fun showMessage(s: String) {
        binding.apply {
            tvMessage.text = s
            tvMessage.visibility = View.VISIBLE
            Handler(Looper.getMainLooper()).postDelayed({
                tvMessage.visibility = View.GONE
            }, 5000)
        }
    }

}