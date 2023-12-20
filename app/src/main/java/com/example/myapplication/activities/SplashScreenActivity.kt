package com.example.myapplication.activities

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.core.content.ContentProviderCompat.requireContext
import com.example.myapplication.Fragments.LoginFragment
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivitySplashScreenBinding
import com.google.android.material.snackbar.Snackbar

class SplashScreenActivity : AppCompatActivity() {
    lateinit var binding: ActivitySplashScreenBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            splashImg.alpha = 0f
            splashImg.animate().setDuration(1500).alpha(1f).withEndAction {
                // Kiểm tra kết nối mạng sau khi hiển thị splash screen
                checkNetworkConnection()
            }
        }
    }

    private fun checkNetworkConnection() {
        if (!isNetworkConnected()) {
            // Hiển thị thông báo khi không có kết nối mạng
            showNetworkSnackbar()
        } else {
            // Nếu có kết nối mạng, chuyển sang MainActivity
            val intent = Intent(this@SplashScreenActivity, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun isNetworkConnected(): Boolean {
        val connectivityManager =
            this@SplashScreenActivity.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun showNetworkSnackbar() {
        val snackbar = Snackbar.make(
            binding.root,
            "Không có kết nối mạng. Vui lòng kiểm tra và thử lại.",
            Snackbar.LENGTH_INDEFINITE
        )
        snackbar.setAction("Thử lại") {
            // Khi người dùng nhấn "Thử lại", kiểm tra lại kết nối mạng
            checkNetworkConnection()
        }
        snackbar.show()
    }
}