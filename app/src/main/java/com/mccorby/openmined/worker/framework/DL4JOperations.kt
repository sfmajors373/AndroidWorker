package com.mccorby.openmined.worker.framework

import com.mccorby.openmined.worker.domain.Operations
import com.mccorby.openmined.worker.domain.SyftTensor
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j

// This mapper will transform a SyftTensor to and from an INDArray
fun INDArray.toSyftTensor(): SyftTensor {
    return SyftTensor()
}

fun SyftTensor.toINDArray(): INDArray {
    // Nd4.createNpyFromByteArray()
    return Nd4j.createNpyFromByteArray(this.byteArray)
}

class DL4JOperations : Operations {

    override fun add(tensor1: SyftTensor, tensor2: SyftTensor): SyftTensor {
        return tensor1.toINDArray().add(tensor2.toINDArray()).toSyftTensor()
    }
}