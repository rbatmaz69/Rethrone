package com.example.androidlauncher.data.backup

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/** Ein einzelner Preference-Eintrag mit Typ-Tag (siehe [BackupValueType]). */
data class BackupEntry(
    val key: String,
    val type: BackupValueType,
    val value: Any,
)

/**
 * Typ-Tag eines [BackupEntry]. Pflicht, weil `Preferences.asMap()` untypisiert ist
 * und der Restore den exakt typisierten Preferences-Key rekonstruieren muss –
 * ein Int-Wert unter einem Long-Key (oder Float unter Double) würde alle Reads
 * still shadowen.
 */
enum class BackupValueType(val tag: String) {
    STRING("string"),
    BOOLEAN("boolean"),
    INT("int"),
    LONG("long"),
    FLOAT("float"),
    DOUBLE("double"),
    STRING_SET("stringSet"),
    ;

    companion object {
        fun fromTag(tag: String): BackupValueType? = entries.firstOrNull { it.tag == tag }

        /** Leitet das Tag aus der Runtime-Klasse eines DataStore-Werts ab; unbekannte Typen → null. */
        fun fromValue(value: Any): BackupValueType? = when (value) {
            is String -> STRING
            is Boolean -> BOOLEAN
            is Int -> INT
            is Long -> LONG
            is Float -> FLOAT
            is Double -> DOUBLE
            is Set<*> -> STRING_SET
            else -> null // z. B. ByteArray – im Projekt ungenutzt, bewusst nicht unterstützt.
        }
    }
}

/** Ein exportiertes Widget – bewusst ohne appWidgetId (gerätespezifisch). */
data class WidgetBackup(
    val provider: String,
    val widthDp: Int,
    val heightDp: Int,
    val offsetX: Float,
    val offsetY: Float,
)

/** Vollständiger Inhalt einer Backup-Datei. */
data class BackupSnapshot(
    val backupVersion: Int,
    val appVersionCode: Long,
    val exportedAtMs: Long,
    val stores: Map<String, List<BackupEntry>>,
    val widgets: List<WidgetBackup>,
)

/**
 * Serialisiert einen [BackupSnapshot] nach/aus JSON (Muster: WidgetSerializer).
 * Korrupte Eingaben liefern `null`; unbekannte Keys/Typen/Sektionen werden beim
 * Parsen still übersprungen (forward-tolerant). Die Versionsprüfung übernimmt
 * der BackupManager beim Restore.
 */
object BackupSerializer {

    private const val KEY_VERSION = "backupVersion"
    private const val KEY_APP_VERSION = "appVersionCode"
    private const val KEY_EXPORTED_AT = "exportedAtMs"
    private const val KEY_STORES = "stores"
    private const val KEY_WIDGETS = "widgets"
    private const val KEY_ENTRY_KEY = "k"
    private const val KEY_ENTRY_TYPE = "t"
    private const val KEY_ENTRY_VALUE = "v"
    private const val KEY_WIDGET_PROVIDER = "provider"
    private const val KEY_WIDGET_WIDTH = "widthDp"
    private const val KEY_WIDGET_HEIGHT = "heightDp"
    private const val KEY_WIDGET_OFFSET_X = "offsetX"
    private const val KEY_WIDGET_OFFSET_Y = "offsetY"

    fun serialize(snapshot: BackupSnapshot): String {
        val root = JSONObject()
        root.put(KEY_VERSION, snapshot.backupVersion)
        root.put(KEY_APP_VERSION, snapshot.appVersionCode)
        root.put(KEY_EXPORTED_AT, snapshot.exportedAtMs)
        val stores = JSONObject()
        snapshot.stores.forEach { (name, entries) ->
            val array = JSONArray()
            entries.forEach { array.put(serializeEntry(it)) }
            stores.put(name, array)
        }
        root.put(KEY_STORES, stores)
        val widgets = JSONArray()
        snapshot.widgets.forEach { widgets.put(serializeWidget(it)) }
        root.put(KEY_WIDGETS, widgets)
        return root.toString()
    }

