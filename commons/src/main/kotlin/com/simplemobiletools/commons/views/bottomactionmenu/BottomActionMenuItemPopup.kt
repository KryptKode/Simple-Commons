package com.simplemobiletools.commons.views.bottomactionmenu

import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.view.*
import android.view.View.MeasureSpec
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ListView
import android.widget.PopupWindow
import androidx.core.content.ContextCompat
import androidx.core.widget.PopupWindowCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.simplemobiletools.commons.R
import com.simplemobiletools.commons.extensions.applyColorFilter
import com.simplemobiletools.commons.extensions.windowManager
import com.simplemobiletools.commons.helpers.isRPlus
import kotlinx.android.synthetic.main.item_action_mode_popup.view.cab_item

class BottomActionMenuItemPopup(
    private val context: Context,
    private val items: List<BottomActionMenuItem>,
    private val onSelect: (BottomActionMenuItem) -> Unit,
) {
    private val popup = PopupWindow(context, null, android.R.attr.popupMenuStyle)
    private var anchorView: View? = null
    private var dropDownWidth = ViewGroup.LayoutParams.WRAP_CONTENT
    private var dropDownHeight = ViewGroup.LayoutParams.WRAP_CONTENT
    private val tempRect = Rect()
    private val popupMinWidth: Int
    private val popupPaddingBottom: Int
    private val popupPaddingStart: Int
    private val popupPaddingEnd: Int
    private val popupPaddingTop: Int

    val isShowing: Boolean
        get() = popup.isShowing

    private val popupListAdapter = BottomActionItemPopupAdapter {
        onSelect.invoke(it)
        popup.dismiss()
    }.apply {
        submitList(items)
    }

    init {
        popup.isFocusable = true
        popupMinWidth = context.resources.getDimensionPixelSize(R.dimen.cab_popup_menu_min_width)
        popupPaddingStart = context.resources.getDimensionPixelSize(R.dimen.smaller_margin)
        popupPaddingEnd = context.resources.getDimensionPixelSize(R.dimen.smaller_margin)
        popupPaddingTop = context.resources.getDimensionPixelSize(R.dimen.smaller_margin)
        popupPaddingBottom = context.resources.getDimensionPixelSize(R.dimen.smaller_margin)
    }

    fun show(anchorView: View) {
        this.anchorView = anchorView
        popupListAdapter.submitList(items)
        buildDropDown()
        PopupWindowCompat.setWindowLayoutType(popup, WindowManager.LayoutParams.TYPE_APPLICATION_SUB_PANEL)
        popup.isOutsideTouchable = true
        popup.width = dropDownWidth
        popup.height = dropDownHeight
        var x = 0
        var y = 0
        val contentView: View = popup.contentView
        val windowRect = Rect()
        contentView.getWindowVisibleDisplayFrame(windowRect)
        val windowW = windowRect.width()
        val windowH = windowRect.height()
        contentView.measure(
            makeDropDownMeasureSpec(dropDownWidth, windowW),
            makeDropDownMeasureSpec(dropDownHeight, windowH)
        )

        val anchorLocation = IntArray(2)
        anchorView.getLocationInWindow(anchorLocation)
        x += anchorLocation[0]

        y += anchorView.height * 2
        x -= dropDownWidth - anchorView.width

        popup.showAtLocation(contentView, Gravity.BOTTOM, x, y)
    }

    internal fun dismiss() {
        popup.dismiss()
        popup.contentView = null
    }

    private fun buildDropDown() {
        var otherHeights = 0
        val dropDownList = RecyclerView(context).apply {
            adapter = popupListAdapter
            isFocusable = true
            isFocusableInTouchMode = true
            clipToPadding = false
            isVerticalScrollBarEnabled = true
            isHorizontalScrollBarEnabled = false
            clipToOutline = true
            elevation = 3f
            layoutManager = LinearLayoutManager(context)
            setPaddingRelative(popupPaddingStart, popupPaddingTop, popupPaddingEnd, popupPaddingBottom)
        }

        val screenWidth = if (isRPlus()) {
            context.windowManager.currentWindowMetrics.bounds.width()
        } else {
            context.windowManager.defaultDisplay.width
        }

        val width = measureMenuSizeAndGetWidth((0.8 * screenWidth).toInt())
        val popupBackground = popup.background
        dropDownWidth = if (popupBackground != null) {
            popupBackground.getPadding(tempRect)
            tempRect.left + tempRect.right + width
        } else {
            width
        }

        // getMaxAvailableHeight() subtracts the padding, so we put it back
        // to get the available height for the whole window.
        val padding: Int = if (popupBackground != null) {
            popupBackground.getPadding(tempRect)
            tempRect.top + tempRect.bottom
        } else {
            tempRect.setEmpty()
            0
        }

        popup.contentView = dropDownList

        val maxHeight = popup.getMaxAvailableHeight(anchorView!!, 0)
        val listContent = measureHeightOfChildrenCompat(maxHeight - otherHeights)
        if (listContent > 0) {
            val listPadding = dropDownList.paddingTop + dropDownList.paddingBottom
            otherHeights += padding + listPadding
        }

        dropDownHeight = listContent + otherHeights
        dropDownList.layoutParams = ViewGroup.LayoutParams(dropDownWidth, dropDownHeight)
    }

    /**
     * @see androidx.appcompat.widget.DropDownListView.measureHeightOfChildrenCompat
     */
    private fun measureHeightOfChildrenCompat(maxHeight: Int): Int {
        val parent = FrameLayout(context)
        val widthMeasureSpec = MeasureSpec.makeMeasureSpec(dropDownWidth, View.MeasureSpec.EXACTLY)

        // Include the padding of the list
        var returnedHeight = 0

        val count = popupListAdapter.itemCount
        for (i in 0 until count) {
            val positionType = popupListAdapter.getItemViewType(i)

            val vh = popupListAdapter.createViewHolder(parent, positionType)
            popupListAdapter.bindViewHolder(vh, i)
            val itemView = vh.itemView

            // Compute child height spec
            val heightMeasureSpec: Int
            var childLp: ViewGroup.LayoutParams? = itemView.layoutParams

            if (childLp == null) {
                childLp = generateDefaultLayoutParams()
                itemView.layoutParams = childLp
            }

            heightMeasureSpec = if (childLp.height > 0) {
                MeasureSpec.makeMeasureSpec(
                    childLp.height,
                    View.MeasureSpec.EXACTLY
                )
            } else {
                MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
            }
            itemView.measure(widthMeasureSpec, heightMeasureSpec)
            // Since this view was measured directly against the parent measure
            // spec, we must measure it again before reuse.
            itemView.forceLayout()

            val marginLayoutParams = childLp as? ViewGroup.MarginLayoutParams
            val topMargin = marginLayoutParams?.topMargin ?: 0
            val bottomMargin = marginLayoutParams?.bottomMargin ?: 0
            val verticalMargin = topMargin + bottomMargin

            returnedHeight += itemView.measuredHeight + verticalMargin

            if (returnedHeight >= maxHeight) {
                // We went over, figure out which height to return.  If returnedHeight >
                // maxHeight, then the i'th position did not fit completely.
                return maxHeight
            }
        }

        // At this point, we went through the range of children, and they each
        // completely fit, so return the returnedHeight
        return returnedHeight
    }


    /**
     * @see androidx.appcompat.view.menu.MenuPopup.measureIndividualMenuWidth
     */
    private fun measureMenuSizeAndGetWidth(maxAllowedWidth: Int): Int {
        val parent = FrameLayout(context)
        var menuWidth = popupMinWidth

        val widthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        val heightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)

        val count = popupListAdapter.itemCount
        for (i in 0 until count) {
            val positionType = popupListAdapter.getItemViewType(i)

            val vh = popupListAdapter.createViewHolder(parent, positionType)
            popupListAdapter.bindViewHolder(vh, i)
            val itemView = vh.itemView
            itemView.measure(widthMeasureSpec, heightMeasureSpec)

            val itemWidth = itemView.measuredWidth
            if (itemWidth >= maxAllowedWidth) {
                return maxAllowedWidth
            } else if (itemWidth > menuWidth) {
                menuWidth = itemWidth
            }
        }

        return menuWidth
    }

    private fun makeDropDownMeasureSpec(measureSpec: Int, maxSize: Int): Int {
        return MeasureSpec.makeMeasureSpec(
            getDropDownMeasureSpecSize(measureSpec, maxSize),
            getDropDownMeasureSpecMode(measureSpec)
        )
    }

    private fun getDropDownMeasureSpecSize(measureSpec: Int, maxSize: Int): Int {
        return when (measureSpec) {
            ViewGroup.LayoutParams.MATCH_PARENT -> maxSize
            else -> MeasureSpec.getSize(measureSpec)
        }
    }

    private fun getDropDownMeasureSpecMode(measureSpec: Int): Int {
        return when (measureSpec) {
            ViewGroup.LayoutParams.WRAP_CONTENT -> MeasureSpec.UNSPECIFIED
            else -> MeasureSpec.EXACTLY
        }
    }

    private fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
