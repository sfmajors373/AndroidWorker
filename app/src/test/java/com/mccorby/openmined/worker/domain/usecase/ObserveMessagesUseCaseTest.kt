package com.mccorby.openmined.worker.domain.usecase

import com.mccorby.openmined.worker.domain.MLFramework
import com.mccorby.openmined.worker.domain.SyftMessage
import com.mccorby.openmined.worker.domain.SyftOperand
import com.mccorby.openmined.worker.domain.SyftRepository
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verifyOrder
import io.reactivex.Flowable
import org.junit.Before
import org.junit.Test

class ObserveMessagesUseCaseTest {

    private val repository = mockk<SyftRepository>()
    private val mlFramework = mockk<MLFramework>()

    private lateinit var cut: ObserveMessagesUseCase

    @Before
    fun setUp() {
        cut = ObserveMessagesUseCase(repository, mlFramework)
    }

    @Test
    fun `Given a setObject message arrives then the object is stored and an ACK is sent back`() {
        val newMessage = mockk<SyftMessage.SetObject>()
        val objectToSet = mockk<SyftOperand.SyftTensor>()

        every { repository.onNewMessage() } returns Flowable.just(newMessage)
        every { newMessage.objectToSet } returns objectToSet
        every { repository.setObject(any()) } just Runs
        every { repository.sendMessage(any()) } just Runs

        val testObserver = cut.execute().test()

        testObserver.assertNoErrors()
            .assertValue(newMessage)

        verifyOrder {
            repository.setObject(objectToSet)
            repository.sendMessage(SyftMessage.OperationAck)
        }
    }
}