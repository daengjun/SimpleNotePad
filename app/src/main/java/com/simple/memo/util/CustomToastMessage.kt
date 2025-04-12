package com.simple.memo.util

import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.TextView
import android.widget.Toast
import com.simple.memo.R


object CustomToastMessage {

    fun createToast(context: Context, message: String): Toast {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.custom_toast, null)

        val textView: TextView = view.findViewById(R.id.tvSample)
        textView.text = message

        return Toast(context).apply {
            setGravity(Gravity.BOTTOM or Gravity.CENTER, 0, 16.toPx(context))
            duration = Toast.LENGTH_LONG
            this.view = view
        }
    }

    private fun Int.toPx(context: Context): Int =
        (this * context.resources.displayMetrics.density).toInt()
}