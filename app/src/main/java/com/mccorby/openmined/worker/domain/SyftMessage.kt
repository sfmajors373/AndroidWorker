package com.mccorby.openmined.worker.domain

// TODO Check which params are required by each message
sealed class SyftMessage {
    data class ExecuteCommand(val command: SyftCommand) : SyftMessage() // And a list of tensors?

    data class SetObject(val objectToSet: SyftOperand): SyftMessage() {
        override fun equals(other: Any?): Boolean {
            return objectToSet.id == (other as SetObject).objectToSet.id
        }
    }
    data class RespondToObjectRequest(val objectToSet: Any): SyftMessage()
    data class DeleteObject(val objectToDelete: Long): SyftMessage()
    data class ClientResponse(val tensorPointerId: Long): SyftMessage()
    object OperationAck: SyftMessage() {
        override fun toString(): String {
            return "ACK"
        }
    }
}

sealed class SyftCommand {
    data class AddPointers(val tensorPointers: List<SyftOperand.SyftTensorPointer>) : SyftCommand()
    data class Add(val tensors: List<SyftOperand>) : SyftCommand()
    data class Result(val result: SyftOperand.SyftTensor?, val desc: String?): SyftCommand()
}