package com.example.memoice.navigation

const val KEY = "reference"

sealed class Screen(val route: String) {
    object Home: Screen(route = "home_screen")
    object Detail: Screen(route = "detail_screen/{$KEY}") {
        fun passRef(reference: String): String {
            return this.route.replace(oldValue = "{$KEY}", newValue = reference)
        }
    }
    object Rec: Screen(route = "rec_screen?reference={ARGUMENT_KEY}") {
        fun passRef(reference: String): String {
            return this.route.replace(oldValue = "{$KEY}", newValue = reference)
        }
    }
}