    fun parse(jsonString: String): BackupSnapshot? {
        return try {
            val root = JSONObject(jsonString)
            val storesJson = root.optJSONObject(KEY_STORES) ?: JSONObject()
            val stores = mutableMapOf<String, List<BackupEntry>>()
            storesJson.keys().forEach { name ->
                val array = storesJson.optJSONArray(name) ?: return@forEach
                stores[name] = parseEntries(array)
            }
            BackupSnapshot(
                backupVersion = root.getInt(KEY_VERSION),
                appVersionCode = root.optLong(KEY_APP_VERSION, 0L),
                exportedAtMs = root.optLong(KEY_EXPORTED_AT, 0L),
                stores = stores,
                widgets = parseWidgets(root.optJSONArray(KEY_WIDGETS) ?: JSONArray()),
            )
        } catch (_: JSONException) {
            null
        }
    }

    private fun serializeEntry(entry: BackupEntry): JSONObject {
        val obj = JSONObject()
        obj.put(KEY_ENTRY_KEY, entry.key)
        obj.put(KEY_ENTRY_TYPE, entry.type.tag)
        val value = when (entry.type) {
            BackupValueType.STRING_SET -> JSONArray((entry.value as Set<*>).map { it.toString() })
            BackupValueType.FLOAT -> (entry.value as Float).toDouble()
            else -> entry.value
        }
        obj.put(KEY_ENTRY_VALUE, value)
        return obj
    }

    private fun parseEntries(array: JSONArray): List<BackupEntry> =
        (0 until array.length()).mapNotNull { i -> parseEntry(array.optJSONObject(i)) }

    private fun parseEntry(obj: JSONObject?): BackupEntry? {
        if (obj == null) return null
        val key = obj.optString(KEY_ENTRY_KEY, "")
        val type = BackupValueType.fromTag(obj.optString(KEY_ENTRY_TYPE, ""))
        if (key.isEmpty() || type == null || !obj.has(KEY_ENTRY_VALUE)) return null
        val value = parseValue(obj, type) ?: return null
        return BackupEntry(key, type, value)
    }

    private fun parseValue(obj: JSONObject, type: BackupValueType): Any? = try {
        when (type) {
            BackupValueType.STRING -> obj.getString(KEY_ENTRY_VALUE)
            BackupValueType.BOOLEAN -> obj.getBoolean(KEY_ENTRY_VALUE)
            BackupValueType.INT -> obj.getInt(KEY_ENTRY_VALUE)
            BackupValueType.LONG -> obj.getLong(KEY_ENTRY_VALUE)
            BackupValueType.FLOAT -> obj.getDouble(KEY_ENTRY_VALUE).toFloat()
            BackupValueType.DOUBLE -> obj.getDouble(KEY_ENTRY_VALUE)
            BackupValueType.STRING_SET -> {
                val array = obj.getJSONArray(KEY_ENTRY_VALUE)
                buildSet { for (i in 0 until array.length()) add(array.getString(i)) }
            }
        }
    } catch (_: JSONException) {
        null
    }

    private fun serializeWidget(widget: WidgetBackup): JSONObject {
        val obj = JSONObject()
        obj.put(KEY_WIDGET_PROVIDER, widget.provider)
        obj.put(KEY_WIDGET_WIDTH, widget.widthDp)
        obj.put(KEY_WIDGET_HEIGHT, widget.heightDp)
        obj.put(KEY_WIDGET_OFFSET_X, widget.offsetX.toDouble())
        obj.put(KEY_WIDGET_OFFSET_Y, widget.offsetY.toDouble())
        return obj
    }

    private fun parseWidgets(array: JSONArray): List<WidgetBackup> =
        (0 until array.length()).mapNotNull { i -> parseWidget(array.optJSONObject(i)) }

    private fun parseWidget(obj: JSONObject?): WidgetBackup? {
        if (obj == null) return null
        val provider = obj.optString(KEY_WIDGET_PROVIDER, "")
        if (provider.isEmpty()) return null
        return WidgetBackup(
            provider = provider,
            widthDp = obj.optInt(KEY_WIDGET_WIDTH, 0),
            heightDp = obj.optInt(KEY_WIDGET_HEIGHT, 0),
            offsetX = obj.optDouble(KEY_WIDGET_OFFSET_X, 0.0).toFloat(),
            offsetY = obj.optDouble(KEY_WIDGET_OFFSET_Y, 0.0).toFloat(),
        )
    }
}
