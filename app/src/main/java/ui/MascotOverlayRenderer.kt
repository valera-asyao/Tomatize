package ui

import android.content.Context
import android.graphics.BitmapFactory
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.example.tomatize.R
import com.example.tomatize.ShopItem
import com.example.tomatize.UserData
import kotlin.math.min

object MascotOverlayRenderer {

    private const val MIN_ALPHA = 20

    private val imageInfoCache = mutableMapOf<Int, ImageInfo>()

    fun render(
        context: Context,
        container: FrameLayout,
        items: List<ShopItem>
    ) {
        container.removeAllViews()

        items.sortedBy { UserData.overlaySortOrder(it.type) }
            .forEach { item ->
                val overlay = ImageView(context).apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                    )
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setImageResource(item.overlayRes)
                    contentDescription = null
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                }

                container.addView(overlay)

                if (item.type == "clothes") {
                    placeClothes(context, container, overlay, item.overlayRes)
                }
            }
    }

    private fun placeClothes(
        context: Context,
        container: FrameLayout,
        overlay: ImageView,
        resId: Int
    ) {
        if (container.width > 0 && container.height > 0) {
            overlay.translationX = getCenterShift(
                context,
                resId,
                container.width,
                container.height
            )
            return
        }

        container.addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                view: View,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int
            ) {
                val viewWidth = right - left
                val viewHeight = bottom - top
                if (viewWidth == 0 || viewHeight == 0) {
                    return
                }

                container.removeOnLayoutChangeListener(this)
                overlay.translationX = getCenterShift(context, resId, viewWidth, viewHeight)
            }
        })
    }

    private fun getCenterShift(context: Context, resId: Int, viewWidth: Int, viewHeight: Int): Float {
        if (viewWidth == 0 || viewHeight == 0) return 0f

        val itemInfo = getImageInfo(context, resId) ?: return 0f
        val mascotInfo = getImageInfo(context, R.drawable.tomagochi) ?: return 0f

        val itemCenter = getShownCenterX(viewWidth, viewHeight, itemInfo)
        val mascotCenter = getShownCenterX(
            viewWidth,
            viewHeight,
            mascotInfo
        )

        return mascotCenter - itemCenter
    }

    private fun getShownCenterX(
        viewWidth: Int,
        viewHeight: Int,
        info: ImageInfo
    ): Float {
        val scale = min(
            viewWidth.toFloat() / info.width.toFloat(),
            viewHeight.toFloat() / info.height.toFloat()
        )
        val imageLeft = (viewWidth - info.width * scale) / 2f

        return imageLeft + info.centerX * scale
    }

    private fun getImageInfo(context: Context, resId: Int): ImageInfo? {
        if (imageInfoCache.containsKey(resId)) {
            return imageInfoCache[resId]
        }

        val options = BitmapFactory.Options()
        options.inScaled = false
        val bitmap = BitmapFactory.decodeResource(context.resources, resId, options)
            ?: return null

        var sumAlpha = 0L
        var sumX = 0L

        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                val alpha = bitmap.getPixel(x, y) ushr 24
                if (alpha > MIN_ALPHA) {
                    sumAlpha += alpha.toLong()
                    sumX += x.toLong() * alpha.toLong()
                }
            }
        }

        val centerX = if (sumAlpha == 0L) {
            bitmap.width / 2f
        } else {
            sumX.toFloat() / sumAlpha.toFloat()
        }

        val info = ImageInfo(bitmap.width, bitmap.height, centerX)

        bitmap.recycle()
        imageInfoCache[resId] = info
        return info
    }

    private data class ImageInfo(
        val width: Int,
        val height: Int,
        val centerX: Float
    )
}
