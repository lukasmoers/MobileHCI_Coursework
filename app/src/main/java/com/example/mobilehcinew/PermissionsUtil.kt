package com.example.mobilehcinew

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object PermissionsUtil {
    fun hasRecordAudioPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context, android.Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
}
