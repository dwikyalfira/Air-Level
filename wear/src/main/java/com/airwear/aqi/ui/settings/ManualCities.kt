package com.airwear.aqi.ui.settings

/** Preset cities for manual location (must match IQAir city/state/country). */
object ManualCities {
    data class Spec(val city: String, val state: String, val country: String) {
        val label: String get() = city
    }

    val list = listOf(
        Spec("Jakarta", "Jakarta", "Indonesia"),
        Spec("Bandung", "West Java", "Indonesia"),
        Spec("Surabaya", "East Java", "Indonesia"),
        Spec("Bali", "Bali", "Indonesia"),
        Spec("Delhi", "Delhi", "India"),
        Spec("Beijing", "Beijing", "China"),
        Spec("Shanghai", "Shanghai", "China"),
        Spec("Tokyo", "Tokyo", "Japan"),
        Spec("Seoul", "Seoul", "South Korea"),
        Spec("Singapore", "Singapore", "Singapore"),
        Spec("Bangkok", "Bangkok", "Thailand"),
        Spec("London", "England", "United Kingdom"),
        Spec("Paris", "Île-de-France", "France"),
        Spec("New York", "New York", "USA"),
        Spec("Los Angeles", "California", "USA"),
    )
}
