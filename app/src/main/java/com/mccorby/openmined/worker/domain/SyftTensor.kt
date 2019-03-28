package com.mccorby.openmined.worker.domain

const val NO_ID: SyftTensorId = -1

typealias SyftTensorId = Long

data class SyftTensor(val id: SyftTensorId, val byteArray: ByteArray = ByteArray(0)) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SyftTensor

        if (id != other.id) return false
        if (!byteArray.contentEquals(other.byteArray)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + byteArray.contentHashCode()
        return result
    }
}

class TensorIdGenerator {
    fun generateId(): SyftTensorId {
        return System.currentTimeMillis()
    }
}