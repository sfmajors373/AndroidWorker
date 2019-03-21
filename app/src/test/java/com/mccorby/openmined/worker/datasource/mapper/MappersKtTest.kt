package com.mccorby.openmined.worker.datasource.mapper

import com.mccorby.openmined.worker.domain.SyftMessage
import org.junit.Assert.assertTrue
import org.junit.Test

class MappersKtTest {

    @Test
    fun `Given a byte stream containing a Set Object operation and a tensor the mapper returns the corresponding SyftMessage`() {
        // Given
        val byteArray = "00x920x000x960xcf0x000x000x000x0130x94P\r0xda0x000x880x93NUMPY0x010x00v0x00{'descr': '<f4', 'fortran_order': False, 'shape': (1, 2), }                                                          \n0x000x000x000x000x000x000x80?0xc00xc00xc00xc0".toByteArray()

        // When
        val syftMessage = byteArray.mapToSyftMessage()

        // Then
        assertTrue(syftMessage is SyftMessage.SetObject)
    }
}