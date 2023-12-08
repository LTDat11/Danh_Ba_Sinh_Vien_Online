package com.example.myapplication.Fragments

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentAddBinding
import com.example.myapplication.models.StudentInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

class AddFragment : Fragment() {
    lateinit var binding: FragmentAddBinding
    private val PICK_IMAGE_REQUEST_CODE = 123
    private var data: Intent? = null
    private var isImageSelected = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentAddBinding.inflate(layoutInflater,container,false)
        val currentDate = Calendar.getInstance()
        val year = currentDate.get(Calendar.YEAR)
        val month = currentDate.get(Calendar.MONTH)
        val day = currentDate.get(Calendar.DAY_OF_MONTH)



        binding.apply {
            tvDateNow.text = "$day/${month + 1}/$year"
            //Chọn ngày tháng nămm
            btnDate.setOnClickListener {

                val datePickerDialog = DatePickerDialog(requireContext(), DatePickerDialog.OnDateSetListener { _, yearSelected, monthOfYear, dayOfMonth ->
                    // Tạo một Calendar với ngày được chọn
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(yearSelected, monthOfYear, dayOfMonth)

                    // Tính toán sự chênh lệch giữa ngày hiện tại và ngày được chọn
                    val ageDifference = currentDate.get(Calendar.YEAR) - yearSelected

                    // Kiểm tra điều kiện nếu nhỏ hơn 18 tuổi
                    if (ageDifference < 18) {
                        // Hiển thị thông báo Toast
                        Toast.makeText(requireContext(), "Bạn chưa đủ 18 tuổi", Toast.LENGTH_SHORT).show()
                    } else {
                        // Hiển thị ngày đã chọn lên TextView
                        tvDateNow.text = "$dayOfMonth/${monthOfYear + 1}/$yearSelected"
                    }
                }, year, month, day)
                // Hiển thị DatePickerDialog
                datePickerDialog.show()
            }

            //Chọn image
            btnSelectImg.setOnClickListener {
                openImageChooser()
            }

            btnDeleteImg.setOnClickListener{
                imgSelected.setImageResource(R.drawable.avtdefault)
                isImageSelected = false
            }

            btnResetForm.setOnClickListener {
                resetForm()
            }

            btnSaveInfo.setOnClickListener {
                val name = edtNameInput.text.toString()
                val dateOfBirth = tvDateNow.text.toString()
                val phoneNumber = edtPhoneNumberInput.text.toString()
                val email = edtEmailInput.text.toString()
                val major = edtMajorInput.text.toString()
                val studentId = edtIdStudentInput.text.toString()
                val classId = edtIdClassInput.text.toString()
                val course = edtIdCourseInput.text.toString()

                if (name.isEmpty() || phoneNumber.isEmpty() || email.isEmpty() || studentId.isEmpty()) {
                    // Hiển thị thông báo lỗi nếu các trường bắt buộc chưa được nhập
                    Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ thông tin bắt buộc(Tên, số đt, email, mã số)", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                else{
                    if (isImageSelected){
                        progressBar.visibility = View.VISIBLE
                        CoroutineScope(Dispatchers.IO).launch {
                            withContext(Dispatchers.Main) {
                                val selectedImageUri: Uri? = data?.data
                                selectedImageUri?.let {
                                    imgSelected.setImageURI(it)
                                    saveStudentInfo(name,dateOfBirth,phoneNumber,email,major,studentId,classId,course, it)
                                    resetForm()
                                }
                            }
                        }
                    }else{
                        progressBar.visibility = View.VISIBLE
                        saveStudentInfo2(name,dateOfBirth,phoneNumber,email,major,studentId,classId,course)
                        resetForm()
                    }
                }
            }


        }

