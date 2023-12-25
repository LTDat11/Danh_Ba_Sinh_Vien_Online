package com.example.myapplication.adapter

import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.models.StudentInfo

class StudentAdapter(private val studentList: ArrayList<StudentInfo>): RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {
    var longPressedPositions: MutableSet<Int> = mutableSetOf()
    var selectedStudentIds: MutableList<String> = mutableListOf()
    inner class StudentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.img_student_info)
        val nameTextView: TextView = itemView.findViewById(R.id.tv_name_of_student)
        val phoneNumberTextView: TextView = itemView.findViewById(R.id.tv_phone_of_student)
        val studentIdTextView: TextView = itemView.findViewById(R.id.tv_id_of_student)
        val emailTextView: TextView = itemView.findViewById(R.id.tv_email_of_student)
        val imgView:ImageView = itemView.findViewById(R.id.imgView)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener?.onItemClick(studentList[position])
                }
            }

            itemView.setOnLongClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    if (longPressedPositions.contains(position)) {
                        longPressedPositions.remove(position)
                        val studentIdToRemove = studentList[position].studentId
                        selectedStudentIds.remove(studentIdToRemove)
                    } else {
                        longPressedPositions.add(position)
                        val studentIdToAdd = studentList[position].studentId
                        selectedStudentIds.add(studentIdToAdd)
                    }
                    notifyDataSetChanged()
                    listener?.onItemLongPress(studentList[position], !longPressedPositions.isEmpty(), selectedStudentIds)
                }
                true
            }
        }

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

        if (longPressedPositions.contains(position)) {
            // Đặt dấu tích khi ấn giữ lâu
            holder.imgView.visibility = View.VISIBLE
        } else {
            // Ẩn dấu tích khi không ấn giữ lâu
            holder.imgView.visibility = View.GONE
        }
        //Nhấn vào menu trên item
        holder.itemView.findViewById<ImageButton>(R.id.menuitem).setOnClickListener {
            var phoneNumber = holder.phoneNumberTextView.text.toString()
            var email =  holder.emailTextView.text.toString()
            var name = holder.nameTextView.text.toString()
            var studentId = holder.studentIdTextView.text.toString()
            showPopupMenu(it,phoneNumber,email,name,studentId)
        }
    }

    private fun showPopupMenu(
        it: View?,
        phoneNumber: String,
        email: String,
        name: String,
        studentId: String
    ) {
        val popupMenu = PopupMenu(it!!.context, it)
        popupMenu.menuInflater.inflate(R.menu.popup_menu, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menuCall -> {
                    // Handle call action
                    handleCallAction(it,phoneNumber)
                    true
                }
                R.id.menuMessage -> {
                    // Handle message action
                    handleMessageAction(it,phoneNumber)
                    true
                }
                R.id.menuEmail -> {
                    // Handle email action
                    handleEmailAction(it, email)
                    true
                }
                R.id.menuShare -> {
                    shareStudentInfo(it, phoneNumber, email,name,studentId)
                    true
                }
                else -> false
            }
        }

        // Show the popup menu
        popupMenu.show()
    }

    private fun shareStudentInfo(
        it: View,
        phoneNumber: String,
        email: String,
        name: String,
        studentId: String
    ) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"

        val shareText = "Họ và tên: $name\n" +"Số điện thoại: $phoneNumber\n" + "Email: $email\n" + "Mã số: $studentId"

        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText)
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Thông tin sinh viên:")

        val chooser = Intent.createChooser(shareIntent, "Chia sẻ thông tin sinh viên")
        if (shareIntent.resolveActivity(it.context.packageManager) != null) {
            it.context.startActivity(chooser)
        } else {
            Toast.makeText(it.context, "Không có ứng dụng để chọn", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleMessageAction(it: View, phoneNumber: String) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.fromParts("sms", phoneNumber, null)
        if (intent.resolveActivity(it.context.packageManager) != null) {
            it.context.startActivity(intent)
        } else {
            Toast.makeText(it.context, "Lỗi", Toast.LENGTH_SHORT).show()
        }

    }

    private fun handleEmailAction(it: View, email: String) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:$email")

        if (intent.resolveActivity(it.context.packageManager) != null) {
            it.context.startActivity(intent)
        } else {
            Toast.makeText(it.context, "Lỗi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleCallAction(it: View, phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$phoneNumber")

        if (intent.resolveActivity(it.context.packageManager) != null) {
            it.context.startActivity(intent)
        } else {
            Toast.makeText(it.context, "Lỗi", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int {
        return studentList.size
    }

    interface OnItemClickListener {
        fun onItemClick(studentInfo: StudentInfo)

        fun onItemLongPress(studentInfo: StudentInfo, isLongPressed: Boolean, selectedStudentIds: List<String>)
    }

    private var listener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }


}