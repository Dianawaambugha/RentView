package com.example.rentview.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.widget.ImageView
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

object ImageLoader {

    private val executor = Executors.newFixedThreadPool(2)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.allocationByteCount / 1024
        }
    }

    fun load(imageView: ImageView, uriString: String?, targetPx: Int) {

        imageView.tag = uriString
        imageView.setImageDrawable(null)

        if (uriString.isNullOrBlank()) return

        val cached = cache.get(uriString)

        if (cached != null) {
            imageView.setImageBitmap(cached)
            return
        }

        val appContext = imageView.context.applicationContext

        executor.execute {

            val bitmap = try {
                if (uriString.startsWith("https://") ||
                    uriString.startsWith("http://")
                ) {
                    decodeRemote(uriString, targetPx)
                } else {
                    decodeLocal(appContext, uriString, targetPx)
                }
            } catch (_: Exception) {
                null
            } catch (_: OutOfMemoryError) {
                null
            }

            if (bitmap != null) {
                cache.put(uriString, bitmap)
            }

            mainHandler.post {
                if (bitmap != null && imageView.tag == uriString) {
                    imageView.setImageBitmap(bitmap)
                }
            }
        }
    }

    private fun decodeRemote(
        imageUrl: String,
        targetPx: Int
    ): Bitmap? {

        val connection = URL(imageUrl).openConnection()
                as HttpURLConnection

        return try {

            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 20000
            connection.doInput = true
            connection.connect()

            if (connection.responseCode !in 200..299) {
                return null
            }

            val imageBytes = connection.inputStream.use {
                it.readBytes()
            }

            decodeBytes(imageBytes, targetPx)

        } finally {
            connection.disconnect()
        }
    }

    private fun decodeBytes(
        imageBytes: ByteArray,
        targetPx: Int
    ): Bitmap? {

        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        ByteArrayInputStream(imageBytes).use {
            BitmapFactory.decodeStream(it, null, bounds)
        }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null
        }

        var sample = 1

        while (
            bounds.outWidth / (sample * 2) >= targetPx &&
            bounds.outHeight / (sample * 2) >= targetPx
        ) {
            sample *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
        }

        return BitmapFactory.decodeByteArray(
            imageBytes,
            0,
            imageBytes.size,
            options
        )
    }

    private fun decodeLocal(
        context: Context,
        uriString: String,
        targetPx: Int
    ): Bitmap? {

        val uri = Uri.parse(uriString)
        val resolver = context.contentResolver

        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, bounds)
        }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            return null
        }

        var sample = 1

        while (
            bounds.outWidth / (sample * 2) >= targetPx &&
            bounds.outHeight / (sample * 2) >= targetPx
        ) {
            sample *= 2
        }

        val options = BitmapFactory.Options().apply {
            inSampleSize = sample
        }

        return resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    }
}