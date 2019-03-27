package com.mccorby.openmined.worker.datasource.mapper

import com.mccorby.openmined.worker.domain.SyftMessage
import org.junit.Assert.assertTrue
import org.junit.Test
import org.msgpack.core.MessagePack

class MappersKtTest {

    @Test
    fun `Given a byte stream containing a Set Object operation and a tensor the mapper returns the corresponding SyftMessage`() {
        // Given
        val tensorAsBytes = byteArrayOf(123.toByte(), 321.toByte())
        val setObject = "[2, [2, [0, [68305306082, ${tensorAsBytes.contentToString()}]]]]"

        // When
        val syftMessage = setObject.toByteArray().mapToSyftMessage()

        // Then
        assertTrue(syftMessage is SyftMessage.SetObject)
    }
}