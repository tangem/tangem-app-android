package com.tangem.domain.walletconnect

import app.cash.turbine.test
import arrow.core.left
import arrow.core.right
import com.tangem.data.walletconnect.sign.FinalActionCollector
import com.tangem.data.walletconnect.sign.MiddleActionCollector
import com.tangem.data.walletconnect.sign.SignCollector
import com.tangem.data.walletconnect.sign.SignStateConverter.toResult
import com.tangem.data.walletconnect.sign.SignStateConverter.toSigning
import com.tangem.data.walletconnect.sign.WcSignUseCaseDelegate
import com.tangem.domain.walletconnect.usecase.sign.WcSignState
import com.tangem.domain.walletconnect.usecase.sign.WcSignStep
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

internal class WcSignUseCaseDelegateTest {

    private var middleActionCollector: MiddleActionCollector<TestMiddleAction, TestSignModel> =
        object : MiddleActionCollector<TestMiddleAction, TestSignModel> {}
    private var finalActionCollector: FinalActionCollector<TestSignModel> =
        object : FinalActionCollector<TestSignModel> {}
    private val initSignModel = TestSignModel()

    private val initState = WcSignState(initSignModel, WcSignStep.PreSign)
    private val signing = initState.toSigning()
    private val result = signing.toResult(Unit.right())
    private val testException = RuntimeException("test")

    private val successSign: suspend FlowCollector<WcSignState<TestSignModel>>.(
        currentState: WcSignState<TestSignModel>,
    ) -> Unit = { state ->
        delay(2)
        emit(state.toResult(Unit.right()))
    }

    private val failedSign: suspend FlowCollector<WcSignState<TestSignModel>>.(
        currentState: WcSignState<TestSignModel>,
    ) -> Unit
        get() = { state ->
            delay(2)
            emit(state.toResult(testException.left()))
        }

    @Before
    fun setup() {
        middleActionCollector = object : MiddleActionCollector<TestMiddleAction, TestSignModel> {}
        finalActionCollector = object : FinalActionCollector<TestSignModel> {}
    }

