package com.mccorby.openmined.worker.domain.usecase

import com.mccorby.openmined.worker.domain.MLFramework
import com.mccorby.openmined.worker.domain.SyftMessage
import com.mccorby.openmined.worker.domain.SyftOperand
import com.mccorby.openmined.worker.domain.SyftRepository
import com.mccorby.openmined.worker.domain.SyftResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.verifyOrder
import io.reactivex.Flowable
import org.junit.Before
import org.junit.Test

class ObserveMessagesUseCaseTest {

    private val repository = mockk<SyftRepository>()
    private val mlFramework = mockk<MLFramework>()
    private val setObjectUseCase = mockk<SetObjectUseCase>(relaxed = true)

    private lateinit var cut: ObserveMessagesUseCase

    @Before
    fun setUp() {
        cut = ObserveMessagesUseCase(repository, mlFramework, setObjectUseCase)
    }

    @Test
    fun `Given a SetObject message then use case forwards it to SetObject use case`() {
        val newMessage = mockk<SyftMessage.SetObject>(relaxed = true)
        val objectToSet = mockk<SyftOperand>(relaxed = true)
        val expected = SyftResult.ObjectAdded(objectToSet)

        every { setObjectUseCase(newMessage) } returns expected
        every { repository.onNewMessage() } returns Flowable.just(newMessage)

        val testObserver = cut().test()

        testObserver.assertNoErrors()
            .assertValue(expected)

        verifyOrder {
            setObjectUseCase(newMessage)
        }
    }
}