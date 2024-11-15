package com.capztone.admin.models

data class AllMenu(
    val key: String? = null,
    var foodName: ArrayList<String>? = null, // Change from ArrayList<String>? to String?
    val foodPrice: String? = null,
    val foodDescription: String? = null,
    var quantity: String? = null,
    val foodImage: String? = null,
    val category: String? = null,
    val productQuantity: String?= null,
    var stock: String? = null,
    val CreatedDate: String? = null,
    val CreatedBy: String? = null,
)

