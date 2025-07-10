package io.github.hyochan.kmpiap.example

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "KMP IAP Example",
    ) {
        App()
    }
}