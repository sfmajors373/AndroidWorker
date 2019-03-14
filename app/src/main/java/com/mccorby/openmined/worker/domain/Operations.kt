package com.mccorby.openmined.worker.domain

interface Operations {
    fun add(tensor1: SyftTensor, tensor2: SyftTensor): SyftTensor

    // TODO Other operations
}
