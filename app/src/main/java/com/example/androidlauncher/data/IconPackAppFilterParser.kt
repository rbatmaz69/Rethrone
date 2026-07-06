package com.example.androidlauncher.data

import org.xmlpull.v1.XmlPullParser

/**
 * Pures Parsen der `appfilter.xml` eines Icon-Packs (ADW-/Nova-Konvention, B4).
 *
 * Nimmt bewusst eine fertige [XmlPullParser]-Instanz entgegen und erzeugt nie selbst
 * eine: Produktion fuettert `resources.getXml(...)` bzw. einen Factory-Parser ueber den
 * Asset-Stream, Unit-Tests einen kxml2-Parser aus Strings (android.util.Xml ist im
 * JVM-Test gestubbt).
 */
object IconPackAppFilterParser {

    private const val TAG_ITEM = "item"
    private const val ATTR_COMPONENT = "component"
    private const val ATTR_DRAWABLE = "drawable"
    private const val COMPONENT_INFO_PREFIX = "ComponentInfo{"
    private const val COMPONENT_INFO_SUFFIX = "}"

    /**
     * Liest alle `<item component="ComponentInfo{pkg/cls}" drawable="name"/>`-Eintraege:
     * geflattete Component (`pkg/cls`) → Drawable-Name. Bei Duplikaten gewinnt der erste
     * Eintrag; fehlerhafte Items und fremde Tags (`<calendar>`, `<scale>`, …) werden
     * uebersprungen. Bricht das Dokument mitten im Stream (korrupte Packs), bleiben die
     * bis dahin gesammelten Eintraege erhalten.
     */
    fun parse(parser: XmlPullParser): Map<String, String> {
        val result = mutableMapOf<String, String>()
        runCatching {
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name == TAG_ITEM) {
                    val component = parser.getAttributeValue(null, ATTR_COMPONENT)
                        ?.let(::parseComponentInfo)
                    val drawable = parser.getAttributeValue(null, ATTR_DRAWABLE)
                    if (component != null && !drawable.isNullOrBlank() && component !in result) {
                        result[component] = drawable
                    }
                }
                event = parser.next()
            }
        }
        return result
    }

    /**
     * `"ComponentInfo{com.foo/com.foo.Main}"` → `"com.foo/com.foo.Main"`. Toleriert
     * Eintraege ohne Wrapper und expandiert relative Klassennamen
     * (`com.foo/.Main` → `com.foo/com.foo.Main`). `null` bei nicht parsebaren Werten.
     */
    fun parseComponentInfo(raw: String): String? {
        val trimmed = raw.trim()
        val inner = if (trimmed.startsWith(COMPONENT_INFO_PREFIX) && trimmed.endsWith(COMPONENT_INFO_SUFFIX)) {
            trimmed.substring(COMPONENT_INFO_PREFIX.length, trimmed.length - COMPONENT_INFO_SUFFIX.length)
        } else {
            trimmed
        }
        val slash = inner.indexOf('/')
        if (slash <= 0 || slash == inner.length - 1) return null
        val packageName = inner.substring(0, slash).trim()
        var className = inner.substring(slash + 1).trim()
        if (packageName.isEmpty() || className.isEmpty()) return null
        if (className.startsWith(".")) {
            className = packageName + className
        }
        return "$packageName/$className"
    }

    /**
     * Sucht den Drawable-Namen fuer eine App: erst exakter Component-Match, sonst der
     * erste Eintrag desselben Packages (Packs mappen oft mehrere Activities pro App,
     * und die Launch-Activity muss nicht die gemappte sein).
     */
    fun resolveDrawableName(
        appFilter: Map<String, String>,
        packageName: String,
        componentFlat: String?,
    ): String? {
        componentFlat?.let { appFilter[it] }?.let { return it }
        val packagePrefix = "$packageName/"
        return appFilter.entries.firstOrNull { it.key.startsWith(packagePrefix) }?.value
    }
}
