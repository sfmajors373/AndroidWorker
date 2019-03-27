package com.mccorby.openmined.worker.datasource.mapper

import android.util.Log
import com.mccorby.openmined.worker.domain.SyftMessage
import com.mccorby.openmined.worker.domain.SyftTensor
import org.msgpack.core.MessageBufferPacker
import org.msgpack.core.MessagePack
import org.msgpack.core.MessagePacker
import org.msgpack.value.ArrayValue
import org.msgpack.value.Value

private const val TAG = "MapperDS"

// These are values defined in PySyft
private const val COMPRESSION_ENABLED = 49
// Operations in PySyft
private const val UNDEFINED = 0
private const val CMD = 1
private const val OBJ = 2
private const val OBJ_REQ = 3
private const val OBJ_DEL = 4
private const val EXCEPTION = 5
private const val IS_NONE = 6
private const val GET_SHAPE = 7
private const val SEARCH = 8

// Types are encoded in the stream sent from PySyft
private const val TYPE_TENSOR = 0
private const val TYPE_TUPLE = 1
private const val TYPE_LIST = 2
private const val TYPE_TENSOR_POINTER = 11

// Commands
private const val CMD_ADD = "__add__"

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
    return packer.packString(syftMessage.command.toString())
}

fun ByteArray.mapToSyftMessage(): SyftMessage {
    // (tensor.id, tensor_bin, chain, grad_chain, tags, tensor.description)
    // Remove first byte indicating if stream has been compressed or not
    val isCompress = this[0]
    val byteArray = if (isCompress.toInt() == COMPRESSION_ENABLED) {
        decompress(this.drop(1).toByteArray())
    } else {
        this.drop(1).toByteArray()
    }

    val unpacker = MessagePack.newDefaultUnpacker(byteArray)
    val streamToDecode = unpacker.unpackValue()
    // Assuming any message comes encapsulated in an outer tuple
    // (2, (2, (0, (68305306082, b"\The Binary Thing representing the tensor))))
    // SEND TENSOR -> (tuple_type, (operation, (tensor_type, (id, data))))
    // ADD Tensor Pointers [2, [10, [2, [[2, [b'__add__', [11, [69112721853, 38651364915, b'bob', None, [5]]], [2, [[11, [22595865006, 76820697749, b'bob', None, [5]]]]], [5, {}]]], [3, [35031792243]]]]]]
    // Add tensor is [tuple_type, [CMD, [tuple_type, [[tuple_type, [op, [pointer_type, [me, bob]]]?????

    // This is the trick done in _detail
    // if if type(obj) == list:
    //        return detailers[obj[0]](worker, obj[1])
    // else:
    //        return obj
    // SO the problem is that a list and a tuple are the same in the stream we receive here
    val outerType = streamToDecode.asArrayValue()[0].asIntegerValue().asInt()
    Log.d(TAG, "Outer type (should be 2) -> $outerType")
    val operationDto = unpackOperation(streamToDecode.asArrayValue()[1].asArrayValue())

    return mapOperation(operationDto)
}

private fun unpackOperation(operationArray: ArrayValue): OperationDto {
    val operation = operationArray[0].asIntegerValue().asInt()
    val operands = operationArray.drop(1)
    return when (operation) {
        OBJ -> unpackObjectSet(operands)
//        CMD -> unpackCommand(operands)
        else -> {
            TODO("Operation $operation not yet implemented!")
        }
    }
}

private fun unpackObjectSet(operands: List<Value>): OperationDto {
    val unpackedOperands = operands[0].asArrayValue()
    val operandType = unpackedOperands[0].asIntegerValue().asInt() // This can be a tensor, a list of tensors....
    val tupleDto = OperationDto()
    val data = when (operandType) {
        TYPE_TENSOR -> { mapTensor(unpackedOperands, tupleDto) }
        else -> {
            TODO("$operandType not yet implemented")
        }
    }
    return OperationDto(operandType, listOf(data))
}

private fun mapTensor(streamToDecode: ArrayValue, dto: OperationDto): TensorDto {
    val tensorDto = TensorDto()
    streamToDecode.forEachIndexed { index, value ->
        when (index) {
            0 -> {
                // TODO Is this the operation or was it consumed before?
                print("Value at position 0 -> ${value.valueType}")
            }
            1 -> {
                val listValues = value.asArrayValue().toList()
                tensorDto.id = listValues[0].asIntegerValue().toInt()
                tensorDto.data = listValues[1].asStringValue().asByteArray()
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
    return tensorDto
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

private fun mapOperation(operationDto: OperationDto): SyftMessage {
    return when (operationDto.op) {
        0 -> SyftMessage.SetObject(SyftTensor(operationDto.value.first().id.toLong(), operationDto.value.first().data))
        else -> {
            throw IllegalArgumentException("Operation ${operationDto.op} not yet supported")
        }
    }
}

class OperationDto(
    var op: Int = 0,
    var value: List<TensorDto> = mutableListOf()
) {
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
