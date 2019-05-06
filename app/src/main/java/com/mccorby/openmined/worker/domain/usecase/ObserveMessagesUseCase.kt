package com.mccorby.openmined.worker.domain.usecase

import com.mccorby.openmined.worker.domain.SyftMessage
import com.mccorby.openmined.worker.domain.SyftRepository
import com.mccorby.openmined.worker.domain.SyftResult
import io.reactivex.Flowable

// TODO This use case should be a composite of different use cases. It will need to route to the use case the message
class ObserveMessagesUseCase(
    private val syftRepository: SyftRepository,
    private val setObjectUseCase: SetObjectUseCase,
    private val executeCommandUseCase: ExecuteCommandUseCase
) {

    operator fun invoke(): Flowable<SyftResult> {
        return syftRepository.onNewMessage()
            .map { processNewMessage(it) }
    }

    private fun processNewMessage(newSyftMessage: SyftMessage): SyftResult {
        return when (newSyftMessage) {
            is SyftMessage.SetObject -> {
                setObjectUseCase(newSyftMessage)
            }
            is SyftMessage.ExecuteCommand -> {
                executeCommandUseCase(newSyftMessage)
            }
            is SyftMessage.GetObject -> {
                val tensor = syftRepository.getObject(newSyftMessage.tensorPointerId)
                // TODO copy should not be necessary. Here set just to make it work. This is a value that should have been already set before
                syftRepository.sendMessage(SyftMessage.RespondToObjectRequest(tensor.copy(id = newSyftMessage.tensorPointerId)))
                SyftResult.ObjectRetrieved(tensor)
            }
            is SyftMessage.DeleteObject -> {
                syftRepository.removeObject(newSyftMessage.objectToDelete)
                syftRepository.sendMessage(SyftMessage.OperationAck)
                SyftResult.ObjectRemoved(newSyftMessage.objectToDelete)
            }
            else -> {
                SyftResult.UnexpectedResult
            }
        }
    }
}