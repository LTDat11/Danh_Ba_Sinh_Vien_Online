package com.example.myapplication.Fragments

import android.app.DatePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentAddBinding
import java.time.LocalDate
import java.util.Calendar

class AddFragment : Fragment() {
    lateinit var binding: FragmentAddBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentAddBinding.inflate(layoutInflater,container,false)

        binding.apply {

            btnDate.setOnClickListener {
                val currentDate = Calendar.getInstance()
                val year = currentDate.get(Calendar.YEAR)
                val month = currentDate.get(Calendar.MONTH)
                val day = currentDate.get(Calendar.DAY_OF_MONTH)

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
        }

        // Inflate the layout for this fragment
        return binding.root
    }

}