package com.mccorby.openmined.worker.domain

interface Operations {
    fun add(tensor1: SyftOperand.SyftTensor, tensor2: SyftOperand.SyftTensor): SyftOperand.SyftTensor
}
