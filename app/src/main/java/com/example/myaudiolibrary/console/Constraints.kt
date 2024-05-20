package com.example.myaudiolibrary.console

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstrainedLayoutReference
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.ConstraintSetScope
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.HorizontalChainReference
import androidx.constraintlayout.compose.HorizontalChainScope
import androidx.constraintlayout.compose.VerticalChainReference
import androidx.constraintlayout.compose.VerticalChainScope
import androidx.constraintlayout.compose.Visibility
import androidx.constraintlayout.compose.atMost
import com.example.myaudiolibrary.core.compose.Range
import com.example.myaudiolibrary.core.compose.WindowSize

private const val TAG = "ConstraintSets"
@Stable
interface Constraints {
    val titleTextSize: TextUnit
    val value: ConstraintSet

    companion object {
        const val ID_SIGNATURE = "_signature"
        const val ID_CLOSE_BTN = "_close"
        const val ID_ARTWORK = "_artwork"
        const val ID_TITLE = "_title"
        const val ID_POSITION = "_position"
        const val ID_SUBTITLE = "_subtitle"
        const val ID_CONTROLS = "_controls"
        const val ID_TIME_BAR = "_time_bar"
        const val ID_OPTIONS = "_options"
        const val ID_MESSAGE = "_message"
        const val ID_BACKGROUND = "_background"
        const val ID_SCRIM = "_scrim"
    }
}

private val REF_SIGNATURE = ConstrainedLayoutReference(Constraints.ID_SIGNATURE)
private val REF_CLOSE_BTN = ConstrainedLayoutReference(Constraints.ID_CLOSE_BTN)
private val REF_ARTWORK = ConstrainedLayoutReference(Constraints.ID_ARTWORK)
private val REF_TITLE = ConstrainedLayoutReference(Constraints.ID_TITLE)
private val REF_POSITION = ConstrainedLayoutReference(Constraints.ID_POSITION)
private val REF_SUBTITLE = ConstrainedLayoutReference(Constraints.ID_SUBTITLE)
private val REF_CONTROLS = ConstrainedLayoutReference(Constraints.ID_CONTROLS)
private val REF_TIME_BAR = ConstrainedLayoutReference(Constraints.ID_TIME_BAR)
private val REF_OPTIONS = ConstrainedLayoutReference(Constraints.ID_OPTIONS)
private val REF_MESSAGE = ConstrainedLayoutReference(Constraints.ID_MESSAGE)
private val REF_BACKGROUND = ConstrainedLayoutReference(Constraints.ID_BACKGROUND)
private val REF_SCRIM = ConstrainedLayoutReference(Constraints.ID_SCRIM)


private fun ConstraintSetScope.horizontal(
    vararg elements: ConstrainedLayoutReference,
    chainStyle: ChainStyle = ChainStyle.Spread,
    constrainBlock: HorizontalChainScope.() -> Unit
): HorizontalChainReference {
    val chain = createHorizontalChain(*elements, chainStyle = chainStyle)
    constrain(chain, constrainBlock)
    return chain
}

private fun ConstraintSetScope.vertical(
    vararg elements: ConstrainedLayoutReference,
    chainStyle: ChainStyle = ChainStyle.Spread,
    constrainBlock: VerticalChainScope.() -> Unit
): VerticalChainReference {
    val chain = createVerticalChain(*elements, chainStyle = chainStyle)
    constrain(chain, constrainBlock)
    return chain
}

private fun ConstraintSet(
    titleTextSize: TextUnit,
    description: ConstraintSetScope.() -> Unit
) = object : Constraints {
    override val titleTextSize: TextUnit
        get() = titleTextSize
    override val value: ConstraintSet = ConstraintSet(description)
}

