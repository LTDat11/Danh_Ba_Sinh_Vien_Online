package com.example.myapplication.Fragments

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.myapplication.R
import com.example.myapplication.activities.InfoActivity
import com.example.myapplication.adapter.StudentAdapter
import com.example.myapplication.databinding.FragmentHomeBinding
import com.example.myapplication.models.StudentInfo
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class HomeFragment : Fragment() {
    lateinit var binding: FragmentHomeBinding
    private val studentList = ArrayList<StudentInfo>()
    lateinit var recyclerView: RecyclerView
    lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private val INFO_ACTIVITY_REQUEST_CODE = 123
    private val selectedStudentIds = mutableListOf<String>()



    private val onItemClickListener = object : StudentAdapter.OnItemClickListener {
        override fun onItemClick(studentInfo: StudentInfo) {
            if (!isNetworkConnected()){
                Toast.makeText(requireContext(), "Vui lòng kiểm tra kết nối mạng và thử lại", Toast.LENGTH_SHORT).show()
            }else{
                // Xử lý khi một mục được nhấn
                // Lấy UID từ Firestore và chuyển sang màn hình chi tiết với thông tin sinh viên
                getUidFromFirestore(studentInfo.studentId, FirebaseAuth.getInstance().currentUser?.uid ?: "") { uid ->
                    val intent = Intent(requireContext(), InfoActivity::class.java)
                    intent.putExtra("studentUid", uid)
                    startActivityForResult(intent, INFO_ACTIVITY_REQUEST_CODE)
                }
            }
        }

        override fun onItemLongPress(studentInfo: StudentInfo, isLongPressed: Boolean, selectedStudentIds: List<String>) {
            // Xử lý khi item được long press
            if (isLongPressed) {
                this@HomeFragment.selectedStudentIds.clear()
                this@HomeFragment.selectedStudentIds.addAll(selectedStudentIds)
            } else {
                this@HomeFragment.selectedStudentIds.clear()
            }

            updateFabVisibility()
        }


    }

    private fun updateFabVisibility() {
        binding.fab.visibility = if (selectedStudentIds.isEmpty()) View.GONE else View.VISIBLE
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == INFO_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Làm mới dữ liệu khi từ trang info quay lại
            val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
            displayStudentInfo(currentUserUid)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentHomeBinding.inflate(layoutInflater,container,false)
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid

        binding.apply {

            setupSpinner()
            // Bắt sự kiện chọn của Spinner
            spinnerFillter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    // Thực hiện xắp xếp dựa trên mục được chọn
                    sortRecyclerView(position)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // Nếu không có mục nào được chọn
                }
            }

            materialCardView.setOnClickListener {
                searchview.isIconified = false

                searchview.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
                    override fun onQueryTextSubmit(query: String?): Boolean {
                        // Xử lý khi người dùng nhấn enter hoặc nút tìm kiếm trên bàn phím
                        return false
                    }

                    override fun onQueryTextChange(newText: String): Boolean {
                        // Xử lý khi người dùng thay đổi nội dung của SearchView
                        filter(newText)
                        return true
                    }
                })
            }

            recyclerView = recycleview
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.setHasFixedSize(true)

            swipeRefreshLayout = swipeRefresh
            swipeRefreshLayout.setOnRefreshListener {
                // Làm mới dữ liệu (kéo từ trên xuống)
                this@HomeFragment.selectedStudentIds.clear()
                updateFabVisibility()
                setupSpinner()
                clearSearchView()
                displayStudentInfo(currentUserUid)
            }
            displayStudentInfo(currentUserUid)

            fab.setOnClickListener {
                if (!isNetworkConnected()){
                    Toast.makeText(requireContext(), "Vui lòng kiểm tra kết nối mạng và thử lại", Toast.LENGTH_SHORT).show()
                }else{
                    showDeleteInfoConfirmationDialog()
                }
            }

        }

        // Inflate the layout for this fragment
        return binding.root
    }

    private fun clearSearchView() {
        binding.apply {
            searchview.clearFocus()
            searchview.setQuery("", false)
        }
    }

    private fun isNetworkConnected(): Boolean {
        val connectivityManager =
            requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private fun showDeleteInfoConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Xác nhận xóa thông tin ${selectedStudentIds.size} sinh viên được chọn")
            .setMessage("Bạn có chắc chắn muốn xóa thông tin của ${selectedStudentIds.size} sinh viên này?")
            .setPositiveButton("Xóa") { _, _ ->
                // Gọi hàm để xóa sinh viên dựa trên selectedStudentIds
                deleteStudents(selectedStudentIds)
                return@setPositiveButton
            }
            .setNegativeButton("Hủy bỏ", null)
            .show()
    }

    override fun onPause() {
        //reset lại selectedStudentIds
        super.onPause()
        selectedStudentIds.clear()
    }

    override fun onResume() {
        super.onResume()
        // Làm mới khi quay lại trang fragment home
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        displayStudentInfo(currentUserUid)
        updateFabVisibility()
    }

    private fun deleteStudents(selectedStudentIds: List<String>) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){
//                if (selectedStudentIds.isEmpty()){
//                    Toast.makeText(requireContext(), "không có nội dung được chọn để xóa (ấn giữ nội dung)", Toast.LENGTH_SHORT).show()
//                    return@withContext
//                }
                if (!isNetworkConnected()){
                    Toast.makeText(requireContext(), "Vui lòng kiểm tra kết nối mạng và thử lại", Toast.LENGTH_SHORT).show()
                }else{
                    for(studentId in selectedStudentIds){
                        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
                        getUidFromFirestore(studentId,currentUserUid ?: ""){studentUid ->
                            val firestore = FirebaseFirestore.getInstance()
                            if (currentUserUid != null){
                                firestore.collection("users")
                                    .document(currentUserUid)
                                    .collection("students")
                                    .document(studentUid)
                                    .get()
                                    .addOnSuccessListener {document ->
                                        if (document.exists()){
                                            val studentInfo = document.toObject(StudentInfo::class.java)
                                            if (studentInfo != null) {
                                                // Kiểm tra ảnh để xóa
                                                val imageUrl = studentInfo.imageUrl
                                                // Kiểm tra có phải là avtdf hay không ?
                                                if (imageUrl!!.contains("avtdf.jpg")){
                                                    // Nếu là avtdf, thực hiện xóa thông tin trên firestore
                                                    firestore.collection("users")
                                                        .document(currentUserUid)
                                                        .collection("students")
                                                        .document(studentUid)
                                                        .delete()
                                                        .addOnSuccessListener {
                                                            // Xóa thông tin thành công
                                                            binding.progressBar.visibility = View.GONE
                                                            Toast.makeText(requireContext(), "Xóa thông tin thành công", Toast.LENGTH_SHORT).show()
                                                            this@HomeFragment.selectedStudentIds.clear()
                                                            displayStudentInfo(currentUserUid)
                                                            updateFabVisibility()
                                                            clearSearchView()
                                                        }
                                                        .addOnFailureListener {
                                                            binding.progressBar.visibility = View.GONE
                                                            Toast.makeText(requireContext(), "Xóa thông tin thất bại", Toast.LENGTH_SHORT).show()
                                                            this@HomeFragment.selectedStudentIds.clear()
                                                            updateFabVisibility()
                                                        }
                                                }else{
                                                    // Nếu không phải là avtdf, xóa ảnh tương ứng trong storage và thông tin trên firestore
                                                    val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl.toString())
                                                    storageRef.delete()
                                                        .addOnSuccessListener {
                                                            //Xóa ảnh thành công, xóa tiếp thông tin trên firestore
                                                            firestore.collection("users")
                                                                .document(currentUserUid)
                                                                .collection("students")
                                                                .document(studentUid)
                                                                .delete()
                                                                .addOnSuccessListener {
                                                                    // Xóa thành công
                                                                    binding.progressBar.visibility = View.GONE
                                                                    Toast.makeText(requireContext(), "Xóa thông tin và ảnh thành công", Toast.LENGTH_SHORT).show()
                                                                    displayStudentInfo(currentUserUid)
                                                                    this@HomeFragment.selectedStudentIds.clear()
                                                                    updateFabVisibility()
                                                                    clearSearchView()
                                                                }
                                                                .addOnFailureListener {
                                                                    binding.progressBar.visibility = View.GONE
                                                                    Toast.makeText(requireContext(), "Xóa thông tin thất bại", Toast.LENGTH_SHORT).show()
                                                                    this@HomeFragment.selectedStudentIds.clear()
                                                                    updateFabVisibility()
                                                                }
                                                        }
                                                        .addOnFailureListener {
                                                            binding.progressBar.visibility = View.GONE
                                                            Toast.makeText(requireContext(), "Xóa ảnh thất bại", Toast.LENGTH_SHORT).show()
                                                            this@HomeFragment.selectedStudentIds.clear()
                                                            updateFabVisibility()
                                                        }
                                                }

                                            }else{
                                                binding.progressBar.visibility = View.GONE
                                                Toast.makeText(requireContext(), "Lỗi Không có thông tin", Toast.LENGTH_SHORT).show()
                                                this@HomeFragment.selectedStudentIds.clear()
                                                updateFabVisibility()
                                            }
                                        }
                                    }.addOnFailureListener { exception ->
                                        binding.progressBar.visibility = View.GONE
                                        Toast.makeText(requireContext(), "Lỗi khi lấy thông tin sinh viên", Toast.LENGTH_SHORT).show()
                                        this@HomeFragment.selectedStudentIds.clear()
                                        updateFabVisibility()
                                    }
                            }
                        }
                    }
                }

            }
        }
    }


    private fun getUidFromFirestore(studentId: String, currentUserUid: String, callback: (String) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){
                val firestore = FirebaseFirestore.getInstance()
                firestore.collection("users")
                    .document(currentUserUid)
                    .collection("students")
                    .whereEqualTo("studentId", studentId)
                    .get()
                    .addOnSuccessListener { result ->
                        if (!result.isEmpty) {
                            val document = result.documents[0]
                            val uid = document.id
                            callback(uid)
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Error getting UID from Firestore", exception)
                    }
            }
        }

    }

    //Lọc thông tin theo searchview
    private fun filter(query: String){
        val filteredList = ArrayList<StudentInfo>()
        for (student_info in studentList){
            if (student_info.name.toLowerCase().contains(query.toLowerCase()) || student_info.email.toLowerCase().contains(query.toLowerCase())
                || student_info.phoneNumber.toLowerCase().contains(query.toLowerCase()) || student_info.studentId.toLowerCase().contains(query.toLowerCase()) ){
                filteredList.add(student_info)
            }
        }

        val updatedSelectedStudentIds = selectedStudentIds.filter { selectedId ->
            filteredList.any { it.studentId == selectedId }
        }

        this@HomeFragment.selectedStudentIds.clear()
        this@HomeFragment.selectedStudentIds.addAll(updatedSelectedStudentIds)
        updateFabVisibility()

        val adapter = StudentAdapter(filteredList)
        adapter.setOnItemClickListener(onItemClickListener)
        recyclerView.adapter = adapter

        if (filteredList.isEmpty()){
            binding.tvNoItem?.visibility = View.VISIBLE
        }else{
            binding.tvNoItem?.visibility = View.GONE
        }

    }

    private fun sortRecyclerView(position: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){
                when (position) {
                    0 ->{
                        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
                        displayStudentInfo(currentUserUid)
                    }
                    1 -> {
                        // Sắp xếp theo Tên (A-Z)
                        studentList.sortBy { it.name }
                    }
                    2 -> {
                        // Sắp xếp theo Tên (Z-A)
                        studentList.sortByDescending { it.name }
                    }
                    3 -> {
                        // Sắp xếp theo Khóa (Tăng dần)
                        studentList.sortBy { it.studentId }
                    }
                    4 -> {
                        // Sắp xếp theo Khóa (Giảm dần)
                        studentList.sortByDescending { it.studentId }
                    }
                    else -> {
                        // Mặc định: không làm gì cả
                    }
                }

                // Cập nhật RecyclerView
                recyclerView.adapter?.notifyDataSetChanged()
            }
        }
    }

    private fun displayStudentInfo(currentUserUid: String?) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){
                val firestore = FirebaseFirestore.getInstance()
                if (currentUserUid != null) {
                    firestore.collection("users")
                        .document(currentUserUid)
                        .collection("students")
                        .get()
                        .addOnSuccessListener { result ->
                            studentList.clear()

                            for (document in result) {
                                val studentInfo = document.toObject(StudentInfo::class.java)
                                studentList.add(studentInfo)
                            }

                            val adapter = StudentAdapter(studentList)
                            adapter.setOnItemClickListener(onItemClickListener)
                            recyclerView.adapter = adapter
                            adapter.notifyDataSetChanged()
                            binding.sumInfo.text = studentList.size.toString()
                            binding.tvNoItem.visibility = if (studentList.isEmpty()) View.VISIBLE else View.GONE

                            swipeRefreshLayout.isRefreshing = false
                        }
                        .addOnFailureListener { exception ->
                            swipeRefreshLayout.isRefreshing = false
                        }
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    binding.apply {
                        recycleview.visibility = View.VISIBLE
                        linearlayout.visibility = View.VISIBLE
                        progressBar.visibility = View.GONE
                    }
                }, 1200)
            }
        }
    }

    private fun setupSpinner() {
        val list = resources.getStringArray(R.array.fillter)
        val adapterSpinner = ArrayAdapter(requireContext(),android.R.layout.simple_spinner_item,list)
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerFillter.adapter = adapterSpinner
    }

}