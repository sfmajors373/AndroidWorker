package com.mccorby.openmined.worker.framework

import com.mccorby.openmined.worker.domain.MLFramework
import com.mccorby.openmined.worker.domain.NO_ID
import com.mccorby.openmined.worker.domain.SyftOperand
import org.jetbrains.bio.npy.NpyFile
import java.nio.file.Files

fun MPCTensor.toSyftTensor(): SyftOperand.SyftTensor {
    // val byteArray = this.toNpyByteArray()
    // This is probably terrible performance wise but it works so... we keep it until DL4J fixes the issue with
    // the conversion to npy format.
    // See ticket open https://github.com/deeplearning4j/deeplearning4j/issues/7466
    val path = Files.createTempFile(null, null)
    val shape = mutableListOf<Int>()
    this.shape.forEach { shape.add(it) }
    NpyFile.write(path, this.data, shape = shape.toIntArray())
    return SyftOperand.SyftTensor(NO_ID, Files.readAllBytes(path))
}

fun SyftOperand.SyftTensor.toMPCTensor(): MPCTensor {
    val path = Files.createTempFile(null, null)
    Files.write(path, this.byteArray)
    val npyArray = NpyFile.read(path)
    val data: LongArray = try {
        npyArray.data as LongArray
    } catch (cce: ClassCastException) {
        throw ClassCastException("MPC Tensors can only deal with tensors of Long data type ")
    }
    return MPCTensor(data, npyArray.shape)
}

class MPCFramework : MLFramework {

    override fun add(tensor1: SyftOperand.SyftTensor, tensor2: SyftOperand.SyftTensor): SyftOperand.SyftTensor {
        return tensor1.toMPCTensor().add(tensor2.toMPCTensor()).toSyftTensor()
    }
}

data class MPCTensor(val data: LongArray, val shape: IntArray)

// TODO This is extremely ineffective and it's here for demo purposes only
fun MPCTensor.add(tensor2: MPCTensor): MPCTensor {
    assert(this.shape.contentEquals(tensor2.shape))
    // TODO just 1 dim to test
    return MPCTensor(longArrayOf(this.data[0] + tensor2.data[0]), this.shape)
}