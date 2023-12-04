package com.example.myapplication.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class RegisterFragment : Fragment() {
    lateinit var binding: FragmentRegisterBinding
    private lateinit var auth: FirebaseAuth
    private val PICK_IMAGE_REQUEST_CODE = 123
    private var data: Intent? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRegisterBinding.inflate(layoutInflater, container, false)

        binding.apply {
            // Khởi tạo Firebase Authentication
            auth = FirebaseAuth.getInstance()

            img.setOnClickListener {
                openImageChooser()
            }

            tvBacktologin.setOnClickListener {
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.frame_layout, LoginFragment())
                    .commit()
            }
            //Click register
            btnRegister.setOnClickListener {
                val emailInput = emailInput.text.toString().trim()
                val passwordInput: String = passwordInput.text.toString().trim()
                val confirmPasswordInput: String = passwordInputConfirm.text.toString().trim()

                if (validate(emailInput, passwordInput, confirmPasswordInput)) {
                    progressBar.visibility = View.VISIBLE
                    CoroutineScope(Dispatchers.IO).launch {
                        withContext(Dispatchers.Main) {
                            val selectedImageUri: Uri? = data?.data
                            selectedImageUri?.let {
                                binding.img.setImageURI(it)
                                registerWithEmailAndPassword(emailInput, passwordInput, it)
                            }
                        }
                    }
                }
            }

            // Xử lý sự kiện khi nút "quay lại" trên điện thoại được nhấn
            requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
                // Chuyển về LoginFragment khi nút "quay lại" được nhấn
                requireActivity().supportFragmentManager.beginTransaction()
                    .replace(R.id.frame_layout, LoginFragment())
                    .commit()
            }
        }
        // Inflate the layout for this fragment
        return binding.root
    }

    private fun openImageChooser() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            this.data = data // Lưu trữ data vào biến data của RegisterFragment
            val selectedImageUri: Uri? = data.data
            selectedImageUri?.let {
                binding.img.setImageURI(it)
            }
        }
    }

    private suspend fun saveImageAndUserInfoToFirestore(imageUri: Uri, email: String) {
        val imageStorageRef = FirebaseStorage.getInstance().reference.child("profile_images")
        val imageFileName = UUID.randomUUID().toString() // Tên file ảnh duy nhất
        val imageRef = imageStorageRef.child("$imageFileName.jpg")

        val uploadTask = imageRef.putFile(imageUri)

        uploadTask.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Lấy URL của ảnh từ Firebase Storage
                imageRef.downloadUrl.addOnSuccessListener { imageUrl ->
                    // Lưu thông tin người dùng vào Firestore
                    val userUid = FirebaseAuth.getInstance().currentUser?.uid
                    userUid?.let {
                        val userDocRef = FirebaseFirestore.getInstance().collection("users")
                            .document(userUid)

                        // Tạo một đối tượng User để lưu vào Firestore
                        val user = hashMapOf(
                            "email" to email,
                            "profileImageUrl" to imageUrl.toString(),
                            "name" to binding.edtName.text.toString()
                        )

                        // Lưu thông tin người dùng vào Firestore
                        userDocRef.set(user, SetOptions.merge())
                            .addOnSuccessListener {
                                // Thành công
                                showMessage("Đăng ký thành công!")
                            }.addOnFailureListener { e ->
                                showMessage("Lỗi khi cập nhật thông tin người dùng: $e")
                            }
                    }
                }
            } else {
                // Đã có lỗi xảy ra khi tải ảnh
                showMessage("Lỗi khi tải ảnh lên: ${task.exception?.message}")
            }
        }
    }

    private fun showMessage(s: String) {
        binding.tvMessage.text = s
        binding.tvMessage.visibility = View.VISIBLE
    }

    private fun registerWithEmailAndPassword(email: String, password: String, selectedImageUri: Uri) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {


                    // Lấy thông tin người dùng sau khi đăng ký thành công
                    val user = auth.currentUser
                    user?.let {
                        // Lưu ảnh và thông tin người dùng vào Firestore
                        CoroutineScope(Dispatchers.IO).launch {
                            withContext(Dispatchers.Main) {
                                binding.progressBar.visibility = View.GONE
                                saveImageAndUserInfoToFirestore(selectedImageUri, email)
                            }
                        }
                    }
                } else {
                    CoroutineScope(Dispatchers.IO).launch {
                        withContext(Dispatchers.Main) {
                            binding.progressBar.visibility = View.GONE
                            showMessage("Đăng ký thất bại: ${task.exception?.message}")
                        }
                    }
                }
            }
    }



    private fun validate(email: String, password: String, confirmPassword: String): Boolean {
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showMessage("Địa chỉ email không hợp lệ")
            return false
        }

        if (password.isEmpty() || password.length < 6) {
            showMessage("Mật khẩu phải có ít nhất 6 ký tự")
            return false
        }

        if (confirmPassword.isEmpty() || confirmPassword != password) {
            showMessage("Nhập lại mật khẩu không khớp")
            return false
        }

        return true
    }
}


