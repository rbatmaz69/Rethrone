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

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // Compose liefert via Modifier.size() EXACTLY-Specs. AppWidgetHostView meldet
        // beim Verkleinern gerne eine groessere measuredWidth/Height (Content-Min),
        // wodurch das Widget im Compose-Layout zwar schrumpft, die View selbst aber
        // nicht -> "wird groesser, uebernimmt die kleine Version nicht". Daher die
        // angeforderte Groesse hart durchsetzen und die Kinder darauf messen.
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY &&
            MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY
        ) {
            val w = MeasureSpec.getSize(widthMeasureSpec)
            val h = MeasureSpec.getSize(heightMeasureSpec)
            val childWidthSpec = MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY)
            val childHeightSpec = MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY)
            for (i in 0 until childCount) {
                getChildAt(i).measure(childWidthSpec, childHeightSpec)
            }
            setMeasuredDimension(w, h)
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
