package com.example.wireframe

import android.app.Activity
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.RippleDrawable
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.children
import androidx.palette.graphics.Palette
import com.google.android.material.shape.MaterialShapeDrawable

object Wireframe {
    private const val TAG = "Wireframe"

    /**
     * Renders the wireframe for an [Activity] by drawing rectangles for each view in the activity's
     * view hierarchy and returning the resulting [Bitmap].
     *
     * @param activity the [Activity] to render the wireframe for
     * @return the rendered wireframe as a [Bitmap]
     */
    fun renderWireframe(activity: Activity): Bitmap? {
        if (activity.isDestroyed || activity.isFinishing || !activity.hasWindowFocus()) {
            Log.e(TAG, "Activity not loaded")
            return null
        }

        val rootView = activity.window.decorView.rootView
        val bitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        try {
            rootView.background?.draw(canvas) ?: canvas.drawColor(Color.WHITE)
            drawWireframeRectangles(rootView, canvas)
        } catch (e: Exception) {
            Log.e(TAG, "Error rendering wireframe", e)
            return null
        }

        return bitmap
    }

    /**
     * Draws a wireframe rectangle on the provided canvas for the given view.
     *
     * @param view The view to draw a wireframe rectangle around.
     * @param canvas The canvas to draw on.
     */
    private fun drawWireframeRectangles(view: View, canvas: Canvas) {
        try {
            if (view.visibility != View.VISIBLE) {
                return
            }

            val rect = Rect()
            view.getGlobalVisibleRect(rect)

            when (view) {
                is TextView -> drawTextViewWireframe(canvas, view, rect)
                is ImageView -> drawImageViewWireframe(canvas, view, rect)
                else -> drawViewWireframe(canvas, view, rect)
            }

            if (view is ViewGroup) {
                for (child in view.children) {
                    drawWireframeRectangles(child, canvas)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error while drawing wireframes", e)
        }
    }

    /**
     * Draws a wireframe for a [TextView].
     *
     * @param canvas The canvas on which the wireframe will be drawn.
     * @param textView The [TextView] to draw a wireframe for.
     * @param rect The global bounds of the [TextView].
     */
    private fun drawTextViewWireframe(canvas: Canvas, textView: TextView, rect: Rect) {
        try {
            val layout = textView.layout ?: return

            val lineCount = layout.lineCount
            val lineBounds = Rect()

            for (i in 0 until lineCount) {
                val lineStart = layout.getLineStart(i)
                val lineEnd = layout.getLineEnd(i)

                val line = textView.text.subSequence(lineStart, lineEnd).toString()

                textView.paint.getTextBounds(line, 0, line.length, lineBounds)

                val lineRect = Rect(rect)
                lineRect.top += layout.getLineTop(i) + textView.paddingTop
                lineRect.bottom = lineRect.top + layout.getLineBottom(i) - layout.getLineTop(i)

                lineRect.right = lineRect.left + lineBounds.width()

                val paint = Paint().apply {
                    style = Paint.Style.FILL
                    color = textView.currentTextColor
                }

                canvas.drawRect(lineRect, paint)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error drawing wireframe for TextView", e)
        }
    }

    /**
     * Draws a wireframe rectangle for an [ImageView].
     *
     * @param canvas The canvas to draw on.
     * @param imageView The [ImageView] to draw a wireframe rectangle for.
     * @param rect The bounds of the [ImageView].
     */
    private fun drawImageViewWireframe(canvas: Canvas, imageView: ImageView, rect: Rect) {
        val color = getImageViewColor(imageView)
        val paint = Paint().apply {
            style = Paint.Style.FILL
            this.color = color
        }
        canvas.drawRect(rect, paint)
    }

    /**
     * Draws a wireframe rectangle for a generic [View].
     *
     * @param canvas The canvas to draw on.
     * @param view The view to draw a wireframe rectangle for.
     * @param rect The bounds of the [View].
     */
    private fun drawViewWireframe(canvas: Canvas, view: View, rect: Rect) {
        val color = getViewColor(view)
        val paint = Paint().apply {
            style = Paint.Style.FILL
            this.color = color
        }
        canvas.drawRect(rect, paint)
    }

    /**
     * Retrieves the dominant color of an [ImageView] based on its drawable.
     *
     * @param view The [ImageView] to retrieve the dominant color of.
     * @return The dominant color of the [ImageView].
     */
    private fun getImageViewColor(view: ImageView): Int {
        try {
            val bitmap = (view.drawable as? BitmapDrawable)?.bitmap

            if (bitmap == null) {
                // TODO get color of view.drawable is VectorDrawable
                Log.i(TAG, "ImageView does not have a bitmap drawable.")
                return Color.GREEN
            }

            val palette = Palette.from(bitmap).generate()

            return palette.getDominantColor(Color.GREEN)
        } catch (e: Exception) {
            Log.e(TAG, "An error occurred while getting the dominant color.", e)
            return Color.GREEN
        }
    }

    /**
     * Calculates the average color of a bitmap and returns it as an RGB integer.
     * If the bitmap is empty or null, it returns Color.BLACK.
     * @param bitmap the bitmap to calculate the average color for
     * @return the average color of the bitmap as an RGB integer
     */
    fun getAverageColorRGB(bitmap: Bitmap?): Int {
        if (bitmap == null || bitmap.width == 0 || bitmap.height == 0) {
            Log.i(TAG, "Bitmap is null or empty")
            return Color.BLACK
        }

        val pixelList = mutableListOf<Int>()
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val pixelColor = bitmap.getPixel(x, y)
                if (pixelColor != 0) {
                    pixelList.add(pixelColor)
                }
            }
        }

        if (pixelList.isEmpty()) {
            Log.i(TAG, "Bitmap has no non-zero pixels")
            return Color.BLACK
        }

        val redSum = pixelList.map { Color.red(it) }.sum()
        val greenSum = pixelList.map { Color.green(it) }.sum()
        val blueSum = pixelList.map { Color.blue(it) }.sum()
        val size = pixelList.size

        val redAvg = redSum / size
        val greenAvg = greenSum / size
        val blueAvg = blueSum / size

        return Color.rgb(redAvg, greenAvg, blueAvg)
    }

    /**
     * Returns the color of the given view's background.
     *
     * @param view the view to get the color from
     * @return the background color of the view, or white if the view has no background color
     */
    private fun getViewColor(view: View): Int {
        return when (val background = view.background) {
            is ColorDrawable -> background.color
            // TODO requires API 24
//            is GradientDrawable -> {
//                val colors = background.colors
//                colors?.getOrNull(0) ?: Color.WHITE
//            }
            is MaterialShapeDrawable -> background.fillColor?.defaultColor ?: Color.WHITE
            is RippleDrawable -> {
                val drawable = background.getDrawable(0)
                if (drawable is ColorDrawable) {
                    drawable.color
                } else {
                    Color.WHITE
                }
            }
            else -> {
                Log.i(TAG, "getViewColor: unsupported background type: ${background?.javaClass?.name}")
                Color.WHITE
            }
        }
    }
}