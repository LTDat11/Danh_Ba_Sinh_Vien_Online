package com.example.myapplication.Fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentForgotPassBinding
import com.google.firebase.auth.FirebaseAuth

class ForgotPassFragment : Fragment() {
    lateinit var binding: FragmentForgotPassBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentForgotPassBinding.inflate(layoutInflater,container,false)

        binding.apply {
            val auth = FirebaseAuth.getInstance()
            btnForgotPass.setOnClickListener {
                val email = forgotPassInput.text.toString()
                progressBar.visibility = View.VISIBLE

                if (validateEmail(email)) {
                    // Kiểm tra xem email có tồn tại trong hệ thống hay không
                    auth.fetchSignInMethodsForEmail(email)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val signInMethods = task.result?.signInMethods
                                if (signInMethods != null && signInMethods.isNotEmpty()) {
                                    // Email đã được đăng ký, gửi yêu cầu đặt lại mật khẩu
                                    auth.sendPasswordResetEmail(email)
                                        .addOnCompleteListener { resetTask ->
                                            if (resetTask.isSuccessful) {
                                                showMessage("Đã gửi yêu cầu đặt lại mật khẩu. Vui lòng kiểm tra email của bạn.")
                                            } else {
                                                showMessage2("Lỗi: ${resetTask.exception?.message}")
                                            }
                                        }
                                } else {
                                    showMessage2("Email chưa từng được đăng ký.")
                                }
                            } else {
                                showMessage2("Lỗi: ${task.exception?.message}")
                            }
                        }
                }
            }

            // Xử lý sự kiện khi nút "quay lại" trên điện thoại được nhấn
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                // Chuyển về LoginFragment khi nút "quay lại" được nhấn
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.frame_layout, LoginFragment())
                    .commit()
            }

        }
        // Inflate the layout for this fragment
        return binding.root
    }

    // Hàm kiểm tra địa chỉ email
    private fun validateEmail(email: String): Boolean {
        binding.apply {
            if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                showMessage2("Địa chỉ email không hợp lệ.")
                progressBar.visibility = View.GONE
                return false
            }
            return true
        }
    }

    // Hàm hiển thị thông báo
    private fun showMessage(message: String) {
        binding.apply {
            tvMessage.setText(message)
            tvMessage.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
            Handler(Looper.getMainLooper()).postDelayed({
                forgotPassInput.setText("")
                tvMessage.visibility = View.GONE
            }, 5000)
        }
    }
    private fun showMessage2(message: String) {
        binding.apply {
            tvMessage2.setText(message)
            tvMessage2.visibility = View.VISIBLE
            progressBar.visibility = View.GONE
            Handler(Looper.getMainLooper()).postDelayed({
                tvMessage2.visibility = View.GONE
            }, 5000)
        }
    }
}