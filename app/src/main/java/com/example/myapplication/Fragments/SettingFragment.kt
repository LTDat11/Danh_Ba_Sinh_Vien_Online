package com.example.myapplication.Fragments

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.activities.ChangePassActivity
import com.example.myapplication.databinding.FragmentSettingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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
            loadUserInfo() //Hàm để load thông tin người dùng

            //Xóa tất cả danh bạ

            btnDeleteAll.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    withContext(Dispatchers.Main){
                        if(!isNetworkConnected()){
                            Toast.makeText(requireContext(), "Vui lòng kiểm tra kết nối mạng và thử lại", Toast.LENGTH_SHORT).show()
                        }else{
                            showDeleAllConfirmationDialog()
                        }
                    }
                }
            }


            //Đổi mật khẩu
            btnChangePass.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    withContext(Dispatchers.Main) {
                        if (!isNetworkConnected()){
                            Toast.makeText(requireContext(), "Vui lòng kiểm tra kết nối mạng và thử lại", Toast.LENGTH_SHORT).show()
                        }else{
                            val intent = Intent (requireContext(), ChangePassActivity::class.java)
                            startActivity(intent)
//                            requireActivity().supportFragmentManager.beginTransaction()
//                                .replace(R.id.frame_layout, ChangePassFragment())
//                                .commit()
                        }
                    }
                }
            }

            //Đăng xuất
            btnLogout.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    withContext(Dispatchers.Main){
                        if (!isNetworkConnected()){
                            Toast.makeText(requireContext(), "Vui lòng kiểm tra kết nối mạng và thử lại", Toast.LENGTH_SHORT).show()
                        }else{
                            showLogoutConfirmationDialog()
                        }
                    }
                }
            }

            //Chỉnh sửa image
            iconEditImg.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch{
                    withContext(Dispatchers.Main){
                        if (!isNetworkConnected()){
                            Toast.makeText(requireContext(), "Vui lòng kiểm tra kết nối mạng và thử lại", Toast.LENGTH_SHORT).show()
                        }else{
                            openImageChooser()
                        }
                    }
                }
            }

            //Xóa image
            iconDeleteImg.setOnClickListener{
                CoroutineScope(Dispatchers.IO).launch {
                    withContext(Dispatchers.Main){
                        if (!isNetworkConnected()){
                            Toast.makeText(requireContext(), "Vui lòng kiểm tra kết nối mạng và thử lại", Toast.LENGTH_SHORT).show()
                        }else{
                            showDeleteConfirmationDialog()
                        }
                    }
                }
            }

            //edit name
            iconEditName.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch{
                    withContext(Dispatchers.Main){
                        if (!isNetworkConnected()){
                            Toast.makeText(requireContext(), "Vui lòng kiểm tra kết nối mạng và thử lại", Toast.LENGTH_SHORT).show()
                        }else{
                            showEditNameDialog()
                        }
                    }
                }
            }


        }

        // Inflate the layout for this fragment
        return binding.root
    }

    private fun showDeleAllConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận xóa tất cả các danh bạ")
            .setMessage("Bạn có chắc chắn muốn xóa tất cả các danh bạ đã lưu?")
            .setPositiveButton("Xóa") { _, _ ->
                deleteAllStudentDocuments()
            }
            .setNegativeButton("Hủy bỏ", null)
            .show()
    }

    private fun deleteAllStudentDocuments() {
        val userUid = FirebaseAuth.getInstance().currentUser?.uid
        // Đường dẫn đến thư mục /profile_images/userUid
        val storageRef = storage.reference.child("profile_images").child(userUid!!)

        val studentCollectionRef = firestore.collection("users").document(userUid!!).collection("students")

        storageRef.listAll()
            .addOnSuccessListener { result ->
                // Xóa tất cả các item trong thư mục
                for (item in result.items) {
                    item.delete()
                }

                // Xóa thư mục
                storageRef.delete()
                    .addOnSuccessListener {
                        return@addOnSuccessListener
                    }
                    .addOnFailureListener { exception ->
                        return@addOnFailureListener
                    }
            }
            .addOnFailureListener { exception ->
                return@addOnFailureListener
            }

        studentCollectionRef
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(requireContext(), "Không có nội dung để xóa", Toast.LENGTH_SHORT).show()
                } else {
                    // Xóa tất cả documents trong collection
                    for (document in documents) {
                        document.reference.delete()
                    }

                    // Sau khi xóa tất cả documents, bạn có thể xóa cả collection nếu cần
                    firestore.collection("students").document().delete()
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Xóa thành công", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(requireContext(), "Lỗi, xóa thất bại !!", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Lỗi, không thể kiểm tra collection: $exception", Toast.LENGTH_SHORT).show()
            }
    }


    private fun isNetworkConnected(): Boolean {
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
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

            Handler(Looper.getMainLooper()).postDelayed({
                progressBar.visibility = View.GONE
                img.visibility = View.VISIBLE
                iconEditImg.visibility = View.VISIBLE
                iconDeleteImg.visibility = View.VISIBLE
                tvName.visibility = View.VISIBLE
                iconEditName.visibility = View.VISIBLE
                tvEmail.visibility = View.VISIBLE
            }, 1000)
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
        val email = binding.tvEmail.text.toString()
        currentUser?.let { user ->
            // Tạo đường dẫn tới ảnh trên Firebase Storage
            val imageRef = storageRef.child("profile_images/$email/${user.uid}.jpg")

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
        //Chọn ảnh từ album
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