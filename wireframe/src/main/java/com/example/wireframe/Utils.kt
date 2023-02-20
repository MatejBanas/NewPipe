package com.example.wireframe

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object Utils {

    /**
     * Saves a bitmap image to the internal storage of the app with the specified filename.
     *
     * @param bitmap The bitmap image to save.
     * @param context The context of the app.
     * @param filename The filename of the saved image.
     */
    fun saveWireframeToInternalStorage(bitmap: Bitmap, context: Context, filename: String) {
        val directory = context.getDir("images", Context.MODE_PRIVATE)
        val file = File(directory, "${filename}.png")

        try {
            FileOutputStream(file).use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
        } catch (e: IOException) {
            Log.e("WIREFRAME", "Error while saving wireframe", e)
        }
    }
}