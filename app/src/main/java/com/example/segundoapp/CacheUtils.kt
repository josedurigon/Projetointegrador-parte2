package com.example.segundoapp

import android.content.Context

object CacheUtils {
    fun clear(context: Context) {
        context.cacheDir?.let { cacheDir ->
            if (cacheDir.exists()) {
                val files = cacheDir.listFiles()
                if (files != null) {
                    for (file in files) {
                        file.delete()
                    }
                }
            }
        }
    }
}
