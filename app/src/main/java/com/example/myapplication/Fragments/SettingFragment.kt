package com.example.myapplication.Fragments

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentSettingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SettingFragment : Fragment() {
    lateinit var binding: FragmentSettingBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference
    private val PICK_IMAGE_REQUEST_CODE = 123
    private var isImageSelected = false
    private var data: Intent? = null
    private var selectedImageUri: Uri? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSettingBinding.inflate(layoutInflater,container,false)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference

        binding.apply {
            loadUserInfo() // Gọi hàm để load thông tin người dùng

            //Đăng xuất
            btnLogout.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    withContext(Dispatchers.Main){
                        showLogoutConfirmationDialog()
                    }
                }
            }

            //edit img
            iconEditImg.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch{
                    withContext(Dispatchers.Main){
                        openImageChooser()
                    }
                }
            }

            //delete img
            iconDeleteImg.setOnClickListener{
                CoroutineScope(Dispatchers.IO).launch {
                    withContext(Dispatchers.Main){
                        showDeleteConfirmationDialog()
                    }
                }
            }

            //edit name
            iconEditName.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch{
                    withContext(Dispatchers.Main){
                        showEditNameDialog()
                    }
                }
            }


        }

        // Inflate the layout for this fragment
        return binding.root
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận xóa ảnh")
            .setMessage("Bạn có chắc chắn muốn xóa ảnh?")
            .setPositiveButton("Xóa") { _, _ ->
                deleteCurrentImage()
            }
            .setNegativeButton("Hủy bỏ", null)
            .show()
    }

    private fun deleteCurrentImage() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userDocument = firestore.collection("users").document(user.uid)
            userDocument.get().addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    // Lấy đường dẫn ảnh hiện tại từ Firestore
                    val currentImageUrl = documentSnapshot.getString("profileImageUrl")
                    // Kiểm tra xem ảnh hiện tại có phải là avtdf.jpg không
                    if (!currentImageUrl.isNullOrEmpty() && currentImageUrl.contains("avtdf.jpg")) {
                        // Thông báo rằng đây là ảnh mặc định và không thể xóa
                        Toast.makeText(context, "Đây là ảnh mặc định và không thể xóa!", Toast.LENGTH_SHORT).show()
                    } else {
                        // Tạo StorageReference từ đường dẫn ảnh hiện tại
                        val currentImageRef = storage.getReferenceFromUrl(currentImageUrl.toString())
                        // Xóa ảnh hiện tại trong Firebase Storage
                        currentImageRef.delete()
                            .addOnSuccessListener {
                                // Thành công, cập nhật đường dẫn ảnh mới vào Firestore
                                val newImageUrl =
                                    "https://firebasestorage.googleapis.com/v0/b/tuhoc-86488.appspot.com/o/profile_images%2Favtdf.jpg?alt=media&token=18fd7912-2a5e-4dea-a851-76b829266fad"
                                userDocument.update("profileImageUrl", newImageUrl)
                                    .addOnSuccessListener {
                                        // Thông báo cập nhật thành công
                                        Toast.makeText(context, "Xóa và cập nhật ảnh thành công!", Toast.LENGTH_SHORT).show()
                                        // Hiển thị ảnh mới lên giao diện
                                        loadUserInfo()
                                    }
                                    .addOnFailureListener {
                                        // Thông báo cập nhật thất bại
                                        Toast.makeText(context, "Cập nhật ảnh thất bại!", Toast.LENGTH_SHORT).show()
                                    }
                            }
                            .addOnFailureListener {
                                // Thông báo xóa ảnh hiện tại thất bại
                                Toast.makeText(context, "Xóa ảnh hiện tại thất bại!", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }
        }
    }

    private fun loadUserInfo() {
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
                        if (userName == "") {
                            //Lấy tên người dùng từ email
                            val userName2 = userEmail?.substringBefore('@')
                            tvName.setText(userName2)
                            tvEmail.setText(userEmail)
                            // Sử dụng Glide để hiển thị ảnh
                            Glide.with(requireContext())
                                .load(userImageUrl)
                                .into(img)
                        } else {
                            tvName.setText(userName)
                            tvEmail.setText(userEmail)

                            // Sử dụng Glide để hiển thị ảnh
                            Glide.with(requireContext())
                                .load(userImageUrl)
                                .into(img)
                        }
                    }
                }
            }
        }
    }

    private fun showEditNameDialog() {
        val editText = EditText(requireContext())
        editText.hint = "Nhập tên mới"

        AlertDialog.Builder(requireContext())
            .setTitle("Chỉnh sửa tên")
            .setView(editText)
            .setPositiveButton("Lưu") { _, _ ->
                val newName = editText.text.toString()
                updateName(newName)
            }
            .setNegativeButton("Hủy bỏ", null)
            .show()
    }

    private fun updateName(newName: String) {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userDocument = firestore.collection("users").document(user.uid)
            userDocument.update("name", newName)
                .addOnSuccessListener {
                    // Thông báo cập nhật tên thành công
                    Toast.makeText(context, "Cập nhật tên thành công!", Toast.LENGTH_SHORT).show()
                    // Hiển thị tên mới lên giao diện
                    loadUserInfo()
                }
                .addOnFailureListener {
                    // Thông báo cập nhật tên thất bại
                    Toast.makeText(context, "Cập nhật tên thất bại!", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateImage() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            // Tạo đường dẫn tới ảnh trên Firebase Storage
            val imageRef = storageRef.child("profile_images/${user.uid}.jpg")

            // Tạo AlertDialog để hiển thị xác nhận
            val confirmationDialog = AlertDialog.Builder(requireContext())
                .setTitle("Xác nhận thay đổi ảnh đại diện")
                .setMessage("Bạn có chắc chắn muốn thay đổi ảnh đại diện?")
                .setPositiveButton("Chấp nhận") { _, _ ->
                    // Nếu chấp nhận, tiếp tục tải ảnh lên Firebase Storage
                    uploadImage(imageRef)
                }
                .setNegativeButton("Hủy bỏ", null)
                .create()

            // Hiển thị AlertDialog
            confirmationDialog.show()
        }
    }

    private fun uploadImage(imageRef: StorageReference) {
        // Lấy Uri của ảnh đã chọn từ Intent
        val selectedImageUri: Uri? = data?.data

        // Kiểm tra xem Uri có tồn tại và đã chọn ảnh chưa
        if (selectedImageUri != null && isImageSelected) {
            // Tải ảnh lên Firebase Storage
            imageRef.putFile(selectedImageUri)
                .addOnSuccessListener { taskSnapshot ->
                    // Lấy đường dẫn tới ảnh trên Firebase Storage
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        // Cập nhật đường dẫn ảnh mới vào Firestore
                        val userDocument = firestore.collection("users").document(auth.currentUser?.uid!!)
                        userDocument.update("profileImageUrl", uri.toString())
                            .addOnSuccessListener {
                                // Thông báo cập nhật thành công
                                Toast.makeText(context, "Cập nhật ảnh đại diện thành công!", Toast.LENGTH_SHORT).show()
                                loadUserInfo()
                            }
                            .addOnFailureListener {
                                // Thông báo cập nhật thất bại
                                Toast.makeText(context, "Cập nhật ảnh đại diện thất bại!", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    // Thông báo tải ảnh lên thất bại
                    Toast.makeText(context, "Tải ảnh lên thất bại!", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun openImageChooser() {
        val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            this.data = data // Lưu trữ data vào biến data
            selectedImageUri = data.data
            selectedImageUri?.let {
                binding.img.setImageURI(it)
                isImageSelected = true //đã chọn ảnh
                updateImage()
            }
        }
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