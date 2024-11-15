package com.capztone.admin.models

data class DiscountItem(
    val foodPrices:String? = null,
    val key:String? = null,
    var foodNames: ArrayList<String>? = null,
    val foodDescriptions:String?=null,
    val quantitys:String? = null,
    val foodImages:String? = null,
    val categorys:String? = null,
    val discounts:String? = null,
    val productQuantity: String?= null,
    var stocks: String? = null,
    val CreatedDate: String? = null,
    val CreatedBy: String?= null,
    val updatedDate: String? = null,
    val updatedBy: String? = null,
)
