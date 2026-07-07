package com.example.androidlauncher.ui

import android.annotation.SuppressLint
import android.appwidget.AppWidgetHostView
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.SizeF
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.androidlauncher.data.HostedWidget
import kotlin.math.roundToInt

/**
 * Compose-Wrapper um eine [AppWidgetHostView] (B1). Die Host-View kommt gecacht aus
 * dem WidgetHostManager und wird hier nur in einen [BlockableFrameLayout] eingehängt –
 * im Edit-Modus fängt der Rahmen alle Touches ab, damit die Drag-Gesten des
 * umgebenden Home-Elements gewinnen; im Normalmodus bleibt das Widget voll interaktiv.
 */
@Composable
fun HostedWidgetView(
    widget: HostedWidget,
    hostView: AppWidgetHostView?,
    isEditMode: Boolean,
    modifier: Modifier = Modifier,
) {
    // Provider deinstalliert oder View (noch) nicht erzeugbar → nichts rendern;
    // der Orphan-Cleanup räumt den Eintrag beim nächsten Start weg.
    if (hostView == null) return

    AndroidView(
        factory = { context ->
            BlockableFrameLayout(context).apply {
                // Die gecachte Host-View kann noch am alten Rahmen hängen
                // (Composable wurde zwischenzeitlich disposed) – erst lösen.
                (hostView.parent as? ViewGroup)?.removeView(hostView)
                addView(
                    hostView,
                    FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    )
                )
            }
        },
        update = { frame ->
            frame.blockTouches = isEditMode
            // Gewuenschte Groesse spec-unabhaengig in px an den Frame geben (siehe
            // BlockableFrameLayout.onMeasure) – nur so schrumpft das Widget wieder.
            val d = frame.resources.displayMetrics.density
            frame.desiredWidthPx = (widget.widthDp * d).roundToInt()
            frame.desiredHeightPx = (widget.heightDp * d).roundToInt()
            frame.requestLayout()
            updateAppWidgetSizeCompat(hostView, widget.widthDp, widget.heightDp)
        },
        modifier = modifier
            .size(widget.widthDp.dp, widget.heightDp.dp)
            .clipToBounds()
    )
}

/** Meldet dem Provider die Anzeigegröße, damit er das passende RemoteViews-Layout liefert. */
private fun updateAppWidgetSizeCompat(hostView: AppWidgetHostView, widthDp: Int, heightDp: Int) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        hostView.updateAppWidgetSize(
            Bundle(),
            listOf(SizeF(widthDp.toFloat(), heightDp.toFloat()))
        )
    } else {
        @Suppress("DEPRECATION")
        hostView.updateAppWidgetSize(null, widthDp, heightDp, widthDp, heightDp)
    }
}

/**
 * FrameLayout, das bei gesetztem [blockTouches] alle Touch-Events abfängt und NICHT
 * behandelt – die Events gelten damit für Compose als unkonsumiert und erreichen die
 * Drag-Gesten des umgebenden Edit-Targets (Muster: Launcher-Edit-Modi).
 */
private class BlockableFrameLayout(context: Context) : FrameLayout(context) {

    var blockTouches = false

    /** Vom Compose-`update`-Block gesetzte Zielgroesse in px (0 = noch nicht gesetzt). */
    var desiredWidthPx = 0
    var desiredHeightPx = 0

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Groesse spec-unabhaengig erzwingen: AppWidgetHostView meldet beim
        // Verkleinern eine groessere Content-Min, und der AndroidView-Holder reicht
        // dem Kind nicht zwingend EXACTLY-Specs durch -> die View wuerde sonst nicht
        // schrumpfen ("wird groesser, uebernimmt die kleine Version nicht").
        if (desiredWidthPx > 0 && desiredHeightPx > 0) {
            val childWidthSpec = MeasureSpec.makeMeasureSpec(desiredWidthPx, MeasureSpec.EXACTLY)
            val childHeightSpec = MeasureSpec.makeMeasureSpec(desiredHeightPx, MeasureSpec.EXACTLY)
            for (i in 0 until childCount) {
                getChildAt(i).measure(childWidthSpec, childHeightSpec)
            }
            setMeasuredDimension(desiredWidthPx, desiredHeightPx)
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean =
        blockTouches || super.onInterceptTouchEvent(ev)

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean =
        if (blockTouches) false else super.onTouchEvent(event)
}
