package com.mccorby.openmined.worker.datasource.mapper

import android.util.Log
import com.mccorby.openmined.worker.domain.SyftCommand
import com.mccorby.openmined.worker.domain.SyftMessage
import com.mccorby.openmined.worker.domain.SyftOperand
import org.msgpack.core.MessagePack
import org.msgpack.value.ArrayValue
import org.msgpack.value.ImmutableArrayValue
import org.msgpack.value.ImmutableRawValue
import org.msgpack.value.Value
import org.msgpack.value.impl.*

private const val TAG = "MapperDS"

// These are values defined in PySyft
internal const val COMPRESSION_ENABLED = 49
internal const val NO_COMPRESSION = 40

// Operations in PySyft
internal const val CMD = 1
internal const val OBJ = 2
internal const val OBJ_REQ = 3
internal const val OBJ_DEL = 4
internal const val EXCEPTION = 5
internal const val IS_NONE = 6
internal const val GET_SHAPE = 7
internal const val SEARCH = 8

// Types are encoded in the stream sent from PySyft
internal const val TYPE_TENSOR = 0
internal const val TYPE_TUPLE = 1
internal const val TYPE_LIST = 2
internal const val TYPE_TENSOR_POINTER = 11

// Commands
private const val CMD_ADD = "__add__"

// TODO Probably Json or something similar. The name should reflect the format
fun SyftMessage.mapToString(): String {
    val packer = MessagePack.newDefaultBufferPacker()
    // TODO packer.packBlahBlahBlha
    return when (this) {
        is SyftMessage.OperationAck -> packer.packString(SyftMessage.OperationAck.toString())
        else -> {
            packer
        }
    }.toString()
}

fun SyftMessage.mapToByteArray(): ByteArray {
    // Assuming we are sending just a tensor
    // (0, (91189711850, b"numpy stuff", None, None, None, None))
    val packer = MessagePack.newDefaultBufferPacker()

    val operationArray = ImmutableArrayValueImpl(
        arrayOf<Value>(
            ImmutableLongValueImpl(TYPE_TENSOR.toLong()),
            ImmutableArrayValueImpl(
                arrayOf<Value>(
                    ImmutableLongValueImpl((this as SyftMessage.RespondToObjectRequest).objectToSend.id),
                    ImmutableStringValueImpl(objectToSend.byteArray),
                    // TODO Fill these values!
                    ImmutableNilValueImpl.get(),
                    ImmutableNilValueImpl.get(),
                    ImmutableNilValueImpl.get(),
                    ImmutableNilValueImpl.get()
                )
            )
        )
    )

    packer.packValue(operationArray)
    return packer.toByteArray()
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
        CMD -> unpackCommand(operands)
        OBJ_DEL -> unpackObjectDelete(operands)
        OBJ_REQ -> unpackObjectRequest(operands)
        else -> {
            TODO("Operation $operation not yet implemented!")
        }
    }
}

fun unpackObjectDelete(operands: List<Value>): OperationDto {
    val operand = operands[0].asNumberValue().toLong()
    val pointerDto = OperandDto.TensorPointerDto()
    pointerDto.id = operand
    return OperationDto(OBJ_DEL, value=listOf((pointerDto)))
}

fun unpackObjectRequest(operands: List<Value>): OperationDto {
    val operand = operands[0].asNumberValue().toLong()
    val pointerDto = OperandDto.TensorPointerDto()
    pointerDto.id = operand
    return OperationDto(OBJ_REQ, value=listOf((pointerDto)))
}

