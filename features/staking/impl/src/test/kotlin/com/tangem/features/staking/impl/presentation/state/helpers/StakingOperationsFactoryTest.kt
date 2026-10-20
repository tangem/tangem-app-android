package com.tangem.features.staking.impl.presentation.state.helpers

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.features.staking.impl.fixtures.allStakeKitIds
import com.tangem.features.staking.impl.fixtures.p2pIntegration
import com.tangem.features.staking.impl.fixtures.stakeKitIntegration
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.Provider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class StakingOperationsFactoryTest {

    private val stakeKitFeeLoaderFactory: StakeKitFeeLoader.Factory = mockk()
    private val p2pFeeLoaderFactory: P2PEthPoolFeeLoader.Factory = mockk()
    private val stakeKitSenderFactory: StakeKitTransactionSender.Factory = mockk()
    private val p2pSenderFactory: P2PEthPoolTransactionSender.Factory = mockk()

    private val stakeKitLoader: StakeKitFeeLoader = mockk()
    private val p2pLoader: P2PEthPoolFeeLoader = mockk()
    private val stakeKitSender: StakeKitTransactionSender = mockk()
    private val p2pSender: P2PEthPoolTransactionSender = mockk()

    private val provider = Provider { status(currency = solanaCoin) }
    private val userWallet = realUserWallet

    private val factory = StakingOperationsFactory(
        stakeKitFeeLoaderFactory = stakeKitFeeLoaderFactory,
        p2pEthPoolFeeLoaderFactory = p2pFeeLoaderFactory,
        stakeKitTransactionSenderFactory = stakeKitSenderFactory,
        p2pEthPoolTransactionSenderFactory = p2pSenderFactory,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(stakeKitFeeLoaderFactory, p2pFeeLoaderFactory, stakeKitSenderFactory, p2pSenderFactory)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CreateFeeLoader {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN stakeKit integration of each chain WHEN createFeeLoader THEN stakeKit loader created with all inputs`(
            model: IntegrationModel,
        ) {
            // Arrange
            val integration = stakeKitIntegration(model.id)
            every { stakeKitFeeLoaderFactory.create(any(), any(), any()) } returns stakeKitLoader

            // Act
            val actual = factory.createFeeLoader(provider, userWallet, integration)

            // Assert
            assertThat(actual).isSameInstanceAs(stakeKitLoader)
            verify(exactly = 1) {
                stakeKitFeeLoaderFactory.create(
                    cryptoCurrencyStatusProvider = provider,
                    userWallet = userWallet,
                    integration = integration,
                )
            }
            verify(exactly = 0) { p2pFeeLoaderFactory.create(any(), any()) }
        }

        @Test
        fun `GIVEN p2p integration WHEN createFeeLoader THEN p2p loader created without user wallet`() {
            // Arrange
            val integration = p2pIntegration()
            every { p2pFeeLoaderFactory.create(any(), any()) } returns p2pLoader

            // Act
            val actual = factory.createFeeLoader(provider, userWallet, integration)

            // Assert
            assertThat(actual).isSameInstanceAs(p2pLoader)
            verify(exactly = 1) {
                p2pFeeLoaderFactory.create(cryptoCurrencyStatusProvider = provider, integration = integration)
            }
            verify(exactly = 0) { stakeKitFeeLoaderFactory.create(any(), any(), any()) }
        }

        private fun provideTestModels() = allStakeKitIds.map(::IntegrationModel)
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CreateTransactionSender {

        @ParameterizedTest
        @ValueSource(booleans = [true, false])
        fun `GIVEN stakeKit integration and subtract flag WHEN createTransactionSender THEN stakeKit sender created with flag forwarded`(
            isAmountSubtractAvailable: Boolean,
        ) {
            // Arrange
            val integration = stakeKitIntegration(StakingIntegrationID.StakeKit.Coin.Solana)
            every { stakeKitSenderFactory.create(cryptoCurrencyStatusProvider = any(), userWallet = any(), integration = any(), isAmountSubtractAvailable = any()) } returns stakeKitSender

            // Act
            val actual = factory.createTransactionSender(
                cryptoCurrencyStatusProvider = provider,
                userWallet = userWallet,
                integration = integration,
                isAmountSubtractAvailable = isAmountSubtractAvailable,
            )

            // Assert
            assertThat(actual).isSameInstanceAs(stakeKitSender)
            verify(exactly = 1) {
                stakeKitSenderFactory.create(
                    cryptoCurrencyStatusProvider = provider,
                    userWallet = userWallet,
                    integration = integration,
                    isAmountSubtractAvailable = isAmountSubtractAvailable,
                )
            }
            verify(exactly = 0) { p2pSenderFactory.create(any(), any(), any()) }
        }

        @Test
        fun `GIVEN p2p integration WHEN createTransactionSender THEN p2p sender created and subtract flag ignored`() {
            // Arrange
            val integration = p2pIntegration()
            every { p2pSenderFactory.create(any(), any(), any()) } returns p2pSender

            // Act
            val actual = factory.createTransactionSender(
                cryptoCurrencyStatusProvider = provider,
                userWallet = userWallet,
                integration = integration,
                isAmountSubtractAvailable = true,
            )

            // Assert
            assertThat(actual).isSameInstanceAs(p2pSender)
            verify(exactly = 1) { p2pSenderFactory.create(provider, userWallet, integration) }
            verify(
                exactly = 0,
            ) { stakeKitSenderFactory.create(
                cryptoCurrencyStatusProvider = any(),
                userWallet = any(),
                integration = any(),
                isAmountSubtractAvailable = any(),
            ) }
        }
    }

    internal data class IntegrationModel(val id: StakingIntegrationID.StakeKit)
}