package com.mccorby.openmined.worker.datasource.mapper

import com.mccorby.openmined.worker.domain.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.msgpack.core.MessagePack
import org.msgpack.value.Value
import org.msgpack.value.impl.ImmutableArrayValueImpl
import org.msgpack.value.impl.ImmutableLongValueImpl
import org.msgpack.value.impl.ImmutableStringValueImpl

class MappersKtTest {

    @Test
    fun `Given a msgpack byte array containing a Set Object operation and a tensor the mapper returns the corresponding SyftMessage`() {
        // Given
        val tensorAsBytes = ByteArray(0)
        print(tensorAsBytes.contentToString())
        val expected = SyftMessage.SetObject(
            SyftOperand.SyftTensor(
                6830,
                tensorAsBytes.contentToString().toByteArray()
            )
        )

        val tensorArray = ImmutableArrayValueImpl(
            arrayOf<Value>(
                ImmutableLongValueImpl(0),
                ImmutableArrayValueImpl(
                    arrayOf<Value>(
                        ImmutableLongValueImpl(6830),
                        ImmutableStringValueImpl(tensorAsBytes.contentToString())
                    )
                )
            )
        )

        val operationArray = ImmutableArrayValueImpl(
            arrayOf<Value>(
                ImmutableLongValueImpl(2),
                tensorArray
            )
        )

        val outerWrapper = ImmutableArrayValueImpl(
            arrayOf<Value>(
                ImmutableLongValueImpl(2),
                operationArray
            )
        )

        val packer = MessagePack.newDefaultBufferPacker()
        packer.packValue(ImmutableArrayValueImpl(arrayOf(outerWrapper)))


        // When
        val syftMessage = packer.toByteArray().mapToSyftMessage()

        // Then
        assertTrue(syftMessage is SyftMessage.SetObject)
        assertEquals(expected, syftMessage)
    }

    @Test
    fun `Given a msgpack byte array containing an Add operation and two pointers the mapper returns the corresponding SyftMessage`() {
        val tensorPointer1 = SyftOperand.SyftTensorPointer(6830)
        val tensorPointer2 = SyftOperand.SyftTensorPointer(1234)
        val resultId = 7766L
        val resultIds = listOf(resultId)
        val expected = SyftMessage.ExecuteCommand(SyftCommand.Add(listOf(tensorPointer1, tensorPointer2), resultIds))

        val pointer1Array = ImmutableArrayValueImpl(
            arrayOf<Value>(
                ImmutableLongValueImpl(11), // Type Pointer
                ImmutableArrayValueImpl(
                    arrayOf<Value>(
                        ImmutableLongValueImpl(9999),
                        ImmutableLongValueImpl(6830)
                    )
                )
            )
        )

        val pointer2Array = ImmutableArrayValueImpl(
            arrayOf<Value>(
                ImmutableLongValueImpl(11), // Type Pointer
                ImmutableArrayValueImpl(
                    arrayOf<Value>(
                        ImmutableLongValueImpl(8888),
                        ImmutableLongValueImpl(1234)
                    )
                )
            )
        )

        val otherPointersWrapper = ImmutableArrayValueImpl(
            arrayOf<Value>(
                ImmutableLongValueImpl(2),
                ImmutableArrayValueImpl(
                    arrayOf<Value>(
                        pointer2Array
                    )
                )
            )
        )

        val operationAndOperandsArray = ImmutableArrayValueImpl(
            arrayOf<Value>(
                ImmutableLongValueImpl(2),
                ImmutableArrayValueImpl(
                    arrayOf<Value>(
                        ImmutableStringValueImpl("__add__"),
                        pointer1Array,
                        otherPointersWrapper
                    )
                )
            )
        )

        val operationArray = ImmutableArrayValueImpl(
            arrayOf<Value>(
                ImmutableLongValueImpl(1), // CMD
                operationAndOperandsArray
            )
        )

        val resultIdsWrapper = ImmutableArrayValueImpl(
            arrayOf<Value>(
                ImmutableLongValueImpl(3),
                ImmutableArrayValueImpl(arrayOf<Value>(ImmutableLongValueImpl(resultId)))
            )
        )

        val outerWrapper = ImmutableArrayValueImpl(
            arrayOf<Value>(
                ImmutableLongValueImpl(2),
                operationArray
            )
        )

        val packer = MessagePack.newDefaultBufferPacker()
        packer.packValue(ImmutableArrayValueImpl(arrayOf(outerWrapper)))


        // When
        val syftMessage = packer.toByteArray().mapToSyftMessage()

        // Then
        assertTrue(syftMessage is SyftMessage.ExecuteCommand)
        assertEquals(expected, syftMessage)
    }
}