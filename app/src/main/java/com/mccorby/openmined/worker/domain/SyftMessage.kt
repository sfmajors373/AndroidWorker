package com.mccorby.openmined.worker.domain

// TODO Check which params are required by each message
sealed class SyftMessage {
    data class ExecuteCommand(val command: SyftCommand) : SyftMessage() // And a list of tensors?

    data class SetObject(val objectToSet: SyftTensor): SyftMessage() {
        override fun equals(other: Any?): Boolean {
            return objectToSet.id == (other as SetObject).objectToSet.id
        }
    }
    data class RespondToObjectRequest(val objectToSet: Any): SyftMessage()
    data class DeleteObject(val objectToSet: Any): SyftMessage()
    data class ClientResponse(val tensorPointerId: Long): SyftMessage()
}

sealed class SyftCommand {
    data class AddPointers(val tensorPointers: List<SyftTensorId>) : SyftCommand()
    data class AddTensors(val tensors: List<SyftTensor>) : SyftCommand()
    data class Result(val result: SyftTensor?, val desc: String?): SyftCommand()
}