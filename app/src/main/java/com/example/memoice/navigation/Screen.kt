package com.example.memoice.navigation

import android.net.Uri

const val KEY = "reference"

sealed class Screen(val route: String) {
    object Home: Screen(route = "home_screen")
    object Detail: Screen(route = "detail_screen/{$KEY}") {
        fun passRef(reference: String): String {
            val encodedRef = Uri.encode(reference)
            return this.route.replace(oldValue = "{$KEY}", newValue = encodedRef)
        }
    }
    object Rec: Screen(route = "rec_screen")
}
