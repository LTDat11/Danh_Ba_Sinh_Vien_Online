package com.example.myapplication.Fragments

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
                if (!isNetworkConnected()){
                    Toast.makeText(requireContext(), "Vui lòng kiểm tra kết nối mạng và thử lại", Toast.LENGTH_SHORT).show()
                } else {
                    val email = forgotPassInput.text.toString().trim()
                    progressBar.visibility = View.VISIBLE

                    if (validateEmail(email)) {
                        auth.sendPasswordResetEmail(email)
                            .addOnCompleteListener(requireActivity()) { resetTask ->
                                progressBar.visibility = View.GONE
                                if (resetTask.isSuccessful) {
                                    // Gửi yêu cầu đặt lại mật khẩu thành công
                                    showMessage("Đã gửi yêu cầu đặt lại mật khẩu. Vui lòng kiểm tra email của bạn.")
                                } else {
                                    // Xảy ra lỗi khi gửi yêu cầu đặt lại mật khẩu
                                    showMessage2("Lỗi: ${resetTask.exception?.message}")
                                }
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

    private fun isNetworkConnected(): Boolean {
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
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