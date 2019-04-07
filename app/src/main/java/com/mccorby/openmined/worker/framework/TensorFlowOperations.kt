package com.mccorby.openmined.worker.framework

import com.mccorby.openmined.worker.domain.Operations
import com.mccorby.openmined.worker.domain.SyftOperand

class TensorFlowOperations : Operations {

    override fun add(tensor1: SyftOperand.SyftTensor, tensor2: SyftOperand.SyftTensor): SyftOperand.SyftTensor {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        // // Let's say graph is an instance of the Graph class
        // // for the computation y = 3 * x
        //
        // try (Session s = new Session(graph)) {
        //   try (Tensor x = Tensor.create(2.0f);
        //       Tensor y = s.runner().feed("x", x).fetch("y").run().get(0)) {
        //       System.out.println(y.floatValue());  // Will print 6.0f
        //   }
        //   try (Tensor x = Tensor.create(1.1f);
        //       Tensor y = s.runner().feed("x", x).fetch("y").run().get(0)) {
        //       System.out.println(y.floatValue());  // Will print 3.3f
        //   }
        // }
    }
}