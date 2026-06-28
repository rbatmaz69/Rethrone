package com.example.androidlauncher.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import com.composables.icons.lucide.Cloud
import com.composables.icons.lucide.CloudDrizzle
import com.composables.icons.lucide.CloudFog
import com.composables.icons.lucide.CloudLightning
import com.composables.icons.lucide.CloudRain
import com.composables.icons.lucide.CloudSnow
import com.composables.icons.lucide.CloudSun
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Sun
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.math.roundToInt

/**
 * Aktuelle Wetterdaten für das schmale Startbildschirm-Widget.
 */
data class WeatherData(
    val temperatureC: Int,
    val weatherCode: Int
)

/**
 * Wetter-Grundkategorie, abgeleitet aus dem WMO-Code (Open-Meteo). Entkoppelt die rein
 * datengetriebene Code-Klassifikation (unit-testbar) von der konkreten Icon-Wahl.
 */
enum class WeatherCondition { CLEAR, PARTLY_CLOUDY, CLOUDY, FOG, DRIZZLE, RAIN, SNOW, THUNDERSTORM }

/**
 * Lädt das aktuelle Wetter über Open-Meteo (kostenlos, ohne API-Key) und ermittelt
 * den groben Gerätestandort über den [LocationManager] (ohne Google Play Services).
 *
 * Bewusst leichtgewichtig: nutzt nur [HttpURLConnection] + [JSONObject], damit keine
 * zusätzlichen Netzwerk-Abhängigkeiten nötig sind.
 */
class WeatherRepository(private val context: Context) {

    /**
     * Liefert die zuletzt bekannte grobe Position oder null, wenn keine Berechtigung
     * vorliegt bzw. (noch) keine Position verfügbar ist.
     */
    fun lastKnownLocation(): Pair<Double, Double>? {
        if (!hasLocationPermission()) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        val providers = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
            LocationManager.GPS_PROVIDER
        )

        var best: Location? = null
        for (provider in providers) {
            val location = try {
                if (manager.isProviderEnabled(provider)) manager.getLastKnownLocation(provider) else null
            } catch (e: SecurityException) {
                null
            }
            if (location != null && (best == null || location.time > best!!.time)) {
                best = location
            }
        }
        return best?.let { it.latitude to it.longitude }
    }

    /**
     * Ermittelt eine Position: zuerst die zuletzt bekannte; ist keine vorhanden, wird
     * aktiv ein einzelner frischer Fix angefordert (mit Timeout). Gibt null zurück, wenn
     * keine Berechtigung vorliegt, kein Provider aktiv ist oder der Fix ausbleibt.
     */
    suspend fun awaitLocation(): Pair<Double, Double>? {
        if (!hasLocationPermission()) return null
        lastKnownLocation()?.let { return it }

        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val provider = when {
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> return null
        }

        return withTimeoutOrNull(20_000L) {
            suspendCancellableCoroutine<Pair<Double, Double>?> { cont ->
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        manager.removeUpdates(this)
                        if (cont.isActive) cont.resume(location.latitude to location.longitude)
                    }

                    @Deprecated("Erforderlich für ältere LocationListener-API")
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
                try {
                    manager.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper())
                } catch (e: SecurityException) {
                    if (cont.isActive) cont.resume(null)
                }
                cont.invokeOnCancellation { manager.removeUpdates(listener) }
            }
        }
    }

    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Grobe Standortbestimmung über die IP-Adresse (Stadt-Ebene) – ohne Standortdienst
     * und ohne Berechtigung, lediglich über das Internet. Dient als Fallback, wenn kein
     * GPS-/Netzwerk-Fix verfügbar ist. Nutzt den kostenlosen Dienst ipwho.is (kein Key).
     */
    suspend fun ipLocation(): Pair<Double, Double>? = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = (
                URL("https://ipwho.is/?fields=latitude,longitude,success")
                    .openConnection() as HttpURLConnection
                ).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            if (!json.optBoolean("success", true)) return@withContext null
            val lat = json.optDouble("latitude", Double.NaN)
            val lon = json.optDouble("longitude", Double.NaN)
            if (lat.isNaN() || lon.isNaN()) null else lat to lon
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Ruft das aktuelle Wetter ab. Gibt null zurück, wenn der Abruf fehlschlägt.
     */
    suspend fun fetch(lat: Double, lon: Double): WeatherData? = withContext(Dispatchers.IO) {
        val urlString = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=$lat&longitude=$lon&current=temperature_2m,weather_code"
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 8000
                readTimeout = 8000
            }
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            // Parsing liegt framework-frei in parseCurrentWeather (unit-getestet).
            parseCurrentWeather(body)?.also { updateCache(it) }
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    companion object {
        // Prozessweiter Cache: überlebt das Verlassen/Neubetreten der Composition
        // (z. B. App-Drawer → Startseite), damit das Wetter nicht kurz verschwindet.
        @Volatile
        private var cachedData: WeatherData? = null

        @Volatile
        private var cachedAtElapsedMs: Long = 0L

        val cached: WeatherData? get() = cachedData

        /** Alter des Caches in Millisekunden (Long.MAX_VALUE, wenn leer). */
        val cacheAgeMs: Long
            get() = if (cachedData == null) Long.MAX_VALUE else SystemClock.elapsedRealtime() - cachedAtElapsedMs

        private fun updateCache(data: WeatherData) {
            cachedData = data
            cachedAtElapsedMs = SystemClock.elapsedRealtime()
        }

        /**
         * Klassifiziert einen WMO-Wettercode (Open-Meteo) in eine [WeatherCondition].
         * Reine Logik – ohne Framework unit-testbar.
         */
        fun weatherConditionFor(code: Int): WeatherCondition = when (code) {
            0 -> WeatherCondition.CLEAR
            1, 2 -> WeatherCondition.PARTLY_CLOUDY
            3 -> WeatherCondition.CLOUDY
            45, 48 -> WeatherCondition.FOG
            in 51..57 -> WeatherCondition.DRIZZLE
            in 61..67, in 80..82 -> WeatherCondition.RAIN
            in 71..77, 85, 86 -> WeatherCondition.SNOW
            in 95..99 -> WeatherCondition.THUNDERSTORM
            else -> WeatherCondition.CLOUDY
        }

        /**
         * Bildet einen WMO-Wettercode (Open-Meteo) auf ein passendes Lucide-Icon ab.
         */
        fun iconFor(code: Int): ImageVector = when (weatherConditionFor(code)) {
            WeatherCondition.CLEAR -> Lucide.Sun
            WeatherCondition.PARTLY_CLOUDY -> Lucide.CloudSun
            WeatherCondition.CLOUDY -> Lucide.Cloud
            WeatherCondition.FOG -> Lucide.CloudFog
            WeatherCondition.DRIZZLE -> Lucide.CloudDrizzle
            WeatherCondition.RAIN -> Lucide.CloudRain
            WeatherCondition.SNOW -> Lucide.CloudSnow
            WeatherCondition.THUNDERSTORM -> Lucide.CloudLightning
        }

        /**
         * Parst die Open-Meteo-Antwort (ein `current`-Objekt mit `temperature_2m` und
         * `weather_code`) zu [WeatherData]. Liefert null bei fehlendem `current`-Objekt oder
         * ungueltigem JSON. Reine Logik – ohne Netzwerk unit-testbar.
         */
        fun parseCurrentWeather(body: String): WeatherData? {
            return try {
                val current = JSONObject(body).optJSONObject("current") ?: return null
                WeatherData(
                    temperatureC = current.getDouble("temperature_2m").roundToInt(),
                    weatherCode = current.getInt("weather_code"),
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
