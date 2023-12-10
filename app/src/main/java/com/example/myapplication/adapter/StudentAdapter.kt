package com.example.myapplication.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.models.StudentInfo

class StudentAdapter(private  val studentList: ArrayList<StudentInfo>): RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    inner class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.img_student_info)
        val nameTextView: TextView = itemView.findViewById(R.id.tv_name_of_student)
        val phoneNumberTextView: TextView = itemView.findViewById(R.id.tv_phone_of_student)
        val studentIdTextView: TextView = itemView.findViewById(R.id.tv_id_of_student)
        val emailTextView: TextView = itemView.findViewById(R.id.tv_email_of_student)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.item_info_student,
            parent,false)
        return StudentViewHolder(itemView)
    }

    override fun onBindViewHolder(holder:StudentViewHolder, position: Int) {
        val currentItem = studentList[position]
        Glide.with(holder.imageView.context).load(currentItem.imageUrl).into(holder.imageView) // setImageURI từ URL
        holder.nameTextView.text = currentItem.name
        holder.phoneNumberTextView.text = currentItem.phoneNumber
        holder.studentIdTextView.text = currentItem.studentId
        holder.emailTextView.text = currentItem.email
    }

    override fun getItemCount(): Int {
        return studentList.size
    }

}