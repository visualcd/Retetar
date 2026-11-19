package com.ro.aminvatatro

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform