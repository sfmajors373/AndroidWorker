package com.mccorby.openmined.worker.domain.usecase

import com.mccorby.openmined.worker.domain.MLFramework
import com.mccorby.openmined.worker.domain.SyftCommand
import com.mccorby.openmined.worker.domain.SyftMessage
import com.mccorby.openmined.worker.domain.SyftOperand
import com.mccorby.openmined.worker.domain.SyftRepository
import com.mccorby.openmined.worker.domain.SyftResult
import io.reactivex.Flowable

// TODO This use case should be a composite of different use cases. It will need to route to the use case the message
class ObserveMessagesUseCase(
    private val syftRepository: SyftRepository,
    private val mlFramework: MLFramework,
    private val setObjectUseCase: SetObjectUseCase
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
                val result = createCommandEvent(newSyftMessage)
                SyftResult.CommandResult(newSyftMessage.command, result)
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