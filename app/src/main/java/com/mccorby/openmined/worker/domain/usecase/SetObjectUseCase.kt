package com.mccorby.openmined.worker.domain.usecase

import com.mccorby.openmined.worker.domain.SyftMessage
import com.mccorby.openmined.worker.domain.SyftOperand
import com.mccorby.openmined.worker.domain.SyftRepository

class SetObjectUseCase(private val syftRepository: SyftRepository) {

    operator fun invoke(syftMessage: SyftMessage.SetObject): SyftMessage {
        syftRepository.setObject(syftMessage.objectToSet as SyftOperand.SyftTensor)
        syftRepository.sendMessage(SyftMessage.OperationAck)
        return syftMessage
    }
}