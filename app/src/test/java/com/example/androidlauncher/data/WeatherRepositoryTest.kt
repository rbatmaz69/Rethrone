package com.example.androidlauncher.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WeatherRepositoryTest {

    // ── weatherConditionFor ──────────────────────────────────────────

    @Test
    fun `weatherConditionFor maps clear and cloud codes`() {
        assertEquals(WeatherCondition.CLEAR, WeatherRepository.weatherConditionFor(0))
        assertEquals(WeatherCondition.PARTLY_CLOUDY, WeatherRepository.weatherConditionFor(1))
        assertEquals(WeatherCondition.PARTLY_CLOUDY, WeatherRepository.weatherConditionFor(2))
        assertEquals(WeatherCondition.CLOUDY, WeatherRepository.weatherConditionFor(3))
    }

    @Test
    fun `weatherConditionFor maps precipitation ranges`() {
        assertEquals(WeatherCondition.FOG, WeatherRepository.weatherConditionFor(45))
        assertEquals(WeatherCondition.DRIZZLE, WeatherRepository.weatherConditionFor(55))
        assertEquals(WeatherCondition.RAIN, WeatherRepository.weatherConditionFor(63))
        assertEquals(WeatherCondition.RAIN, WeatherRepository.weatherConditionFor(81))
        assertEquals(WeatherCondition.SNOW, WeatherRepository.weatherConditionFor(73))
        assertEquals(WeatherCondition.SNOW, WeatherRepository.weatherConditionFor(86))
        assertEquals(WeatherCondition.THUNDERSTORM, WeatherRepository.weatherConditionFor(95))
    }

    @Test
    fun `weatherConditionFor falls back to cloudy for unknown codes`() {
        assertEquals(WeatherCondition.CLOUDY, WeatherRepository.weatherConditionFor(12345))
        assertEquals(WeatherCondition.CLOUDY, WeatherRepository.weatherConditionFor(-1))
    }

    // ── parseCurrentWeather ──────────────────────────────────────────

    @Test
    fun `parseCurrentWeather reads temperature and code and rounds the temperature`() {
        val json = """{"current":{"temperature_2m":17.6,"weather_code":61}}"""
        val data = WeatherRepository.parseCurrentWeather(json)
        assertEquals(WeatherData(temperatureC = 18, weatherCode = 61), data)
    }

    @Test
    fun `parseCurrentWeather returns null when current object is missing`() {
        assertNull(WeatherRepository.parseCurrentWeather("""{"hourly":{}}"""))
    }

    @Test
    fun `parseCurrentWeather returns null for invalid json`() {
        assertNull(WeatherRepository.parseCurrentWeather("not json at all"))
    }
}