private fun Compact(
    insets: DpRect,
    compact: Boolean,
) = ConstraintSet(if (compact) 16.sp else 44.sp) {
    val (left, up, right, down) = insets

    constrain(REF_MESSAGE){
        linkTo(parent.start, parent.end)
        bottom.linkTo(parent.bottom, 8.dp)
    }

    constrain(REF_SIGNATURE) {
        visibility = Visibility.Gone
    }

    constrain(REF_SCRIM) {
        linkTo(parent.start, parent.top, parent.end, parent.bottom)
        width = Dimension.fillToConstraints
        height = Dimension.fillToConstraints
        visibility = Visibility.Gone
    }

    // The background
    constrain(REF_BACKGROUND) {
        linkTo(parent.start, parent.top, parent.end, parent.bottom)
        width = Dimension.fillToConstraints
        height = Dimension.fillToConstraints
    }

    // TopRow Constitutes Close button and options
    constrain(REF_CLOSE_BTN) {
        start.linkTo(parent.start, 8.dp + left)
        top.linkTo(parent.top, 8.dp + up)
    }

    constrain(REF_OPTIONS) {
        end.linkTo(parent.end, 8.dp + right)
        top.linkTo(REF_CLOSE_BTN.top)
        bottom.linkTo(REF_CLOSE_BTN.bottom)
    }

    horizontal(
        REF_ARTWORK,
        REF_TITLE,
        chainStyle = ChainStyle.Packed(0f),
        constrainBlock = {
            start.linkTo(parent.start, 8.dp + left)
            end.linkTo(parent.end, 8.dp + right)
        }
    )

    constrain(REF_ARTWORK) {
        top.linkTo(REF_CLOSE_BTN.bottom, 8.dp)
        bottom.linkTo(REF_CONTROLS.top, 8.dp)
        width = Dimension.percent(0.3f)
        height = Dimension.ratio(if (compact) "1:1" else "0.8")
        val scale = if (compact) 0.6f else 0.7f
        scaleY = scale
        scaleX = scale
    }

    vertical(
        REF_POSITION,
        REF_TITLE,
        REF_SUBTITLE,
        chainStyle = ChainStyle.Packed(if (compact) 0.5f else 0.4f),
        constrainBlock = {
            top.linkTo(REF_ARTWORK.top)
            bottom.linkTo(REF_ARTWORK.bottom)
        }
    )

    constrain(REF_TITLE) {
        width = Dimension.fillToConstraints
    }

    constrain(REF_SUBTITLE) {
        start.linkTo(REF_TITLE.start)
    }

    constrain(REF_POSITION) {
        start.linkTo(REF_TITLE.start)
    }

    constrain(REF_CONTROLS) {
        start.linkTo(parent.start, 8.dp + left)
        end.linkTo(parent.end, 8.dp + right)
        bottom.linkTo(
            if (!compact) REF_TIME_BAR.top else parent.bottom,
            if (!compact) 8.dp else down + 8.dp
        )
    }

    constrain(REF_TIME_BAR) {
        start.linkTo(parent.start, 8.dp + left)
        end.linkTo(parent.end, 8.dp + right)
        bottom.linkTo(parent.bottom, 16.dp + down)
        width = Dimension.fillToConstraints
        visibility = if (compact) Visibility.Gone else Visibility.Visible
    }
}
private fun Portrait(
    insets: DpRect,
    compact: Boolean
) = ConstraintSet(44.sp) {
    val (left, up, right, down) = insets

    constrain(REF_MESSAGE){
        linkTo(parent.start, parent.end)
        top.linkTo(REF_TIME_BAR.bottom, 8.dp)
    }

    constrain(REF_SCRIM) {
        linkTo(parent.start, parent.top, parent.end, parent.bottom)
        width = Dimension.fillToConstraints
        height = Dimension.fillToConstraints
        visibility = Visibility.Gone
    }

    horizontal(
        REF_SIGNATURE,
        REF_CLOSE_BTN,
        chainStyle = ChainStyle.SpreadInside,
        constrainBlock = {
            start.linkTo(parent.start, 16.dp + left)
            end.linkTo(parent.end, 16.dp + right)
        }
    )
    constrain(REF_SIGNATURE) {
        top.linkTo(parent.top, 16.dp + up)
        visibility = if (compact) Visibility.Gone else Visibility.Visible
    }

    constrain(REF_CLOSE_BTN) {
        top.linkTo(REF_SIGNATURE.top)
        bottom.linkTo(REF_SIGNATURE.bottom)
        visibility = if (compact) Visibility.Gone else Visibility.Visible
    }

    // Artwork Row
    constrain(REF_ARTWORK) {
        start.linkTo(parent.start, 16.dp + left)
        end.linkTo(parent.end, 16.dp + right)
        top.linkTo(REF_SIGNATURE.bottom, up)
        bottom.linkTo(REF_POSITION.top)
        width = Dimension.fillToConstraints
        height = Dimension.ratio("1:1")
        scaleY = 0.8f
        scaleX = 0.8f
    }

    vertical(
        REF_TITLE,
        REF_TIME_BAR,
        REF_CONTROLS,
        REF_OPTIONS,
        chainStyle = ChainStyle.Spread,
        constrainBlock = {
            top.linkTo(REF_ARTWORK.bottom, 16.dp)
            bottom.linkTo(parent.bottom, down + 16.dp)
        }
    )

    // Title
    constrain(REF_TITLE) {
        start.linkTo(parent.start, left + 32.dp)
        end.linkTo(parent.end, right + 32.dp)
        width = Dimension.fillToConstraints

    }
    // Position + Subtitle
    constrain(REF_POSITION) {
        start.linkTo(REF_TITLE.start)
        bottom.linkTo(REF_TITLE.top)
        width = Dimension.wrapContent
    }

    constrain(REF_SUBTITLE) {
        start.linkTo(REF_POSITION.end, 4.dp)
        bottom.linkTo(REF_POSITION.bottom)
        end.linkTo(REF_TITLE.end)
        width = Dimension.fillToConstraints
    }

    constrain(REF_TIME_BAR) {
        start.linkTo(parent.start, left + 32.dp)
        end.linkTo(parent.end, right + 32.dp)
        width = Dimension.fillToConstraints
        top.linkTo(REF_ARTWORK.bottom, 16.dp)
    }

    // Controls
    constrain(REF_CONTROLS) {
        start.linkTo(parent.start, left + 16.dp)
        end.linkTo(parent.end, right + 16.dp)
        top.linkTo(REF_TIME_BAR.bottom, 16.dp)
    }

    // Options
    constrain(REF_OPTIONS) {
        start.linkTo(parent.start, left + 32.dp)
        end.linkTo(parent.end, right + 32.dp)
    }
}
private fun Landscape(
    insets: DpRect,
    compact: Boolean
) = ConstraintSet(44.sp) {

    val (left, up, right, down) = insets

    constrain(REF_MESSAGE){
        linkTo(parent.start, parent.end)
        top.linkTo(REF_TIME_BAR.bottom, 8.dp)
    }

    constrain(REF_SCRIM) {
        linkTo(parent.start, parent.top, parent.end, parent.bottom)
        width = Dimension.fillToConstraints
        height = Dimension.fillToConstraints
        visibility = Visibility.Gone
    }

    constrain(REF_SIGNATURE) {
        // Will be set to visible in future releases.
        visibility = if (compact) Visibility.Gone else Visibility.Invisible
        end.linkTo(REF_CLOSE_BTN.end)
    }

    vertical(
        REF_CLOSE_BTN,
        REF_SIGNATURE,
        chainStyle = ChainStyle.SpreadInside,
        constrainBlock = {
            top.linkTo(parent.top, 16.dp + up)
            bottom.linkTo(parent.bottom, 16.dp + down)
        }
    )

    constrain(REF_CLOSE_BTN) {
        start.linkTo(parent.start, 16.dp + left)
        visibility = if (compact) Visibility.Gone else Visibility.Visible
    }

    horizontal(
        REF_ARTWORK,
        REF_TITLE,
        chainStyle = ChainStyle.Spread,
        constrainBlock = {
            start.linkTo(REF_CLOSE_BTN.end,4.dp)
            end.linkTo(parent.end, 32.dp + right)
        }
    )

    // Artwork Row
    constrain(REF_ARTWORK) {
        top.linkTo(parent.top, 16.dp + up)
        // Make artwork middle only if not compact
        if (!compact)
            bottom.linkTo(parent.bottom, 16.dp + down)
        width = Dimension.percent(0.35f)
        height = Dimension.ratio("1:1")
        scaleY = if (compact) 0.7f else 0.9f
        scaleX = if (compact) 0.7f else 0.9f
    }

    constrain(REF_TITLE) {
        start.linkTo(REF_ARTWORK.end, 22.dp)
        end.linkTo(REF_CLOSE_BTN.start, 16.dp)
        top.linkTo(REF_ARTWORK.top, 32.dp)
        width = Dimension.fillToConstraints
    }

    constrain(REF_POSITION) {
        start.linkTo(REF_TITLE.start)
        bottom.linkTo(REF_TITLE.top)
    }

    constrain(REF_SUBTITLE) {
        start.linkTo(REF_POSITION.end, 2.dp)
        bottom.linkTo(REF_POSITION.bottom)
        end.linkTo(REF_TITLE.end, 32.dp)
        width = Dimension.fillToConstraints
    }

    constrain(REF_CONTROLS) {
        end.linkTo(REF_TITLE.end)
        start.linkTo(REF_TITLE.start)
        top.linkTo(REF_TITLE.bottom, 16.dp)
    }
    constrain(REF_TIME_BAR) {
        end.linkTo(REF_TITLE.end)
        start.linkTo(REF_TITLE.start)
        top.linkTo(REF_CONTROLS.bottom, 16.dp)
        width = Dimension.fillToConstraints
    }

    constrain(REF_OPTIONS) {
        end.linkTo(REF_TIME_BAR.end)
        top.linkTo(REF_TIME_BAR.bottom)
    }
}