        // Inflate the layout for this fragment
        return binding.root
    }

    private fun resetForm() {
        val currentDate = Calendar.getInstance()
        val year = currentDate.get(Calendar.YEAR)
        val month = currentDate.get(Calendar.MONTH)
        val day = currentDate.get(Calendar.DAY_OF_MONTH)
        binding.edtNameInput.text.clear()
        binding.tvDateNow.text = "$day/${month + 1}/$year"
        binding.edtPhoneNumberInput.text.clear()
        binding.edtEmailInput.text.clear()
        binding.edtMajorInput.text.clear()
        binding.edtIdStudentInput.text.clear()
        binding.edtIdClassInput.text.clear()
        binding.edtIdCourseInput.text.clear()
        binding.imgSelected.setImageResource(R.drawable.avtdefault)
        isImageSelected = false
        binding.progressBar.visibility = View.GONE
    }

    //Lưu không có ảnh(lấy ảnh mặc định trong storage)
    private fun saveStudentInfo2(name: String, dateOfBirth: String, phoneNumber: String, email: String, major: String, studentId: String, classId: String, course: String) {
    // Lưu ảnh và thông tin người dùng vào Firestore
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
            val imageStorageRef = FirebaseStorage.getInstance().reference.child("profile_images")
            val imageRef = imageStorageRef.child("avtdf.jpg")
            imageRef.downloadUrl.addOnSuccessListener{ imageUrl ->
                val userUid = FirebaseAuth.getInstance().currentUser?.uid
                // Tạo một đối tượng StudentInfo

                val studentInfo = StudentInfo(
                    name = name,
                    dateOfBirth = dateOfBirth,
                    phoneNumber = phoneNumber,
                    email = email,
                    major = major,
                    studentId = studentId,
                    classId = classId,
                    course = course,
                    imageUrl = imageUrl.toString()
                )

                userUid?.let{
                    val firestore = FirebaseFirestore.getInstance()
                    val studentCollectionRef = firestore.collection("users").document(userUid).collection("students")
                    studentCollectionRef.add(studentInfo)
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Lưu thành công", Toast.LENGTH_SHORT).show()
                            binding.progressBar.visibility = View.GONE
                        }.addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Lỗi: $e", Toast.LENGTH_SHORT).show()
                            binding.progressBar.visibility = View.GONE
                        }


                }

            }

            }
        }
    }
//    lưu có ảnh
    private fun saveStudentInfo(name: String, dateOfBirth: String, phoneNumber: String, email: String, major: String, studentId: String, classId: String, course: String, it: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                val imageStorageRef = FirebaseStorage.getInstance().reference.child("profile_images")
                val imageFileName = UUID.randomUUID().toString() // Tên file ảnh duy nhất
                val imageRef = imageStorageRef.child("$imageFileName.jpg")

                val uploadTask = imageRef.putFile(it)
                uploadTask.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Lấy URL của ảnh từ Firebase Storage
                        imageRef.downloadUrl.addOnSuccessListener { imageUrl ->
                            // Lưu thông tin người dùng vào Firestore
                            val userUid = FirebaseAuth.getInstance().currentUser?.uid
                            // Tạo một đối tượng StudentInfo
                            val studentInfo = StudentInfo(
                                name = name,
                                dateOfBirth = dateOfBirth,
                                phoneNumber = phoneNumber,
                                email = email,
                                major = major,
                                studentId = studentId,
                                classId = classId,
                                course = course,
                                imageUrl = imageUrl.toString()
                            )
                            userUid?.let {
                                val firestore = FirebaseFirestore.getInstance()
                                val studentCollectionRef = firestore.collection("users").document(userUid).collection("students")
                                studentCollectionRef.add(studentInfo)
                                    .addOnSuccessListener {
                                        Toast.makeText(requireContext(), "Lưu thành công", Toast.LENGTH_SHORT).show()
                                        binding.progressBar.visibility = View.GONE
                                    }.addOnFailureListener { e ->
                                        Toast.makeText(requireContext(), "Lỗi: $e", Toast.LENGTH_SHORT).show()
                                        binding.progressBar.visibility = View.GONE
                                    }

                            }
                        }
                    }
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
            this.data = data // Lưu trữ data vào biến data của RegisterFragment
            val selectedImageUri: Uri? = data.data
            selectedImageUri?.let {
                binding.imgSelected.setImageURI(it)
                isImageSelected = true //đã chọn ảnh
            }
        }
    }

}