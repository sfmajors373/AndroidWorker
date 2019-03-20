package com.mccorby.openmined.worker.framework

import com.mccorby.openmined.worker.domain.NO_ID
import com.mccorby.openmined.worker.domain.Operations
import com.mccorby.openmined.worker.domain.SyftTensor
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j

fun INDArray.toSyftTensor(): SyftTensor {
    return SyftTensor(NO_ID, Nd4j.toNpyByteArray(this))
}

fun SyftTensor.toINDArray(): INDArray {
    return Nd4j.createNpyFromByteArray(this.byteArray)
}

class DL4JOperations : Operations {
    override fun add(tensor1: SyftTensor, tensor2: SyftTensor): SyftTensor {
        return tensor1.toINDArray().add(tensor2.toINDArray()).toSyftTensor()
    }
}