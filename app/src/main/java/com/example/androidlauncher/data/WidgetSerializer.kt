package com.example.androidlauncher.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Serialisiert die Liste der gehosteten System-Widgets nach/aus JSON
 * (Muster: [FolderSerializer]). Korrupte Eingaben liefern eine leere Liste –
 * ein halb geparster Widget-Bestand waere inkonsistent zum AppWidgetHost.
 */
object WidgetSerializer {

    private const val KEY_APP_WIDGET_ID = "appWidgetId"
    private const val KEY_PROVIDER = "provider"
    private const val KEY_WIDTH_DP = "widthDp"
    private const val KEY_HEIGHT_DP = "heightDp"
    private const val KEY_OFFSET_X = "offsetX"
    private const val KEY_OFFSET_Y = "offsetY"

    fun parseWidgets(jsonString: String): List<HostedWidget> {
        val list = mutableListOf<HostedWidget>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    HostedWidget(
                        appWidgetId = obj.getInt(KEY_APP_WIDGET_ID),
                        provider = obj.getString(KEY_PROVIDER),
                        widthDp = obj.getInt(KEY_WIDTH_DP),
                        heightDp = obj.getInt(KEY_HEIGHT_DP),
                        offsetX = obj.optDouble(KEY_OFFSET_X, 0.0).toFloat(),
                        offsetY = obj.optDouble(KEY_OFFSET_Y, 0.0).toFloat(),
                    )
                )
            }
        } catch (_: JSONException) {
            return emptyList()
        }
        return list
    }

    fun serializeWidgets(widgets: List<HostedWidget>): String {
        val jsonArray = JSONArray()
        widgets.forEach { widget ->
            val obj = JSONObject()
            obj.put(KEY_APP_WIDGET_ID, widget.appWidgetId)
            obj.put(KEY_PROVIDER, widget.provider)
            obj.put(KEY_WIDTH_DP, widget.widthDp)
            obj.put(KEY_HEIGHT_DP, widget.heightDp)
            obj.put(KEY_OFFSET_X, widget.offsetX.toDouble())
            obj.put(KEY_OFFSET_Y, widget.offsetY.toDouble())
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }
}
