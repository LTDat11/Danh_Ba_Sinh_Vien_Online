package com.example.myapplication.Fragments

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentTransaction
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentMainPageBinding

class MainPageFragment : Fragment() {
    lateinit var binding: FragmentMainPageBinding
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
                    R.id.navigation_add -> replaceFragment(AddFragment())
                    R.id.navigation_weather -> replaceFragment(WeatherFragment())
                    R.id.navigation_setting -> replaceFragment(SettingFragment())
                }
                true
            }

        }
        // Inflate the layout for this fragment
        return binding.root
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

}