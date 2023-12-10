package com.example.myapplication.Fragments

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.myapplication.R
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
            }
            recyclerView = recycleview
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.setHasFixedSize(true)

            swipeRefreshLayout = swipeRefresh
            swipeRefreshLayout.setOnRefreshListener {
                // Refresh data when swiped
                displayStudentInfo(currentUserUid)
                setupSpinner()
            }
            displayStudentInfo(currentUserUid)
        }

        // Inflate the layout for this fragment
        return binding.root
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