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
                    }
                }, year, month, day)
                // Hiển thị DatePickerDialog
                datePickerDialog.show()
            }


            btnSelectImg.setOnClickListener {
                openImageChooser()
            }

            btnDeleteImg.setOnClickListener {
                showDeleteConfirmationDialog()
            }

            btnReset.setOnClickListener {
                showResetConfirmationDialog()
            }



        }

    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Xác nhận xóa ảnh")
            .setMessage("Bạn có chắc chắn muốn xóa ảnh?")
            .setPositiveButton("Xóa") { _, _ ->
                val studentUid = intent.getStringExtra("studentUid")
                if (studentUid != null) {
                    deleteImage(studentUid)
                }

            }
            .setNegativeButton("Hủy bỏ", null)
            .show()
    }

    private fun deleteImage(studentUid: String) {
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
                            var imgageUrl = studentInfo.imageUrl
                            if (!imgageUrl.isNullOrEmpty() &&imgageUrl.contains("avtdf.jpg")){
                                Toast.makeText(this@InfoActivity, "Đây là ảnh mặc định và không thể xóa!", Toast.LENGTH_SHORT).show()
                            }else{
                                // Tạo StorageReference từ đường dẫn ảnh hiện tại
                                val currentImageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imgageUrl.toString())
                                // Xóa ảnh hiện tại trong Firebase Storage
                                currentImageRef.delete()
                                    .addOnSuccessListener {
                                        // Thành công, cập nhật đường dẫn ảnh mới vào Firestore
                                        val newImageUrl = "https://firebasestorage.googleapis.com/v0/b/tuhoc-86488.appspot.com/o/profile_images%2Favtdf.jpg?alt=media&token=18fd7912-2a5e-4dea-a851-76b829266fad"

                                        firestore.collection("users")
                                            .document(currentUserUid)
                                            .collection("students")
                                            .document(studentUid)
                                            .update("imageUrl", newImageUrl)
                                            .addOnSuccessListener {
                                                // Thông báo cập nhật thành công
                                                Toast.makeText(this@InfoActivity, "Xóa và cập nhật ảnh thành công!", Toast.LENGTH_SHORT).show()
                                                // Gọi hàm để hiển thị ảnh mới lên giao diện
                                                displayStudentInfo(studentUid)
                                            }
                                            .addOnFailureListener {
                                                // Thông báo cập nhật thất bại
                                                Toast.makeText(this@InfoActivity, "Cập nhật ảnh thất bại!", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                    .addOnFailureListener {
                                        // Thông báo xóa ảnh hiện tại thất bại
                                        Toast.makeText(this@InfoActivity, "Xóa ảnh hiện tại thất bại!", Toast.LENGTH_SHORT).show()
                                    }
                            }
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
            this@InfoActivity.data = data // Lưu trữ data vào biến data
            selectedImageUri = data.data
            selectedImageUri?.let {
                binding.imgSelected.setImageURI(it)
                isImageSelected = true //đã chọn ảnh
                val studentUid = intent.getStringExtra("studentUid")
                if (studentUid != null) {
                    updateImage(studentUid, selectedImageUri!!)
                }
            }
        }
    }

    private fun updateImage(studentUid: String, selectedImageUri: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
                val firestore = FirebaseFirestore.getInstance()
                if (currentUserUid != null) {
                    firestore.collection("users")
                        .document(currentUserUid)
                        .collection("students")
                        .document(studentUid)
                        .get()
                        .addOnSuccessListener { documentSnapshot ->
                            if (documentSnapshot.exists()) {
                                val studentInfo = documentSnapshot.toObject(StudentInfo::class.java)
                                val currentImageUrl = studentInfo?.imageUrl

                                // Kiểm tra có phải là avtdf hay không
                                if (!currentImageUrl.isNullOrEmpty() && !currentImageUrl.contains("avtdf.jpg")) {
                                    val oldImageRef = FirebaseStorage.getInstance().getReferenceFromUrl(currentImageUrl)
                                    oldImageRef.delete()
                                        .addOnSuccessListener {

                                            uploadNewImage(studentUid, selectedImageUri)
                                        }
                                        .addOnFailureListener {
                                            Toast.makeText(this@InfoActivity, "Lôĩ khi xóa ảnh", Toast.LENGTH_SHORT).show()
                                        }
                                } else {
                                    uploadNewImage(studentUid, selectedImageUri)
                                }
                            }
                        }
                }
            }
        }
    }

    private fun uploadNewImage(studentUid: String, selectedImageUri: Uri) {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserUid != null) {
            val storageRef = FirebaseStorage.getInstance().reference.child("profile_images").child("$studentUid.jpg")
            // Up ảnh lên storage
            storageRef.putFile(selectedImageUri)
                .addOnSuccessListener { taskSnapshot ->
                    // Lấy Url từ storage
                    storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                        // Cập nhật Url vào firestore
                        FirebaseFirestore.getInstance().collection("users")
                            .document(currentUserUid)
                            .collection("students")
                            .document(studentUid)
                            .update("imageUrl", downloadUrl.toString())
                            .addOnSuccessListener {
                                // Cập nhật Url ảnh vào firestore
                                Toast.makeText(this@InfoActivity, "Cập nhât ảnh thành công!", Toast.LENGTH_SHORT).show()
                                // Tải lại thông tin
                                displayStudentInfo(studentUid)
                            }
                            .addOnFailureListener {
                                Toast.makeText(this@InfoActivity, "Cập ảnh thất bại!", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this@InfoActivity, "Lỗi khi tải ảnh!", Toast.LENGTH_SHORT).show()
                }
        }
    }


    private fun displayStudentInfo(studentUid: String) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){
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
                                    Toast.makeText(this@InfoActivity, "Không có thông tin", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .addOnFailureListener { exception ->
                            // Xử lý khi có lỗi xảy ra
                        }
                }
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