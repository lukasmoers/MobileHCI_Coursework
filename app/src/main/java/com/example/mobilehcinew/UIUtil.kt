package com.example.mobilehcinew

import android.content.Context
import android.content.res.ColorStateList
import android.widget.Button
import android.widget.Toast
import androidx.core.content.ContextCompat

object UIUtil {

    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun updateButtonState(button: Button, context: Context, colorResId: Int) {
        button.backgroundTintList =
            ColorStateList.valueOf(ContextCompat.getColor(context, colorResId))
    }
}
