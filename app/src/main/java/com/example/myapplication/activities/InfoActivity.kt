package com.example.myapplication.activities

import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.example.myapplication.databinding.ActivityInfoBinding
import com.example.myapplication.models.StudentInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class InfoActivity : AppCompatActivity() {
    lateinit var binding: ActivityInfoBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val studentUid = intent.getStringExtra("studentUid")

        if (studentUid != null) {
            // Lấy thông tin sinh viên từ Firestore bằng UID
            // Hiển thị thông tin sinh viên trên giao diện
            displayStudentInfo(studentUid)

        }

    }

    private fun displayStudentInfo(studentUid: String) {
        val firestore = FirebaseFirestore.getInstance()
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserUid != null) {
            firestore.collection("users")
                .document(currentUserUid)
                .collection("students")
                .document(studentUid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()){
                        val studentInfo = document.toObject(StudentInfo::class.java)
                        if (studentInfo != null) {
                            // Hiển thị thông tin sinh viên trên giao diện
                            displayStudentInfo(studentInfo)
                        }else{
                            Toast.makeText(this, "Không có thông tin", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener { exception ->
                    // Xử lý khi có lỗi xảy ra
                }
        }

    }

    private fun displayStudentInfo(studentInfo: StudentInfo) {
        binding.apply {
            edtNameInput.setText(studentInfo.name)
            edtEmailInput.setText(studentInfo.email)
            edtIdStudentInput.setText(studentInfo.studentId)
            tvDateNow.setText(studentInfo.dateOfBirth)
            edtPhoneNumberInput.setText(studentInfo.phoneNumber)
            edtMajorInput.setText(studentInfo.major)
            edtIdClassInput.setText(studentInfo.classId)
            edtIdCourseInput.setText(studentInfo.course)

            edtPhoneNumberInput.setTextColor(Color.BLUE)
            edtPhoneNumberInput.paintFlags = edtEmailInput.paintFlags or Paint.UNDERLINE_TEXT_FLAG

            edtEmailInput.setTextColor(Color.BLUE)
            edtEmailInput.paintFlags = edtEmailInput.paintFlags or Paint.UNDERLINE_TEXT_FLAG

            if (!studentInfo.imageUrl.isNullOrEmpty()) {
                Glide.with(this@InfoActivity)
                    .load(studentInfo.imageUrl) // Đường dẫn hoặc tên tệp của ảnh
                    .into(imgSelected)
            }

            edtPhoneNumberInput.setOnClickListener {
                if (!studentInfo.phoneNumber.isNullOrEmpty()) {
                    showCallOrMessageDialog(studentInfo.phoneNumber)
                }
            }

            edtEmailInput.setOnClickListener {
                if (!studentInfo.email.isNullOrEmpty()) {
                    openEmail(studentInfo.email)
                }
            }
        }
    }

    private fun openEmail(email: String) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){
                val intent = Intent(Intent.ACTION_SENDTO)
                intent.data = Uri.parse("mailto:$email")
                if (intent.resolveActivity(packageManager) != null) {
                    startActivity(intent)
                } else {
                    // Xử lý khi không có ứng dụng email được cài đặt
                    Toast.makeText(this@InfoActivity, "Không có ứng dụng email được cài đặt", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showCallOrMessageDialog(phoneNumber: String) {
        CoroutineScope(Dispatchers.IO).launch{
            withContext(Dispatchers.Main){
                val options = arrayOf("Gọi", "Nhắn tin")

                val builder = AlertDialog.Builder(this@InfoActivity)
                builder.setTitle("Chọn hành động")
                builder.setItems(options) { _, which ->
                    when (which) {
                        0 -> {
                            // Gọi điện thoại
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
                            startActivity(intent)
                        }
                        1 -> {
                            // Nhắn tin
                            val intent = Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", phoneNumber, null))
                            startActivity(intent)
                        }
                    }
                }

                builder.setNegativeButton("Hủy") { dialog, _ ->
                    dialog.dismiss()
                }

                builder.show()
            }
        }

    }



}