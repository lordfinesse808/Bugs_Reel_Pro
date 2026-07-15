package com.bugreel.pro.core.util

import android.content.Context
import android.widget.Toast
import java.time.Instant

fun String.toInstant(): Instant = Instant.parse(this)

fun Long.toInstant(): Instant = Instant.ofEpochMilli(this)

fun Context.showToast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}
