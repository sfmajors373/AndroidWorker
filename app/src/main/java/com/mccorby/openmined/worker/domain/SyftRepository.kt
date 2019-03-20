package com.mccorby.openmined.worker.domain

import io.reactivex.Flowable

// The repository allow us to use different types of data sources without requiring modifying the upper layers
class SyftRepository(private val syftDataSource: SyftDataSource, private val tensorIdGenerator: TensorIdGenerator) {

    private val tensorMap = mutableMapOf<Long, SyftTensor>()

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

    fun setObject(objectToSet: SyftTensor) {
        // Create id for this tensor
        val id = tensorIdGenerator.generateId()
        tensorMap[id] = objectToSet.copy(id = id)
        sendMessage(SyftMessage.ClientResponse(id))
    }
}