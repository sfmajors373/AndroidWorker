package com.mccorby.openmined.worker.domain

const val NO_ID: Long = -1

data class SyftTensor(val id: Long, val byteArray: ByteArray = ByteArray(0))

class TensorIdGenerator {
    fun generateId(): Long {
        return System.currentTimeMillis()
    }
}