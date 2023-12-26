package com.example.myapplication.Fragments

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
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
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Locale
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
            //Check kết nối internet trước khi gọi hàm lưu
                if (!isNetworkConnected()){
                    Toast.makeText(requireContext(), "Vui lòng kiểm tra kết nối mạng và thử lại", Toast.LENGTH_SHORT).show()
                }else{
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
                                    if (!isValidPhoneNumber(phoneNumber)) {
                                        binding.progressBar.visibility = View.GONE
                                        Toast.makeText(requireContext(), "Số điện thoại không hợp lệ(ít nhất 10 số)", Toast.LENGTH_SHORT).show()
                                        return@withContext
                                    }

                                    if (!isValidEmail(email)) {
                                        binding.progressBar.visibility = View.GONE
                                        Toast.makeText(requireContext(), "Email không hợp lệ", Toast.LENGTH_SHORT).show()
                                        return@withContext
                                    }

                                    if (!isDateOfBirthValid(dateOfBirth)) {
                                        binding.progressBar.visibility = View.GONE
                                        Toast.makeText(requireContext(), "Ngày sinh không hợp lệ, chưa đủ 18 tuổi", Toast.LENGTH_SHORT).show()
                                        return@withContext
                                    }
                                    val selectedImageUri: Uri? = data?.data
                                    selectedImageUri?.let {
                                        imgSelected.setImageURI(it)
                                        saveStudentInfo(name,dateOfBirth,phoneNumber,email,major,studentId,classId,course, it)
                                    }
                                }
                            }
                        }else{
                            CoroutineScope(Dispatchers.IO).launch {
                                withContext(Dispatchers.Main) {
                                    if (!isValidPhoneNumber(phoneNumber)) {
                                        binding.progressBar.visibility = View.GONE
                                        Toast.makeText(requireContext(), "Số điện thoại không hợp lệ(ít nhất 10 số)", Toast.LENGTH_SHORT).show()
                                        return@withContext
                                    }

                                    if (!isValidEmail(email)) {
                                        binding.progressBar.visibility = View.GONE
                                        Toast.makeText(requireContext(), "Email không hợp lệ", Toast.LENGTH_SHORT).show()
                                        return@withContext
                                    }

                                    if (!isDateOfBirthValid(dateOfBirth)) {
                                        binding.progressBar.visibility = View.GONE
                                        Toast.makeText(requireContext(), "Ngày sinh không hợp lệ, chưa đủ 18 tuổi", Toast.LENGTH_SHORT).show()
                                        return@withContext
                                    }
                                    progressBar.visibility = View.VISIBLE
                                    saveStudentInfo2(name,dateOfBirth,phoneNumber,email,major,studentId,classId,course)
                                }
                            }
                        }
                    }
                }

            }
        }

        // Inflate the layout for this fragment
        return binding.root
    }

    private fun isDateOfBirthValid(dateOfBirth: String): Boolean {
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.getDefault())
        val dob = LocalDate.parse(dateOfBirth, formatter)
        val currentDate = LocalDate.now()

        val age = currentDate.year - dob.year

        // Kiểm tra nếu dưới 18 tuổi
        return if (currentDate.monthValue < dob.monthValue || (currentDate.monthValue == dob.monthValue && currentDate.dayOfMonth < dob.dayOfMonth)) {
            age - 1 >= 18
        } else {
            age >= 18
        }
    }

    private fun isNetworkConnected(): Boolean {
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun isValidEmail(email: String): Boolean {
        val emailRegex = Regex("^[A-Za-z](.*)([@]{1})(.{1,})(\\.)(.{1,})")
        return emailRegex.matches(email)
    }

    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        val phoneRegex = Regex("^\\d{10,11}\$")
        return phoneRegex.matches(phoneNumber)
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
                // Kiểm tra xem mã số sinh viên đã tồn tại chưa
                if (isStudentInfoExist(studentId,email,phoneNumber)){
                    Toast.makeText(requireContext(), "Thông tin sinh viên đã tồn tại. Vui lòng kiểm tra lại.", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                }else{
                    if (isStudentIdExist(studentId)){
                        Toast.makeText(requireContext(), "Mã số sinh viên đã tồn tại. Vui lòng kiểm tra lại.", Toast.LENGTH_SHORT).show()
                        binding.progressBar.visibility = View.GONE
                    }else if (isStudentEmailExist(email)){
                        Toast.makeText(requireContext(), "Email đã tồn tại. Vui lòng kiểm tra lại.", Toast.LENGTH_SHORT).show()
                        binding.progressBar.visibility = View.GONE
                    }else if (isStudentPhoneNumberlExist(phoneNumber)){
                        Toast.makeText(requireContext(), "Số điện thoại đã tồn tại. Vui lòng kiểm tra lại.", Toast.LENGTH_SHORT).show()
                        binding.progressBar.visibility = View.GONE
                    }else{
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
                                        resetForm()
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

    private suspend fun isStudentInfoExist(studentId: String, email: String, phoneNumber: String): Boolean {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserUid != null) {
            val firestore = FirebaseFirestore.getInstance()

            // Kiểm tra xem có bất kỳ sinh viên nào có cùng mã số sinh viên, email và số điện thoại hay không
            val querySnapshot = firestore.collection("users")
                .document(currentUserUid)
                .collection("students")
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("email", email)
                .whereEqualTo("phoneNumber", phoneNumber)
                .get()
                .await()

            return !querySnapshot.isEmpty
        }
        return false
    }


    //    lưu có ảnh
    private fun saveStudentInfo(name: String, dateOfBirth: String, phoneNumber: String, email: String, major: String, studentId: String, classId: String, course: String, it: Uri) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main) {
                val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
                // Kiểm tra xem mã số sinh viên đã tồn tại chưa
                if (isStudentInfoExist(studentId,email,phoneNumber)){
                    Toast.makeText(requireContext(), "Thông tin sinh viên đã tồn tại. Vui lòng kiểm tra lại.", Toast.LENGTH_SHORT).show()
                    binding.progressBar.visibility = View.GONE
                }else{
                    if (isStudentIdExist(studentId)){
                        Toast.makeText(requireContext(), "Mã số sinh viên đã tồn tại. Vui lòng kiểm tra lại.", Toast.LENGTH_SHORT).show()
                        binding.progressBar.visibility = View.GONE
                    }else if (isStudentEmailExist(email)){
                        Toast.makeText(requireContext(), "Email đã tồn tại. Vui lòng kiểm tra lại.", Toast.LENGTH_SHORT).show()
                        binding.progressBar.visibility = View.GONE
                    }else if (isStudentPhoneNumberlExist(phoneNumber)){
                        Toast.makeText(requireContext(), "Số điện thoại đã tồn tại. Vui lòng kiểm tra lại.", Toast.LENGTH_SHORT).show()
                        binding.progressBar.visibility = View.GONE
                    }else{
                        val imageStorageRef = FirebaseStorage.getInstance().reference.child("profile_images/$currentUserUid")
                        val imageFileName = studentId // Tên file ảnh duy nhất
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
                                                resetForm()
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
        }
    }

    private suspend fun isStudentPhoneNumberlExist(phoneNumber: String): Boolean {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserUid != null) {
            val firestore = FirebaseFirestore.getInstance()

            // Kiểm tra xem có bất kỳ sinh viên nào có cùng mã số sinh viên, email và số điện thoại hay không
            val querySnapshot = firestore.collection("users")
                .document(currentUserUid)
                .collection("students")
                .whereEqualTo("phoneNumber", phoneNumber)
                .get()
                .await()

            return !querySnapshot.isEmpty
        }
        return false
    }

    private suspend fun isStudentEmailExist(email: String): Boolean {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserUid != null) {
            val firestore = FirebaseFirestore.getInstance()

            // Kiểm tra xem có bất kỳ sinh viên nào có cùng mã số sinh viên, email và số điện thoại hay không
            val querySnapshot = firestore.collection("users")
                .document(currentUserUid)
                .collection("students")
                .whereEqualTo("email", email)
                .get()
                .await()

            return !querySnapshot.isEmpty
        }
        return false
    }

    private suspend fun isStudentIdExist(studentId: String): Boolean {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUserUid != null) {
            val firestore = FirebaseFirestore.getInstance()

            // Kiểm tra xem có bất kỳ sinh viên nào có cùng mã số sinh viên, email và số điện thoại hay không
            val querySnapshot = firestore.collection("users")
                .document(currentUserUid)
                .collection("students")
                .whereEqualTo("studentId", studentId)
                .get()
                .await()
            return !querySnapshot.isEmpty
        }
        return false
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

