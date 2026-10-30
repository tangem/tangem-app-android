package com.tangem.feature.swap.model

import androidx.compose.runtime.MutableState
import arrow.core.left
import com.google.common.truth.Truth.assertThat
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.transaction.error.GetFeeError
import com.tangem.feature.swap.domain.models.domain.SwapDataModel
import com.tangem.feature.swap.domain.models.ui.PermissionDataState
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class SwapModelTronGaslessDexFeeTest : SwapModelTestBase() {

    private val swapDataModel: SwapDataModel = mockk(relaxed = true)

    @BeforeEach
    fun setUp() {
        clearMocks(swapInteractor, swapFeatureToggles)
        setUpBase()
        coEvery {
            swapInteractor.loadSwapFee(
                quotesLoadedState = any(),
                fromStatus = any(),
                toStatus = any(),
                amount = any(),
                swapData = any(),
                selectedFeeToken = any(),
                isGasless = any(),
                txType = any(),
            )
        } returns GetFeeError.UnknownError.left()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class LoadFeeExtended {

        @Test
        fun `GIVEN Tron DEX swap and toggle on WHEN loadFeeExtended THEN loads gasless fee for quoted swap data`() =
            runTest {
                // Arrange
                every { swapFeatureToggles.isTronGaslessDexSwapEnabled } returns true
                val fromStatus = swapCurrencyStatus(currency = token(Blockchain.Tron))
                val model = createLoadedModel(fromStatus)

                // Act
                model.feeSelectorRepository.loadFeeExtended(selectedToken = null)

                // Assert
                coVerify(exactly = 1) {
                    swapInteractor.loadSwapFee(
                        quotesLoadedState = any(),
                        fromStatus = fromStatus,
                        toStatus = any(),
                        amount = any(),
                        swapData = swapDataModel,
                        selectedFeeToken = null,
                        isGasless = true,
                        txType = any(),
                    )
                }
            }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN DEX swap without Tron gasless WHEN loadFeeExtended THEN network is not supported`(
            model: UnsupportedModel,
        ) = runTest {
            // Arrange
            every { swapFeatureToggles.isTronGaslessDexSwapEnabled } returns model.isToggleEnabled
            val fromStatus = swapCurrencyStatus(currency = token(model.blockchain))
            val swapModel = createLoadedModel(fromStatus)

            // Act
            val result = swapModel.feeSelectorRepository.loadFeeExtended(selectedToken = null)

            // Assert
            assertThat(result).isEqualTo(GetFeeError.GaslessError.NetworkIsNotSupported.left())
            coVerify(exactly = 0) {
                swapInteractor.loadSwapFee(any(), any(), any(), any(), any(), any(), any(), any())
            }
        }

        private fun provideTestModels() = listOf(
            UnsupportedModel(blockchain = Blockchain.Tron, isToggleEnabled = false),
            UnsupportedModel(blockchain = Blockchain.Ethereum, isToggleEnabled = true),
        )
    }

    data class UnsupportedModel(val blockchain: Blockchain, val isToggleEnabled: Boolean)

    private fun createLoadedModel(fromStatus: com.tangem.domain.swap.models.SwapCurrencyStatus): SwapModel {
        val provider = swapProvider()
        val quoteState = quotesLoadedState(provider = provider, permissionState = PermissionDataState.Empty).also {
            every { it.txType } returns null
            every { it.swapDataModel } returns swapDataModel
        }
        return createModel().apply {
            dataState = dataState.copy(
                fromSwapCurrencyStatus = fromStatus,
                toSwapCurrencyStatus = swapCurrencyStatus(),
                selectedProvider = provider,
                lastLoadedSwapStates = mapOf(provider to quoteState),
            )
            setLastAmount("10")
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun SwapModel.setLastAmount(amount: String) {
        val field = SwapModel::class.java.getDeclaredField("lastAmount").apply { isAccessible = true }
        (field.get(this) as MutableState<String>).value = amount
    }

    private fun token(blockchain: Blockchain): CryptoCurrency.Token = mockk(relaxed = true) {
        every { network.rawId } returns blockchain.toNetworkId()
        every { decimals } returns 6
    }
}