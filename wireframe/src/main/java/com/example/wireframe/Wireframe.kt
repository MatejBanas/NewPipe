package com.example.wireframe

import android.app.Activity
import android.graphics.*
import android.graphics.drawable.*
import android.os.Build
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.palette.graphics.Palette
import androidx.viewpager.widget.ViewPager
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
//        val startTime: Long = System.currentTimeMillis()

        if (activity.isDestroyed || activity.isFinishing) {
            Log.e(TAG, "Activity not loaded")
            return null
        }

        val rootViews: List<View> = getViewRoots()

        val rootView = activity.window.decorView.rootView

        val bitmap = Bitmap.createBitmap(rootView.width, rootView.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        try {
            rootView.background?.draw(canvas) ?: canvas.drawColor(Color.WHITE)
            drawWireframeRectangles(rootView, canvas)
            for (view in rootViews) {
                if (view != activity.window.decorView) {
                    drawWireframeRectangles(view, canvas)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error rendering wireframe", e)
            return null
        }

//        val time: Long = System.currentTimeMillis() - startTime
//        Log.e(TAG, "Wireframe rendering duration ms: - $time")

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
            if (!view.isVisible || view.alpha == 0.0f || !view.isAttachedToWindow) {
                return
            }

            val location = IntArray(2)
            view.getLocationOnScreen(location)
            val left = location[0]
            val top = location[1]
            val right = left + view.width
            val bottom = top + view.height
            val rect = Rect(left, top, right, bottom)

            when (view) {
                is TextView -> drawTextViewWireframe(canvas, view, rect)
                is ImageView -> drawImageViewWireframe(canvas, view, rect)
                is ViewPager -> {
                    drawViewPagerWireframe(canvas, view)
                    return
                }
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
     * Draws wireframe rectangles on the given [canvas] for the [currentItem] of the [viewPager].
     *
     * @param canvas the canvas on which the wireframe rectangles are drawn.
     * @param viewPager the view pager for which the wireframe rectangles are drawn.
     */
    private fun drawViewPagerWireframe(canvas: Canvas, viewPager: ViewPager) {
        val currentItem = viewPager.currentItem
        val currentView = viewPager.getChildAt(currentItem)

        drawWireframeRectangles(currentView, canvas)
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
            return when (val drawable = view.drawable) {
                is VectorDrawable -> {
                    Color.GREEN
                }
                is BitmapDrawable -> {
                    val bitmap = drawable.bitmap
                    getBitmapDominantColor(bitmap)
                }
                else -> {
                    Color.GREEN
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "An error occurred while getting the dominant color.", e)
            return Color.GREEN
        }
    }

    /**
     * Calculates the dominant color of the given [bitmap] using the Palette library.
     *
     * @param bitmap the bitmap for which the dominant color is calculated
     * @return the dominant color of the bitmap, or the default color [Color.GRAY] if the palette is unable to generate colors.
     */
    private fun getBitmapDominantColor(bitmap: Bitmap): Int {
        val palette = Palette.from(bitmap).generate()

        return palette.getDominantColor(Color.GRAY)
    }


    /**
     * Returns the color of the given view's background.
     *
     * @param view the view to get the color from
     * @return the background color of the view, or dark gray if the view has no background color
     */
    private fun getViewColor(view: View): Int {
        return when (val background = view.background) {
            is ColorDrawable -> background.color
            is GradientDrawable -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    val colors = background.colors
                    colors?.getOrNull(0) ?: Color.DKGRAY
                } else {
                    getViewColorUsingBitmap(view)
                }
            }
            is MaterialShapeDrawable -> background.fillColor?.defaultColor ?: Color.DKGRAY
            is RippleDrawable -> {
                val drawable = background.getDrawable(0)
                if (drawable is ColorDrawable) {
                    drawable.color
                } else {
                    Color.DKGRAY
                }
            }
            else -> {
                Log.i(TAG, "getViewColor: unsupported background type: ${background?.javaClass?.name}")
                getViewColorUsingBitmap(view)
            }
        }
    }

    /**
     * Calculates the dominant color of the given [view] by creating a bitmap of the view
     * and using the Palette library to calculate the dominant color.
     *
     * @param view the view for which the dominant color is calculated.
     * @return the dominant color of the view, or the default color [Color.GRAY]
     */
    private fun getViewColorUsingBitmap(view: View): Int {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)

        return getBitmapDominantColor(bitmap)
    }

    /**
     * Retrieves the list of currently visible root views.
     *
     * @return a list of currently visible root views.
     */
    private fun getViewRoots(): List<View> {
        val views = ArrayList<View>()

        try {
            val windowManager = Class.forName("android.view.WindowManagerGlobal")
                .getMethod("getInstance").invoke(null)

            val viewsField = windowManager.javaClass.getDeclaredField("mViews")
            viewsField.isAccessible = true

            val viewsFieldList = viewsField.get(windowManager) as ArrayList<View>
            for (view in viewsFieldList) {
                if (view.isVisible) {
                    views.add(view)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return views
    }
}