package com.mccorby.openmined.worker.domain

// TODO Check which params are required by each message
sealed class SyftMessage {
    data class ExecuteCommand(val name: String) : SyftMessage() // And a list of tensors?

    data class SetObject(val objectToSet: SyftTensor): SyftMessage()
    data class RespondToObjectRequest(val objectToSet: Any): SyftMessage()
    data class DeleteObject(val objectToSet: Any): SyftMessage()
}