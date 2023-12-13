package com.example.myapplication.activities

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.example.myapplication.databinding.ActivityInfoBinding
import com.example.myapplication.models.StudentInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar


class InfoActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    lateinit var binding: ActivityInfoBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference
    private val PICK_IMAGE_REQUEST_CODE = 123
    private var data: Intent? = null
    private var selectedImageUri: Uri? = null
    private var isImageSelected = false
    private var check = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val currentDate = Calendar.getInstance()
        val year = currentDate.get(Calendar.YEAR)
        val month = currentDate.get(Calendar.MONTH)
        val day = currentDate.get(Calendar.DAY_OF_MONTH)

        val studentUid = intent.getStringExtra("studentUid")
        if (studentUid != null) {
            // Lấy thông tin sinh viên từ Firestore bằng UID
            // Hiển thị thông tin sinh viên trên giao diện
            displayStudentInfo(studentUid)
        }

        binding.apply {

            btnDate.setOnClickListener {
                val datePickerDialog = DatePickerDialog(this@InfoActivity, DatePickerDialog.OnDateSetListener { _, yearSelected, monthOfYear, dayOfMonth ->
                    // Tạo một Calendar với ngày được chọn
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(yearSelected, monthOfYear, dayOfMonth)

                    // Tính toán sự chênh lệch giữa ngày hiện tại và ngày được chọn
                    val ageDifference = currentDate.get(Calendar.YEAR) - yearSelected

                    // Kiểm tra điều kiện nếu nhỏ hơn 18 tuổi
                    if (ageDifference < 18) {
                        // Hiển thị thông báo Toast
                        Toast.makeText(this@InfoActivity, "Bạn chưa đủ 18 tuổi", Toast.LENGTH_SHORT).show()
                    } else {
                        // Hiển thị ngày đã chọn lên TextView
                        tvDateNow.text = "$dayOfMonth/${monthOfYear + 1}/$yearSelected"
                        check = true
                        showbutton()
                    }
                }, year, month, day)
                // Hiển thị DatePickerDialog
                datePickerDialog.show()
            }


            btnSelectImg.setOnClickListener {
                openImageChooser()
                showbutton()
            }

            btnDeleteImg.setOnClickListener {

            }

            btnReset.setOnClickListener {
                showResetConfirmationDialog()
            }

            btnSaveInfo.setOnClickListener {

            }


        }

    }

    private fun showbutton() {
        binding.apply {
            if (isImageSelected || check) {
                btnSaveInfo.visibility = View.VISIBLE
            } else {
                btnSaveInfo.visibility = View.GONE
            }
        }
    }

    private fun showResetConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Xác nhận đặt lại")
            .setMessage("Bạn có chắc chắn muốn đặt lại thông tin sinh viên?")
            .setPositiveButton("Đặt lại") { _, _ ->
                reset()
            }
            .setNegativeButton("Hủy bỏ", null)
            .show()
    }

    private fun reset() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){
                val studentUid = intent.getStringExtra("studentUid")
                if (studentUid != null) {
                    displayStudentInfo(studentUid)
                    data = null
                    selectedImageUri = null
                    isImageSelected = false
                    check = false
                }
            }
        }
    }


    private fun openImageChooser() {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){
                val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST_CODE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            this.data = data // Lưu trữ data vào biến data
            selectedImageUri = data.data
            selectedImageUri?.let {
                binding.imgSelected.setImageURI(it)
                isImageSelected = true //đã chọn ảnh
                check =true
            }
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