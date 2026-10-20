package com.xboard.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

fun Context.openBrowser(url: String): Boolean {
    if (url.isBlank()) return false
    return try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        true
    } catch (e: Exception) {
        Toast.makeText(this, "无法打开链接", Toast.LENGTH_SHORT).show()
        false
    }
}

fun Context.shareText(text: String, title: String = "分享") {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    startActivity(Intent.createChooser(intent, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
