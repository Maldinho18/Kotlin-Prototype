package com.sidequests.app.context

import java.time.ZonedDateTime

interface TimeProvider {
    fun now(): ZonedDateTime
}

class SystemTimeProvider : TimeProvider {
    override fun now(): ZonedDateTime = ZonedDateTime.now()
}
