package com.mccorby.openmined.worker.datasource.mapper

import android.util.Log
import com.mccorby.openmined.worker.domain.SyftMessage
import com.mccorby.openmined.worker.domain.SyftTensor
import org.msgpack.core.MessageBufferPacker
import org.msgpack.core.MessagePack
import org.msgpack.core.MessagePacker

private const val TAG = "MapperDS"
private const val COMPRESSION_ENABLED = 49


// TODO Probably Json or something similar. The name should reflect the format
fun SyftMessage.mapToString(): String {
    val packer = MessagePack.newDefaultBufferPacker()
    // TODO packer.packBlahBlahBlha
    return when (this) {
        is SyftMessage.ExecuteCommand -> mapExecuteCommand(packer, this)
        else -> {
            packer
        }
    }.toString()
}

private fun mapExecuteCommand(packer: MessageBufferPacker, syftMessage: SyftMessage.ExecuteCommand): MessagePacker {
    return packer.packString(syftMessage.name)
}

fun ByteArray.mapToSyftMessage(): SyftMessage {
    // (tensor.id, tensor_bin, chain, grad_chain, tags, tensor.description)
    Log.d(TAG, this.toString())
    // Remove first byte indicating if stream has been compressed or not
    val isCompress = this[0]
    val byteArray = if (isCompress.toInt() == COMPRESSION_ENABLED) {
        decompress(this.drop(1).toByteArray())
    } else {
        this.drop(1).toByteArray()
    }


    val unpacker = MessagePack.newDefaultUnpacker(byteArray)
    val map = unpacker.unpackValue()

    val dto = TupleDto()
    map.asArrayValue().forEachIndexed { index, value ->
        when (index) {
            0 -> dto.op = value.asIntegerValue().asInt()
            1 -> {
                val listValues = value.asArrayValue().toList()
                val tensorDto = TensorDto()
                tensorDto.id = listValues[0].asIntegerValue().toInt()
                tensorDto.data = listValues[1].asStringValue().asByteArray()
                dto.value = tensorDto
            }
            2 -> {
                // TODO chain
            }
            3 -> {
                // TODO grad_chain
            }
            4 -> {
                // TODO tags
            }
            5 -> {
                // TODO tensor description
            }
            else -> {
                Log.e(TAG, "What are you doing here?") // Raise an error!
            }
        }
    }
    return mapOperation(dto)
}

fun decompress(stream: ByteArray): ByteArray {
    TODO("LZ4 Compression Not yet Implemented")
//    val factory = LZ4Factory.fastestInstance()
//    // Size is not known. It could be sent in the tuple
//    val decompressor = factory.safeDecompressor()
//    var dest = ByteArray(8096)
//    val decompressedLength = decompressor.decompress(stream, dest)
//    return dest
}

private fun mapOperation(tupleDto: TupleDto): SyftMessage {
    return when (tupleDto.op) {
        0 -> SyftMessage.SetObject(SyftTensor(tupleDto.value.id.toLong(), tupleDto.value.data))
        else -> {
            throw IllegalArgumentException("Operation ${tupleDto.op} not yet supported")
        }
    }
}

class TupleDto {
    var op: Int = 0
    var value = TensorDto()

    override fun toString(): String {
        return "$op - $value"
    }
}

class TensorDto {
    var id: Int = 0
    var data: ByteArray = byteArrayOf()

    override fun toString(): String {
        return "{$id - [${String(data)}]}"
    }
}
