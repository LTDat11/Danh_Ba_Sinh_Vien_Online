package com.example.myapplication.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import com.example.myapplication.Fragments.LoginFragment
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivitySplashScreenBinding

class SplashScreenActivity : AppCompatActivity() {
    lateinit var binding: ActivitySplashScreenBinding
    private val SPLASH_TIME_OUT: Long = 2000 // 2 giây
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.apply {
            // Sử dụng Handler để đợi một khoảng thời gian và sau đó chuyển đến trang mới
//            Handler().postDelayed({
//                val intent = Intent(this@SplashScreenActivity, MainActivity::class.java)
//                startActivity(intent)
//                finish()
//            }, SPLASH_TIME_OUT)

            splashImg.alpha = 0f
            splashImg.animate().setDuration(1500).alpha(1f).withEndAction {
                val intent = Intent(this@SplashScreenActivity, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
        }
    }
}