package com.simple.memo.util


object TextSizeUtils {
    fun getTextSizeValue(selectedSize: String?): Float {
        return when (selectedSize) {
            "small" -> 14f
            "medium" -> 16f
            "large" -> 18f
            else -> 16f
        }
    }
}
