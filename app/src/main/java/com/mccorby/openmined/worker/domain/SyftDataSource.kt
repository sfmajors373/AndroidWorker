package com.mccorby.openmined.worker.domain

import io.reactivex.Flowable

interface SyftDataSource {
    fun connect()
    fun disconnect()
    fun sendMessage(syftMessage: SyftMessage)
    fun onNewMessage(): Flowable<SyftMessage>
}