package com.mccorby.openmined.worker.domain.usecase

import com.mccorby.openmined.worker.domain.Operations
import com.mccorby.openmined.worker.domain.SyftCommand
import com.mccorby.openmined.worker.domain.SyftMessage
import com.mccorby.openmined.worker.domain.SyftOperand
import com.mccorby.openmined.worker.domain.SyftRepository
import io.reactivex.Flowable

// TODO This use case should be a composite of different use cases. It will need to route to the use case the message
class ObserveMessagesUseCase(private val syftRepository: SyftRepository, private val mlFramework: Operations) {

    fun execute(): Flowable<SyftMessage> {
        return syftRepository.onNewMessage()
            .map { processNewMessage(it) }
    }

    private fun processNewMessage(newSyftMessage: SyftMessage): SyftMessage {
        when (newSyftMessage) {
            is SyftMessage.SetObject -> {
                syftRepository.setObject(newSyftMessage.objectToSet as SyftOperand.SyftTensor)
                syftRepository.sendMessage(SyftMessage.OperationAck)
            }
            is SyftMessage.ExecuteCommand -> {
                createCommandEvent(newSyftMessage)
            }
            is SyftMessage.GetObject -> {
                val tensor = syftRepository.getObject(newSyftMessage.tensorPointerId)
                // TODO copy should not be necessary. Here set just to make it work. This is a value that should have been already set before
                syftRepository.sendMessage(SyftMessage.RespondToObjectRequest(tensor.copy(id = newSyftMessage.tensorPointerId)))
            }
            is SyftMessage.DeleteObject -> {
                syftRepository.removeObject(newSyftMessage.objectToDelete)
                syftRepository.sendMessage(SyftMessage.OperationAck)
            }
            else -> {
            }
        }
        return newSyftMessage
    }

    private fun createCommandEvent(syftMessage: SyftMessage.ExecuteCommand): SyftOperand.SyftTensor {
        return when (syftMessage.command) {
            is SyftCommand.Add -> {
                when (syftMessage.command.tensors[0]) {
                    is SyftOperand.SyftTensor -> {
                        mlFramework.add(
                            syftMessage.command.tensors[0] as SyftOperand.SyftTensor,
                            syftMessage.command.tensors[1] as SyftOperand.SyftTensor
                        )
                    }
                    is SyftOperand.SyftTensorPointer -> {
                        val result = mlFramework.add(
                            syftRepository.getObject(syftMessage.command.tensors[0].id),
                            syftRepository.getObject(syftMessage.command.tensors[1].id)
                        )
                        // Add only expects now a single return id
                        val resultId = syftMessage.command.resultIds[0]
                        syftRepository.setObject(resultId, result)
                        syftRepository.sendMessage(SyftMessage.OperationAck)
                        result
                    }
                }
            }
        }
    }

}