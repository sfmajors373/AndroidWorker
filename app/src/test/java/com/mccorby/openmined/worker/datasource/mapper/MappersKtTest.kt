package com.mccorby.openmined.worker.datasource.mapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.collect.ImmutableCollection
import com.mccorby.openmined.worker.domain.SyftCommand
import com.mccorby.openmined.worker.domain.SyftMessage
import com.mccorby.openmined.worker.domain.SyftTensor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.msgpack.core.MessagePack
import org.msgpack.value.IntegerValue
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
        val expected = SyftMessage.SetObject(SyftTensor(6830, tensorAsBytes.contentToString().toByteArray()))

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

        val operationArray = ImmutableArrayValueImpl(arrayOf<Value>(
            ImmutableLongValueImpl(2),
            tensorArray
        ))

        val outerWrapper = ImmutableArrayValueImpl(arrayOf<Value>(
            ImmutableLongValueImpl(2),
            operationArray
        ))

        val packer = MessagePack.newDefaultBufferPacker()
        packer.packValue(ImmutableArrayValueImpl(arrayOf(outerWrapper)))


        // When
        val syftMessage = packer.toByteArray().mapToSyftMessage()

        // Then
        assertTrue(syftMessage is SyftMessage.SetObject)
        assertEquals(expected, syftMessage)
    }

    @Test
    fun `Given a msgpack byte array containing an Add operation and two tensors the mapper returns the corresponding SyftMessage`() {
        // Given
        val tensorAsBytes = ByteArray(0)
        print(tensorAsBytes.contentToString())
        val tensor1 = SyftTensor(6830, tensorAsBytes.contentToString().toByteArray())
        val tensor2 = SyftTensor(1234, tensorAsBytes.contentToString().toByteArray())
        val expected = SyftMessage.ExecuteCommand(SyftCommand.AddTensors(listOf(tensor1, tensor2)))

        val tensor1Array = ImmutableArrayValueImpl(
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

        val tensor2Array = ImmutableArrayValueImpl(
            arrayOf<Value>(
                ImmutableLongValueImpl(0),
                ImmutableArrayValueImpl(
                    arrayOf<Value>(
                        ImmutableLongValueImpl(1234),
                        ImmutableStringValueImpl(tensorAsBytes.contentToString())
                    )
                )
            )
        )

        val operandsArray = ImmutableArrayValueImpl(
            arrayOf<Value>(
                ImmutableLongValueImpl(2),
                ImmutableArrayValueImpl(
                    arrayOf<Value>(
                        tensor1Array, tensor2Array
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
                        operandsArray
                    )
                )
            )
        )

        val operationArray = ImmutableArrayValueImpl(arrayOf<Value>(
            ImmutableLongValueImpl(1),
            operationAndOperandsArray
        ))

        val outerWrapper = ImmutableArrayValueImpl(arrayOf<Value>(
            ImmutableLongValueImpl(2),
            operationArray
        ))

        val packer = MessagePack.newDefaultBufferPacker()
        packer.packValue(ImmutableArrayValueImpl(arrayOf(outerWrapper)))


        // When
        val syftMessage = packer.toByteArray().mapToSyftMessage()

        // Then
        assertTrue(syftMessage is SyftMessage.ExecuteCommand)
        assertEquals(expected, syftMessage)
    }
}