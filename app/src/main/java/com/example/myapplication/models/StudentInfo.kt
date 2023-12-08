package com.example.myapplication.models

import java.io.Serializable

data class StudentInfo(
    val name: String = "",
    val dateOfBirth: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val major: String = "",
    val studentId: String = "",
    val classId: String = "",
    val course: String = "",
    val imageUrl: String? = null
): Serializable
