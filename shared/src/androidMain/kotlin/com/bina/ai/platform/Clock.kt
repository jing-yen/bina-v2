package com.bina.ai.platform

actual object Clock {
    actual fun now(): Long = System.currentTimeMillis()
}
