package com.example.myapplication.Fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentSettingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SettingFragment : Fragment() {
    lateinit var binding: FragmentSettingBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSettingBinding.inflate(layoutInflater,container,false)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        binding.apply {
            // Lấy thông tin người dùng từ Firestore
            val currentUser = auth.currentUser
            currentUser?.let { user ->
                val userDocument = firestore.collection("users").document(user.uid)

                userDocument.get().addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        // Lấy thông tin từ Firestore
                        val userName = documentSnapshot.getString("name")
                        val userEmail = documentSnapshot.getString("email")
                        val userImageUrl = documentSnapshot.getString("profileImageUrl")

                        // Hiển thị thông tin lên giao diện
                        tvName.setText(userName)
                        tvEmail.setText(userEmail)

                        // Sử dụng Glide để hiển thị ảnh
                        Glide.with(requireContext())
                            .load(userImageUrl)
                            .into(img)
                    }
                }
            }

            btnLogout.setOnClickListener {
                showLogoutConfirmationDialog()
            }
        }

        // Inflate the layout for this fragment
        return binding.root
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận đăng xuất")
            .setMessage("Bạn có chắc chắn muốn đăng xuất?")
            .setPositiveButton("Đăng xuất") { _, _ ->
                // Đăng xuất người dùng
                auth.signOut()
                // Chuyển hướng về màn hình đăng nhập
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.frame_layout, LoginFragment())
                    .commit()
            }
            .setNegativeButton("Hủy bỏ", null)
            .show()
    }
}