private fun Medium(
    insets: DpRect
) = ConstraintSet(16.sp) {
    val (left, up, right, down) = insets

    constrain(REF_MESSAGE){
        linkTo(parent.start, parent.end)
        top.linkTo(REF_TIME_BAR.bottom, 8.dp)
    }

    constrain(REF_SCRIM) {
        linkTo(parent.start, parent.top, parent.end, parent.bottom)
        width = Dimension.fillToConstraints
        height = Dimension.fillToConstraints
        visibility = Visibility.Gone
    }

    // This Config of controller doesnt support signature
    constrain(REF_SIGNATURE) {
        visibility = Visibility.Gone
    }

    constrain(REF_CLOSE_BTN) {
        end.linkTo(parent.end, 16.dp + right)
        top.linkTo(parent.top, 22.dp + up)
    }

    // Artwork Row
    constrain(REF_ARTWORK) {
        start.linkTo(parent.start, 16.dp)
        end.linkTo(parent.end, 16.dp)
        top.linkTo(REF_CLOSE_BTN.bottom)
        bottom.linkTo(REF_OPTIONS.top)
        width = Dimension.fillToConstraints.atMost(420.dp)
        height = Dimension.ratio("1:1")
        scaleY = 0.8f
        scaleX = 0.8f
    }

    constrain(REF_TIME_BAR) {
        start.linkTo(parent.start, left + 32.dp)
        end.linkTo(parent.end, right + 32.dp)
        bottom.linkTo(REF_CONTROLS.top, 8.dp)
        width = Dimension.fillToConstraints
    }

    constrain(REF_CONTROLS) {
        bottom.linkTo(parent.bottom, down + 16.dp)
        horizontalChainWeight = 0.3f
    }

    constrain(REF_POSITION) {
        start.linkTo(REF_TIME_BAR.start, 16.dp)
        bottom.linkTo(REF_TIME_BAR.top)
    }

    horizontal(
        REF_TITLE,
        REF_CONTROLS,
        chainStyle = ChainStyle.Packed,
        constrainBlock = {
            start.linkTo(REF_TIME_BAR.start)
            end.linkTo(REF_TIME_BAR.end)
        }
    )

    constrain(REF_TITLE) {
        width = Dimension.fillToConstraints
        top.linkTo(REF_CONTROLS.top)
        bottom.linkTo(REF_CONTROLS.bottom)
        horizontalChainWeight = 0.3f
    }

    constrain(REF_SUBTITLE) {
        bottom.linkTo(REF_TITLE.top)
        start.linkTo(REF_TITLE.start)
    }

    constrain(REF_OPTIONS) {
        end.linkTo(parent.end, right + 32.dp)
        bottom.linkTo(REF_TIME_BAR.top)
    }

}
private fun Large(
    insets: DpRect
) = ConstraintSet(16.sp) {
    val (left, up, right, down) = insets

    constrain(REF_MESSAGE){
        linkTo(parent.start, parent.end)
        top.linkTo(REF_TIME_BAR.bottom, 8.dp)
    }

    constrain(REF_SCRIM) {
        linkTo(parent.start, parent.top, parent.end, parent.bottom)
        width = Dimension.fillToConstraints
        height = Dimension.fillToConstraints
        visibility = Visibility.Gone
    }

    // This Config of controller doesnt support signature
    constrain(REF_SIGNATURE) {
        visibility = Visibility.Gone
    }

    constrain(REF_CLOSE_BTN) {
        end.linkTo(parent.end, 16.dp + right)
        top.linkTo(parent.top, 22.dp + up)
    }

    // Artwork Row
    constrain(REF_ARTWORK) {
        start.linkTo(parent.start, 16.dp)
        bottom.linkTo(REF_TIME_BAR.top)
        width = Dimension.fillToConstraints.atMost(420.dp)
        height = Dimension.ratio("1:1")
        scaleY = 0.8f
        scaleX = 0.8f
    }

    constrain(REF_TIME_BAR) {
        start.linkTo(parent.start, left + 32.dp)
        end.linkTo(parent.end, right + 32.dp)
        bottom.linkTo(REF_CONTROLS.top, 8.dp)
        width = Dimension.fillToConstraints
    }


    constrain(REF_POSITION) {
        start.linkTo(REF_TIME_BAR.start, 16.dp)
        bottom.linkTo(REF_TIME_BAR.top)
    }

    horizontal(
        REF_TITLE,
        REF_CONTROLS,
        REF_OPTIONS,
        chainStyle = ChainStyle.SpreadInside,
        constrainBlock = {
            start.linkTo(REF_TIME_BAR.start, 32.dp)
            end.linkTo(REF_TIME_BAR.end)
        }
    )

    constrain(REF_CONTROLS) {
        bottom.linkTo(parent.bottom, down + 16.dp)
    }

    constrain(REF_TITLE) {
        top.linkTo(REF_CONTROLS.top)
        bottom.linkTo(REF_CONTROLS.bottom)
    }

    constrain(REF_SUBTITLE) {
        bottom.linkTo(REF_TITLE.top)
        start.linkTo(REF_TITLE.start)
    }

    constrain(REF_OPTIONS) {
        top.linkTo(REF_CONTROLS.top)
        bottom.linkTo(REF_CONTROLS.bottom)
    }
}

fun calculateConstraintSet(
    windowSize: WindowSize,
    insets: DpRect,
): Constraints {

    val (wReach, hReach) = windowSize
    val (width, height) = windowSize.value

    return when {
        wReach == Range.Compact && hReach == Range.Compact -> Compact(
            insets,
            height < 300.dp
        )
        hReach > Range.Medium && wReach > Range.Medium -> Large(insets)
        hReach > Range.Compact && wReach > Range.Compact -> Medium(insets)

        wReach < hReach -> Portrait(
            insets,
            height < 600.dp
        )
        else -> Landscape(
            insets,
            width < 650.dp )
    }
}
