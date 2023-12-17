package com.example.myapplication.Fragments

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SearchView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.myapplication.R
import com.example.myapplication.activities.InfoActivity
import com.example.myapplication.adapter.StudentAdapter
import com.example.myapplication.databinding.FragmentHomeBinding
import com.example.myapplication.models.StudentInfo
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class HomeFragment : Fragment() {
    lateinit var binding: FragmentHomeBinding
    private val studentList = ArrayList<StudentInfo>()
    lateinit var recyclerView: RecyclerView
    lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private val INFO_ACTIVITY_REQUEST_CODE = 123



    private val onItemClickListener = object : StudentAdapter.OnItemClickListener {
        override fun onItemClick(studentInfo: StudentInfo) {
            // Xử lý khi một mục được nhấn
            // Lấy UID từ Firestore và chuyển sang màn hình chi tiết với thông tin sinh viên
            getUidFromFirestore(studentInfo.studentId, FirebaseAuth.getInstance().currentUser?.uid ?: "") { uid ->
                val intent = Intent(requireContext(), InfoActivity::class.java)
                intent.putExtra("studentUid", uid)
                startActivityForResult(intent, INFO_ACTIVITY_REQUEST_CODE)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == INFO_ACTIVITY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            // Refresh data when returning from InfoActivity
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
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
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
                displayStudentInfo(currentUserUid)
                setupSpinner()
            }
            displayStudentInfo(currentUserUid)
        }

        // Inflate the layout for this fragment
        return binding.root
    }

    private fun getUidFromFirestore(studentId: String, currentUserUid: String, callback: (String) -> Unit) {
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

    //Lọc thông tin theo searchview
    private fun filter(query: String){
        val filteredList = ArrayList<StudentInfo>()
        for (student_info in studentList){
            if (student_info.name.toLowerCase().contains(query.toLowerCase()) || student_info.email.toLowerCase().contains(query.toLowerCase())
                || student_info.phoneNumber.toLowerCase().contains(query.toLowerCase()) || student_info.studentId.toLowerCase().contains(query.toLowerCase()) ){
                filteredList.add(student_info)
            }
        }
        val adapter = StudentAdapter(filteredList)
        recyclerView.adapter = adapter
    }

    private fun sortRecyclerView(position: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            withContext(Dispatchers.Main){
                when (position) {
                    0 -> {
                        // Sắp xếp theo Tên (A-Z)
                        studentList.sortBy { it.name }
                    }
                    1 -> {
                        // Sắp xếp theo Tên (Z-A)
                        studentList.sortByDescending { it.name }
                    }
                    2 -> {
                        // Sắp xếp theo Khóa (Tăng dần)
                        studentList.sortBy { it.studentId }
                    }
                    3 -> {
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