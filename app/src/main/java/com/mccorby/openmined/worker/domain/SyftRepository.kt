package com.mccorby.openmined.worker.domain

import io.reactivex.Flowable

// The repository allow us to use different types of data sources without requiring modifying the upper layers
class SyftRepository(private val syftDataSource: SyftDataSource) {
    fun connect() {
        syftDataSource.connect()
    }

    fun sendMessage(syftMessage: SyftMessage) {
        syftDataSource.sendMessage(syftMessage)
    }

    fun onNewMessage(): Flowable<SyftMessage> = syftDataSource.onNewMessage()

    fun disconnect() {
        syftDataSource.disconnect()
    }
}