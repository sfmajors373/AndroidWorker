package com.mccorby.openmined.worker.datasource.mapper

import com.mccorby.openmined.worker.domain.SyftMessage
import com.mccorby.openmined.worker.domain.SyftTensor
import org.msgpack.core.MessageBufferPacker
import org.msgpack.core.MessagePack
import org.msgpack.core.MessagePacker

fun SyftMessage.toByteArray(): ByteArray {
    val packer = MessagePack.newDefaultBufferPacker()
    // TODO packer.packBlahBlahBlha
    return packer.toByteArray()
}

// TODO Probably Json or something similar. The name should reflect the format
fun SyftMessage.mapToString(): String {
    val packer = MessagePack.newDefaultBufferPacker()
    // TODO packer.packBlahBlahBlha
    return when (this) {
        is SyftMessage.ExecuteCommand -> mapExecuteCommand(packer, this)
        else -> { packer }
    }.toString()
}

private fun mapExecuteCommand(packer: MessageBufferPacker, syftMessage: SyftMessage.ExecuteCommand): MessagePacker {
    return packer.packString(syftMessage.name)
}

fun ByteArray.mapToSyftTensor(): SyftTensor {
    // https://github.com/msgpack/msgpack-java/blob/develop/msgpack-core/src/test/java/org/msgpack/core/example/MessagePackExample.java
    // Assume args brings a byte of array for this PoC
    val unpacker = MessagePack.newDefaultUnpacker(this)
    // Start unpacking the byte stream as packed by PySyft

    // The unpacking should give us the data to build a SyftTensor
    // This SyftTensor can then be transformed into a DL4J INDArray or any other framework-dependent representation

    return SyftTensor()
}

fun ByteArray.mapToSyftMessage(): SyftMessage{
    // https://github.com/msgpack/msgpack-java/blob/develop/msgpack-core/src/test/java/org/msgpack/core/example/MessagePackExample.java
    // Assume args brings a byte of array for this PoC
    val unpacker = MessagePack.newDefaultUnpacker(this)
//    return SyftMessage.ExecuteCommand("something")
    return SyftMessage.SetObject(SyftTensor(this))
}
