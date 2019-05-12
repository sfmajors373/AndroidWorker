package com.mccorby.openmined.worker.framework

import com.mccorby.openmined.worker.domain.NO_ID
import com.mccorby.openmined.worker.domain.MLFramework
import com.mccorby.openmined.worker.domain.SyftOperand
import org.jetbrains.bio.npy.NpyFile
import org.nd4j.linalg.api.ndarray.INDArray
import org.nd4j.linalg.factory.Nd4j
import java.lang.ClassCastException
import java.nio.file.Files

fun INDArray.toSyftTensor(): SyftOperand.SyftTensor {
    // val byteArray = this.toNpyByteArray()
    // This is probably terrible performance wise but it works so... we keep it until DL4J fixes the issue with
    // the conversion to npy format.
    // See ticket open https://github.com/deeplearning4j/deeplearning4j/issues/7466
//    val path = Files.createTempFile(null, null)
//    val shape = mutableListOf<Int>()
//    this.shape().forEach { shape.add(it.toInt()) }
//    NpyFile.write(path, this.toFloatVector(), shape = shape.toIntArray())
//
//    return SyftOperand.SyftTensor(NO_ID, Files.readAllBytes(path))
    return SyftOperand.SyftTensor(NO_ID, Nd4j.toNpyByteArray(this))
}

fun SyftOperand.SyftTensor.toINDArray(): INDArray {
    val path = Files.createTempFile(null, null)
    Files.write(path, this.byteArray)
    val npyArray = NpyFile.read(path)
    val data = try {
        Nd4j.create(npyArray.asFloatArray().toList())
    } catch (cce: ClassCastException) {
        Nd4j.create((npyArray.data as LongArray).toList())
//        transformArrayToFloatArray(npyArray.asLongArray())

    }
//    return Nd4j.create(data)


//    return Nd4j.createFromNpyFile(path.toFile())
    return data
}

// Nd4J does not allow creating NDArrays from an int/long array.
// This is hardly more inefficient but it is the only way right now to receive long[] or int[] from PySyft
private fun transformArrayToFloatArray(data: LongArray): FloatArray {
    return data.map { it.toFloat() }.toFloatArray()
}

private fun transformToLongArray(floatVector: FloatArray): LongArray {
    return floatVector.map { it.toLong() }.toLongArray()
}

class DL4JFramework : MLFramework {
    override fun add(tensor1: SyftOperand.SyftTensor, tensor2: SyftOperand.SyftTensor): SyftOperand.SyftTensor {
        return tensor1.toINDArray().add(tensor2.toINDArray()).toSyftTensor()
    }
}