    @Test
    fun `invoke and keep flow running`() = runTest {
        finalActionCollector = object : FinalActionCollector<TestSignModel> {
            override suspend fun SignCollector<TestSignModel>.onSign(state: WcSignState<TestSignModel>) {
                successSign(state)
            }
        }
        val useCase = WcSignUseCaseDelegate(
            finalActionCollector = finalActionCollector,
            middleActionCollector = middleActionCollector,
        )

        useCase.invoke(initSignModel).test {
            assertEquals(initState, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `success sign, keep flow running`() = runTest {
        finalActionCollector = object : FinalActionCollector<TestSignModel> {
            override suspend fun SignCollector<TestSignModel>.onSign(state: WcSignState<TestSignModel>) {
                successSign(state)
            }
        }
        val useCase = WcSignUseCaseDelegate(
            finalActionCollector = finalActionCollector,
            middleActionCollector = middleActionCollector,
        )

        useCase.invoke(initModel = initSignModel).test {
            assertEquals(initState, awaitItem())
            useCase.sign()
            assertEquals(signing, awaitItem())
            assertEquals(result, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `failed sign, keep flow running`() = runTest {
        val failedResult = signing.toResult(testException.left())
        finalActionCollector = object : FinalActionCollector<TestSignModel> {
            override suspend fun SignCollector<TestSignModel>.onSign(state: WcSignState<TestSignModel>) {
                failedSign(state)
            }
        }
        val useCase = WcSignUseCaseDelegate(
            finalActionCollector = finalActionCollector,
            middleActionCollector = middleActionCollector,
        )

        useCase.invoke(initSignModel).test {
            assertEquals(initState, awaitItem())
            useCase.sign()
            assertEquals(signing, awaitItem())
            assertEquals(failedResult, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `failed sign and catch unknown exception`() = runTest {
        val exception = RuntimeException("asd")
        val expectedErrorState = signing.toResult(exception.left())
        finalActionCollector = object : FinalActionCollector<TestSignModel> {
            override suspend fun SignCollector<TestSignModel>.onSign(state: WcSignState<TestSignModel>) {
                delay(2)
                throw exception
            }
        }
        val useCase = WcSignUseCaseDelegate(
            finalActionCollector = finalActionCollector,
            middleActionCollector = middleActionCollector,
        )

        useCase.invoke(initSignModel).test {
            assertEquals(initState, awaitItem())
            useCase.sign()
            assertEquals(signing, awaitItem())
            assertEquals(expectedErrorState, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `interrupt signing and complete flow on cancel call`() = runTest {
        finalActionCollector = object : FinalActionCollector<TestSignModel> {
            override suspend fun SignCollector<TestSignModel>.onSign(state: WcSignState<TestSignModel>) {
                delay(5)
                emit(result)
            }
        }
        val useCase = WcSignUseCaseDelegate(
            finalActionCollector = finalActionCollector,
            middleActionCollector = middleActionCollector,
        )

        useCase.invoke(initSignModel).test {
            assertEquals(initState, awaitItem())
            useCase.sign()
            assertEquals(signing, awaitItem())
            delay(2)
            useCase.cancel()
            awaitComplete()
        }
    }

    @Test
    fun `ignore multi time sign call till signed`() = runTest {
        var count = 0
        val startLoading = WcSignState(TestSignModel("startLoading 1"), WcSignStep.Signing)
        val startLoading2 = WcSignState(TestSignModel("startLoading 2"), WcSignStep.Signing)
        val expectedSignResult = result

        finalActionCollector = object : FinalActionCollector<TestSignModel> {
            override suspend fun SignCollector<TestSignModel>.onSign(state: WcSignState<TestSignModel>) {
                // should emit single time in this test
                emit(if (count % 2 == 0) startLoading else startLoading2)
                count = count.inc()
                delay(10)
                emit(expectedSignResult)
            }
        }
        val useCase = WcSignUseCaseDelegate(
            finalActionCollector = finalActionCollector,
            middleActionCollector = middleActionCollector,
        )

        useCase.invoke(initSignModel).test {
            useCase.sign()
            delay(2)
            assertEquals(startLoading, expectMostRecentItem())

            // should ignore
            useCase.sign()
            delay(2)
            expectNoEvents()

            // should ignore
            useCase.sign()
            expectNoEvents()

            delay(8)
            assertEquals(expectedSignResult, expectMostRecentItem())
            expectNoEvents()
        }
    }

    @Test
    fun `ignore middle actions while signing, on failed collect middle actions again`() = runTest {
        val firstTextMode = TestSignModel(TestMiddleAction.One().newTestStr)
        val firstMiddleUpdate = WcSignState(
            signModel = firstTextMode,
            domainStep = WcSignStep.PreSign,
        )
        val startLoading = firstMiddleUpdate.toSigning()
        val failedSign = startLoading.toResult(testException.left())
        val thirdMiddleUpdate = WcSignState(
            signModel = TestSignModel(TestMiddleAction.Three().newTestStr),
            domainStep = WcSignStep.PreSign,
        )

        finalActionCollector = object : FinalActionCollector<TestSignModel> {
            override suspend fun SignCollector<TestSignModel>.onSign(state: WcSignState<TestSignModel>) {
                delay(6)
                emit(failedSign)
            }
        }
        middleActionCollector = object : MiddleActionCollector<TestMiddleAction, TestSignModel> {
            override suspend fun FlowCollector<TestSignModel>.onMiddleAction(
                signModel: TestSignModel,
                middleAction: TestMiddleAction,
            ) {
                emit(signModel.copy(testStr = middleAction.newTestStr))
            }
        }
        val useCase = WcSignUseCaseDelegate(
            finalActionCollector = finalActionCollector,
            middleActionCollector = middleActionCollector,
        )

        useCase.invoke(initSignModel).test {
            delay(2)
            useCase.middleAction(TestMiddleAction.One())
            assertEquals(firstMiddleUpdate, expectMostRecentItem())

            useCase.sign()
            assertEquals(startLoading, awaitItem())

            // should ignore
            delay(2)
            useCase.middleAction(TestMiddleAction.Two())

            delay(3)
            assertEquals(failedSign, awaitItem())

            // continue listen
            delay(2)
            useCase.middleAction(TestMiddleAction.Three())
            assertEquals(thirdMiddleUpdate, awaitItem())
        }
    }

    @Test
    fun `buffered middle actions and drop on sign call`() = runTest {
        val firstTextMode = TestSignModel(TestMiddleAction.One().newTestStr)
        val expectedFirst = initState.copy(signModel = firstTextMode)
        val expectedSecond = initState.copy(signModel = TestSignModel(TestMiddleAction.Two().newTestStr))

        finalActionCollector = object : FinalActionCollector<TestSignModel> {
            override suspend fun SignCollector<TestSignModel>.onSign(state: WcSignState<TestSignModel>) {
                successSign(state)
            }
        }
        middleActionCollector = object : MiddleActionCollector<TestMiddleAction, TestSignModel> {
            override suspend fun FlowCollector<TestSignModel>.onMiddleAction(
                signModel: TestSignModel,
                middleAction: TestMiddleAction,
            ) {
                emit(signModel.copy(testStr = middleAction.newTestStr))
                delay(4)
            }
        }
        val useCase = WcSignUseCaseDelegate(
            finalActionCollector = finalActionCollector,
            middleActionCollector = middleActionCollector,
        )

        useCase.invoke(initSignModel).test {
            assertEquals(initState, awaitItem())
            useCase.middleAction(TestMiddleAction.One())
            useCase.middleAction(TestMiddleAction.Two())
            // must be dropped
            useCase.middleAction(TestMiddleAction.Three())

            // 0 - 4 -> "one" is emitted
            // 4 - 8 -> "two" is emitted
            // 8 - 12 -> "Signing" is emitted, "three" ignored
            delay(2)
            assertEquals(expectedFirst, awaitItem())
            delay(4)
            assertEquals(expectedSecond, awaitItem())

            useCase.sign()
            delay(4)
            assertEquals(expectedSecond.toSigning(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    internal data class TestSignModel(val testStr: String = "testStr")

    internal sealed interface TestMiddleAction {
        val newTestStr: String

        data class One(override val newTestStr: String = "Middle Action One") : TestMiddleAction
        data class Two(override val newTestStr: String = "Middle Action Two") : TestMiddleAction
        data class Three(override val newTestStr: String = "Middle Action Three") : TestMiddleAction
    }
}