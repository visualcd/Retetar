package com.ro.aminvatatro

import androidx.compose.ui.window.ComposeUIViewController
import com.ro.aminvatatro.di.initKoin

fun MainViewController() = ComposeUIViewController {
    initKoin()
    App()
}