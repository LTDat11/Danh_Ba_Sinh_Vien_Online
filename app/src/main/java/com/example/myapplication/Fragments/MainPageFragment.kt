package com.example.myapplication.Fragments

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.ConnectivityManager
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
import androidx.fragment.app.FragmentTransaction
import com.example.myapplication.R
import com.example.myapplication.activities.AddActivity
import com.example.myapplication.activities.ChangePassActivity
import com.example.myapplication.databinding.FragmentMainPageBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainPageFragment : Fragment() {
    lateinit var binding: FragmentMainPageBinding
    private val handler = Handler(Looper.getMainLooper())
    private val checkNetworkInterval = 10000L // Thời gian giữa các lần kiểm tra (5 giây)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMainPageBinding.inflate(layoutInflater,container,false)

        // Mặc định, hiển thị Fragment Home khi MainPageFragment được tạo
        replaceFragment(HomeFragment())

        // Xử lý sự kiện khi chọn một mục trên Bottom Navigation
        binding.apply {
            bottomNavigationView.setOnNavigationItemSelectedListener { item ->
                when (item.itemId) {
                    R.id.navigation_home -> replaceFragment(HomeFragment())
//                    R.id.navigation_add -> replaceFragment(AddFragment())
                    R.id.navigation_scan -> replaceFragment(ScanQRCodeFragment())
                    R.id.navigation_weather -> replaceFragment(WeatherFragment())
                    R.id.navigation_setting -> replaceFragment(SettingFragment())
                }
                true
            }

            floating.setOnClickListener{
                val intent = Intent (requireContext(), AddActivity::class.java)
                startActivity(intent)
            }

            // Kiểm tra kết nối mạng định kỳ
            checkNetworkStatusPeriodically()


        }
        // Inflate the layout for this fragment
        return binding.root
    }

    private fun checkNetworkStatusPeriodically() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){
                handler.postDelayed(object : Runnable {
                    override fun run() {
                        if (!isNetworkAvailable()) {
                            showSnackbar("Không có kết nối mạng. Vui lòng kiểm tra lại.")
                        }

                        // Tiếp tục kiểm tra sau khoảng thời gian nhất định
                        handler.postDelayed(this, checkNetworkInterval)
                    }
                }, checkNetworkInterval)
            }
        }
    }

    private fun showSnackbar(s: String) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                Snackbar.make(binding.root, s, Snackbar.LENGTH_INDEFINITE)
                    .setAnchorView(binding.bottomNavigationView)
                    .setAction("Thử lại") {
                        if (isNetworkAvailable()) {
                            // Nếu có kết nối, thực hiện các hành động cần thiết
                            Toast.makeText(requireContext(), "Kết nối khôi phục", Toast.LENGTH_SHORT).show()
                        } else {
                            // Nếu vẫn không có kết nối, hiển thị Snackbar lại
                            showSnackbar("Không có kết nối mạng. Vui lòng kiểm tra lại.")
                        }
                    }
                    .show()
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetworkInfo = connectivityManager.activeNetworkInfo
        return activeNetworkInfo != null && activeNetworkInfo.isConnected
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            showExitConfirmationDialog()
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        val transaction: FragmentTransaction = childFragmentManager.beginTransaction()
        transaction.replace(R.id.fragmentContainer, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun showExitConfirmationDialog() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Xác nhận thoát ứng dụng")
            .setMessage("Bạn có chắc chắn muốn thoát ứng dụng?")
            .setPositiveButton("Thoát") { _, _ ->
                requireActivity().finish()
            }
            .setNegativeButton("Hủy bỏ", null)
            .show()
    }

    override fun onDestroyView() {
        // Hủy định kỳ kiểm tra khi Fragment bị hủy
        handler.removeCallbacksAndMessages(null)
        super.onDestroyView()
    }

}