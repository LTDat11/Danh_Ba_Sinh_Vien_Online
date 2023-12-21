package com.example.myapplication.Fragments

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentChangePassBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class ChangePassFragment : Fragment() {
    lateinit var binding: FragmentChangePassBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentChangePassBinding.inflate(layoutInflater,container,false)


        binding.apply {


            btnChangePass.setOnClickListener {
                if (!isNetworkConnected()){
                    Toast.makeText(requireContext(), "Vui lòng kiểm tra kết nối mạng và thử lại", Toast.LENGTH_SHORT).show()
                }else {
                    showChangePassConfirmationDialog()
                }
            }


            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                // Chuyển về Setting Fragment khi nút "quay lại" được nhấn
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.frame_layout, MainPageFragment())
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

    private fun showMessage(s: String) {
        binding.tvChangePassErro.text = s
        binding.tvChangePassErro.visibility = View.VISIBLE
        Handler(Looper.getMainLooper()).postDelayed({
            binding.tvChangePassErro.visibility = View.GONE
        }, 5000)
    }

    private fun showChangePassConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận đổi mật khẩu")
            .setMessage("Bạn có chắc chắn muốn đổi mật khẩu của tài khoản này?")
            .setPositiveButton("Đồng ý") { _, _ ->
                binding.progressBar.visibility = View.VISIBLE
                changePass()
            }
            .setNegativeButton("Hủy bỏ", null)
            .show()
    }

    private fun changePass() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                binding.apply {
                    val user = FirebaseAuth.getInstance().currentUser
                    val oldPassword = oldPassInput.text.toString().trim()
                    val newPassword = newPassInput.text.toString().trim()
                    val confirmPassword = newPassConfirmInput.text.toString().trim()

                    if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                        progressBar.visibility = View.GONE
                        showMessage("Vui lòng điền đầy đủ tất cả thông tin!")
                        return@withContext
                    }

                    // Kiểm tra mật khẩu mới có trùng với mật khẩu cũ
                    if (newPassword == oldPassword) {
                        progressBar.visibility = View.GONE
                        showMessage("Mật khẩu mới không được trùng với mật khẩu cũ.")
                        return@withContext
                    }

                    // Kiểm tra xem mật khẩu cũ có đúng hay không
                    val credential = EmailAuthProvider.getCredential(user?.email ?: "", oldPassword)
                    user?.reauthenticate(credential)?.addOnCompleteListener { reauthTask ->
                        if (reauthTask.isSuccessful) {
                            // Mật khẩu cũ đúng, kiểm tra mật khẩu mới và xác nhận mật khẩu mới
                            if (newPassword == confirmPassword) {
                                // Cập nhật mật khẩu mới
                                user.updatePassword(newPassword)
                                    .addOnCompleteListener { updatePasswordTask ->
                                        if (updatePasswordTask.isSuccessful) {
                                            Toast.makeText(requireContext(), "Đổi mật khẩu thành công", Toast.LENGTH_SHORT).show()
                                            progressBar.visibility = View.GONE

                                            // Đăng xuất khỏi tất cả các thiết bị
                                            user.getIdToken(true)
                                                .addOnCompleteListener { tokenTask ->
                                                    if (tokenTask.isSuccessful) {
                                                        // Đăng xuất khỏi tất cả các thiết bị
                                                        FirebaseAuth.getInstance().signOut()

                                                        // Chuyển về trang đăng nhập
                                                        requireActivity().supportFragmentManager.beginTransaction()
                                                            .replace(R.id.frame_layout, LoginFragment())
                                                            .commit()
                                                    } else {
                                                        showMessage("Lỗi khi cập nhật token: ${tokenTask.exception?.message}")
                                                    }
                                                }
                                        } else {
                                            Toast.makeText(requireContext(), "Lỗi khi đổi mật khẩu", Toast.LENGTH_SHORT).show()
                                            progressBar.visibility = View.GONE
                                        }
                                    }
                            } else {
                                progressBar.visibility = View.GONE
                                showMessage("Mật khẩu mới và xác nhận mật khẩu không khớp.")
                            }
                        } else {
                            showMessage("Mật khẩu cũ không đúng.")
                            progressBar.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

}