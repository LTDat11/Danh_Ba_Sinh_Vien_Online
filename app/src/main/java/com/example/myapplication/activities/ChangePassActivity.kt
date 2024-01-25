package com.example.myapplication.activities

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.myapplication.Fragments.LoginFragment
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityChangePassBinding
import com.example.myapplication.databinding.ActivityInfoBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChangePassActivity : AppCompatActivity() {
    lateinit var binding: ActivityChangePassBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChangePassBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            btnChangePass.setOnClickListener {
                if (!isNetworkConnected()) {
                    Toast.makeText(
                        this@ChangePassActivity,
                        "Vui lòng kiểm tra kết nối mạng và thử lại",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    showChangePassConfirmationDialog()
                }
            }
        }

    }

    private fun isNetworkConnected(): Boolean {
        val connectivityManager =
            this@ChangePassActivity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun showChangePassConfirmationDialog() {
        AlertDialog.Builder(this@ChangePassActivity)
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
                                            Toast.makeText(
                                                this@ChangePassActivity,
                                                "Đổi mật khẩu thành công",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            progressBar.visibility = View.GONE

                                            // Đăng xuất khỏi tất cả các thiết bị
                                            user.getIdToken(true)
                                                .addOnCompleteListener { tokenTask ->
                                                    if (tokenTask.isSuccessful) {
                                                        // Đăng xuất khỏi tất cả các thiết bị
                                                        FirebaseAuth.getInstance().signOut()

                                                        // Chuyển về trang đăng nhập
                                                        val intent = Intent(this@ChangePassActivity, MainActivity::class.java)
                                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                        startActivity(intent)
                                                    } else {
                                                        showMessage("Lỗi khi cập nhật token: ${tokenTask.exception?.message}")
                                                    }
                                                }
                                        } else {
                                            Toast.makeText(
                                                this@ChangePassActivity,
                                                "Lỗi khi đổi mật khẩu",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                            progressBar.visibility = View.GONE
                                        }
                                    }
                            } else {
                                progressBar.visibility = View.GONE
                                showMessage("Mật khẩu mới và xác nhận mật khẩu không khớp.")
                            }
                        } else {
                            showMessage("Mật khẩu hiện tại không đúng.")
                            progressBar.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }
    private fun showMessage(s: String) {
        binding.tvChangePassErro.text = s
        binding.tvChangePassErro.visibility = View.VISIBLE
        Handler(Looper.getMainLooper()).postDelayed({
            binding.tvChangePassErro.visibility = View.GONE
        }, 5000)
    }
}