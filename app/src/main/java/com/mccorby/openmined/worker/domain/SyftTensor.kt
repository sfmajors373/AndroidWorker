package com.mccorby.openmined.worker.domain

const val NO_ID: SyftTensorId = -1

typealias SyftTensorId = Long

data class SyftTensor(val id: SyftTensorId, val byteArray: ByteArray = ByteArray(0))

class TensorIdGenerator {
    fun generateId(): SyftTensorId {
        return System.currentTimeMillis()
    }
}