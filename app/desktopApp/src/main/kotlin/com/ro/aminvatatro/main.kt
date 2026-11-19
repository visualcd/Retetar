package com.ro.aminvatatro

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.ro.aminvatatro.di.initKoin

fun main() {
    initKoin()
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Rețetar",
        ) {
            App()
        }
    }
}