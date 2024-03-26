package com.example.admin.models

data class AddProduct(
    val name: String,
    val selectCategory: String,
    val stockCheck: String,
    val price: Double,
    val stock: Int,
    val imageResource: Int)