fun unpackCommand(operands: List<Value>): OperationDto {
    // operands comes in the form (2, listOfOperands), with the "2" meaning that a tuple comes next.
    // At this point we should have a list with the form [2, [command, [list of operands]]
    // TODO Check the type of the list of operands. Would it be possible to receive just one element and not a list?
    val listOfOperands = operands[0].asArrayValue().drop(1)[0].asArrayValue()
    val flatListOfOperands = if (listOfOperands[0].isArrayValue) {
        listOfOperands[0].asArrayValue().drop(1)[0]
    } else {
        listOfOperands
    }.asArrayValue()
    val command = flatListOfOperands[0].asStringValue().asString()

    return when (command) {
        CMD_ADD -> {
            val operationDto = OperationDto(op = CMD, command = command)
            // ["__add__",[2,[[0,[98058441856,"tensor_data",null,null,null,null]],[0,[31147267379,"tensor_data",null,null,null,null]]]]]
            // TODO Assuming we are receiving tensors... not always the case. This is just a start!
            // TODO Forcing how pointers are received. This has to be completely redone
            // ["__add__",
            //    [11,[35342533178,17228400254,"phone",null,[5]]],
            //    [2,[
            //        [11,[31778841511,91035389282,"phone",null,[5]]]]
            //        ]
            //    ,[5,{}]
            //]
            val tensorList = mutableListOf<OperandDto>()
            // Drop operation __add___
            val opWrapper = flatListOfOperands.drop(1)
            val op1 = opWrapper[0].asArrayValue()
            val op2 = opWrapper[1].asArrayValue().drop(1)[0].asArrayValue()[0].asArrayValue()
            tensorList.add(unpackOperandByType(op1))
            tensorList.add(unpackOperandByType(op2))
            val resultPointer = listOfOperands[1].asArrayValue()[1].asArrayValue()

            operationDto.value = tensorList.toList()
            operationDto.returnId = resultPointer.map { it.asNumberValue().toLong() }
            operationDto
        }
        else -> {
            TODO("$command not yet implemented!")
        }
    }
}

private fun unpackOperandByType(streamToDecode: ArrayValue): OperandDto {
    val type = streamToDecode[0].asIntegerValue().toInt()
    val operandArray = streamToDecode.drop(1)[0].asArrayValue()
    return when (type) {
        TYPE_TENSOR -> mapTensor(operandArray)
        TYPE_TENSOR_POINTER -> mapTensorPointer(operandArray)
        else -> {
            TODO("$type not yet implemented")
        }
    }
}

private fun mapTensorPointer(streamToDecode: ArrayValue): OperandDto.TensorPointerDto {
    // (64458802353, 7201727941, 'bob', None, torch.Size([1, 2]))
    val tensorDto = OperandDto.TensorPointerDto()
    tensorDto.id = streamToDecode[1].asNumberValue().toLong()
    // TODO The rest of attributes will come later
    return tensorDto
}

private fun unpackObjectSet(operands: List<Value>): OperationDto {
    val unpackedOperands = operands[0].asArrayValue()
    val data = unpackOperandByType(unpackedOperands)
    return OperationDto(OBJ, "", listOf(data))
}

private fun mapTensor(streamToDecode: ArrayValue): OperandDto {
    val tensorDto = OperandDto.TensorDto()
    tensorDto.id = streamToDecode[0].asNumberValue().toLong()
    tensorDto.data = streamToDecode[1].asStringValue().asByteArray()
    // 3 -> chain
    // 4 -> grad_chain
    // 5 -> tags
    // 6 -> tensor description
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
        OBJ -> {
            val operand = mapOperandToDomain(operationDto.value.first())
            SyftMessage.SetObject(operand)
        }
        CMD -> {
            val listOfSyftOperands = operationDto.value.map {
                mapOperandToDomain(it)
            }
            SyftMessage.ExecuteCommand(SyftCommand.Add(listOfSyftOperands, operationDto.returnId))
        }
        OBJ_DEL -> {
            SyftMessage.DeleteObject((operationDto.value[0] as OperandDto.TensorPointerDto).id)
        }
        OBJ_REQ -> {
            SyftMessage.GetObject((operationDto.value[0] as OperandDto.TensorPointerDto).id)
        }
        else -> {
            throw IllegalArgumentException("Operation ${operationDto.op} not yet supported")
        }
    }
}

private fun mapOperandToDomain(dto: OperandDto): SyftOperand {
    return when (dto) {
        is OperandDto.TensorDto -> SyftOperand.SyftTensor(dto.id, dto.data)
        is OperandDto.TensorPointerDto -> SyftOperand.SyftTensorPointer(dto.id)
    }
}

class OperationDto(
    var op: Int = 0,
    var command: String = "",
    var value: List<OperandDto> = mutableListOf(),
    var returnId: List<Long> = listOf()
) {
    override fun toString(): String {
        return "$op - $command - $value"
    }
}

sealed class OperandDto {
    class TensorDto : OperandDto() {
        var id: Long = 0
        var data: ByteArray = byteArrayOf()
    }

    class TensorPointerDto : OperandDto() {
        var id: Long = 0
    }
}
