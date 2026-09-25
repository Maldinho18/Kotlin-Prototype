package com.sidequests.app.context

interface LocationProvider {
    suspend fun getCurrentLocation(): LocationData?
}
