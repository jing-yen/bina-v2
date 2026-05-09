package com.bina.ai.platform

interface LocationProvider {
    suspend fun getCurrentLocation(): Pair<Double, Double>?
}
