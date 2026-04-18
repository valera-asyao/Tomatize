package ui

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.example.tomatize.ShopItem
import com.example.tomatize.UserData

object MascotOverlayRenderer {

    private const val CLOTHES_OFFSET_DP = 6f

    fun render(
        context: Context,
        container: FrameLayout,
        items: List<ShopItem>,
        clothesOffsetDp: Float = CLOTHES_OFFSET_DP
    ) {
        container.removeAllViews()
        val density = context.resources.displayMetrics.density

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
                    // Clothing PNGs have a slightly left-shifted canvas, so compensate here.
                    translationX = if (item.type == "clothes") clothesOffsetDp * density else 0f
                }

                container.addView(overlay)
            }
    }
}