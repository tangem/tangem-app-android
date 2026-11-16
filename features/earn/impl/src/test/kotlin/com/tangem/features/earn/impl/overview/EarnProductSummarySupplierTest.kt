package com.tangem.features.earn.impl.overview

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.impl.di.EarnSupplierModule
import com.tangem.test.core.TestFlowProducerTools
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.test.core.getEmittedValues
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class EarnProductSummarySupplierTest {

    private val summary = createSummary(productId = PRODUCT_ID)
    private val summariesFlow = MutableStateFlow(mapOf(ETH_ID to summary))
    private val product = TestEarnProduct(id = PRODUCT_ID, summariesFlow = summariesFlow)

    @Test
    fun `GIVEN supplier called twice for one wallet WHEN nobody collects THEN product is requested once`() = runTest {
        // Arrange
        val supplier = createSupplier()

        // Act
        supplier(USER_WALLET_ID)
        supplier(USER_WALLET_ID)
        runCurrent()

        // Assert
        assertThat(product.summariesCalls).isEqualTo(1)
        assertThat(product.collectCount).isEqualTo(0)
    }

    @Test
    fun `GIVEN two subscribers of one wallet WHEN both are active THEN product flow is collected once`() = runTest {
        // Arrange
        val supplier = createSupplier()

        // Act
        getEmittedValues(supplier(USER_WALLET_ID))
        getEmittedValues(supplier(USER_WALLET_ID))
        runCurrent()

        // Assert
        assertThat(product.summariesCalls).isEqualTo(1)
        assertThat(product.collectCount).isEqualTo(1)
    }

    @Test
    fun `GIVEN two subscribers of one wallet WHEN product emits THEN both receive the same summaries`() = runTest {
        // Arrange
        val supplier = createSupplier()
        val expected = EarnProductSummaries(
            products = mapOf(ETH_ID to listOf(summary)),
            rateSources = emptyMap(),
            source = StatusSource.ACTUAL,
        )

        // Act
        val firstValues = getEmittedValues(supplier(USER_WALLET_ID))
        val secondValues = getEmittedValues(supplier(USER_WALLET_ID))
        runCurrent()

        // Assert
        assertThat(firstValues.last()).isEqualTo(expected)
        assertThat(secondValues.last()).isEqualTo(expected)
    }

    @Test
    fun `GIVEN subscriber of one wallet WHEN another wallet is subscribed THEN it gets its own producer`() = runTest {
        // Arrange
        val supplier = createSupplier()
        getEmittedValues(supplier(USER_WALLET_ID))
        getEmittedValues(supplier(USER_WALLET_ID))
        runCurrent()

        // Act
        getEmittedValues(supplier(OTHER_USER_WALLET_ID))
        runCurrent()

        // Assert
        assertThat(product.summariesCalls).isEqualTo(2)
        assertThat(product.collectCount).isEqualTo(2)
        assertThat(product.summariesWalletIds).containsExactly(USER_WALLET_ID, OTHER_USER_WALLET_ID).inOrder()
    }

    private fun TestScope.createSupplier(): EarnProductSummarySupplier {
        val registry = EarnProductsRegistry(products = setOf(product), rateSources = emptySet())
        val factory = object : EarnProductSummaryProducer.Factory {
            override fun create(params: EarnProductSummaryProducer.Params): EarnProductSummaryProducer =
                DefaultEarnProductSummaryProducer(
                    params = params,
                    flowProducerTools = TestFlowProducerTools(backgroundScope, StandardTestDispatcher(testScheduler)),
                    registry = registry,
                    order = EarnSummaryOrder(registry),
                    rateBadgeConverter = EarnRateBadgeConverter(),
                    dispatchers = createTestingCoroutineDispatcherProvider(),
                )
        }

        return EarnSupplierModule.provideEarnProductSummarySupplier(factory)
    }

    private companion object {

        val PRODUCT_ID = EarnProductId("product")

        val OTHER_USER_WALLET_ID = UserWalletId("fedcba9876543210")
    }
}