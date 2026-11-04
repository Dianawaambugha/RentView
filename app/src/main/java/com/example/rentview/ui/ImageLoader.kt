package com.example.rentview.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.widget.ImageView
import java.util.concurrent.Executors

// Loads a property photo (a content:// URI chosen by the owner) into an ImageView.
// The photo is decoded on a background thread and shrunk to roughly the size
// it will be shown at, so large camera photos do not run the phone out of memory.
object ImageLoader {

    private val executor = Executors.newFixedThreadPool(2)
    private val mainHandler = Handler(Looper.getMainLooper())

    // Remembers recent photos so scrolling back up is instant.
    // The cache budget is 1/8 of the app's memory, measured in kilobytes.
    private val cache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt()
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.allocationByteCount / 1024
        }
    }

    // targetPx = about how many pixels wide or tall the photo will be shown.
    fun load(imageView: ImageView, uriString: String?, targetPx: Int) {

        // RecyclerView reuses views. The tag lets a slow load know the view has
        // since been given a different photo, so it must not overwrite it.
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
            val bitmap = decode(appContext, uriString, targetPx)

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

    // Returns null if the photo cannot be opened (deleted, permission lost, etc.).
    private fun decode(context: Context, uriString: String, targetPx: Int): Bitmap? {
        return try {
            val uri = Uri.parse(uriString)
            val resolver = context.contentResolver

            // First pass: read only the photo's size.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }

            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                null
            } else {
                // Halve the photo while it is still at least target size.
                var sample = 1
                while (bounds.outWidth / (sample * 2) >= targetPx &&
                    bounds.outHeight / (sample * 2) >= targetPx
                ) {
                    sample *= 2
                }

                // Second pass: decode the shrunk photo.
                val options = BitmapFactory.Options().apply { inSampleSize = sample }
                resolver.openInputStream(uri)?.use {
                    BitmapFactory.decodeStream(it, null, options)
                }
            }
        } catch (e: Exception) {
            null
        } catch (e: OutOfMemoryError) {
            null
        }
    }
}
