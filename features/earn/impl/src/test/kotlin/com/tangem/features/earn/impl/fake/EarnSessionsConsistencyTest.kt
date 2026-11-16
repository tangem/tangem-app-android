package com.tangem.features.earn.impl.fake

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.product.api.navigation.EarnFlowRoute
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.session.Deposit
import com.tangem.test.core.ProvideTestModels
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnSessionsConsistencyTest {

    private val params = EarnProduct.Params(
        userWalletId = UserWalletId("0123456789abcdef"),
        currency = mockk<CryptoCurrency>(),
    )

    private val lending = createFakeEarnProduct(FakeEarnProductPreset.Lending)
    private val staking = createFakeEarnProduct(FakeEarnProductPreset.Staking)
    private val vault = createFakeEarnProduct(FakeEarnProductPreset.Vault)

    @Test
    fun `GIVEN fake products and session factories WHEN keys compared THEN keys are the ids of lending and vault`() {
        // Arrange
        val expected = listOf(lending, vault).map { it.id.value }

        // Act
        val actual = fakeEarnSessionFactories.keys

        // Assert
        assertThat(actual).containsExactlyElementsIn(expected)
    }

    @Test
    fun `GIVEN staking WHEN session factories searched THEN it has no entry`() {
        // Act
        val actual = fakeEarnSessionFactories

        // Assert
        assertThat(actual).doesNotContainKey(staking.id.value)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN product with a session WHEN deposit clicked THEN the routed product has a session factory`(
        preset: FakeEarnProductPreset,
    ) {
        // Arrange
        val product = createFakeEarnProduct(preset)
        val navigator = FakeEarnNavigator()

        // Act
        product.actionHandler(params, navigator).onActionClick(Deposit())

        // Assert
        val routedIds = navigator.routes.filterIsInstance<EarnFlowRoute>().map { it.productId.value }
        assertThat(routedIds).containsExactly(product.id.value)
        assertThat(fakeEarnSessionFactories.keys).containsAtLeastElementsIn(routedIds)
    }

    @Test
    fun `GIVEN staking WHEN deposit clicked THEN no flow route is produced`() {
        // Arrange
        val navigator = FakeEarnNavigator()

        // Act
        staking.actionHandler(params, navigator).onActionClick(Deposit())

        // Assert
        assertThat(navigator.routes.filterIsInstance<EarnFlowRoute>()).isEmpty()
    }

    private fun provideTestModels() = listOf(FakeEarnProductPreset.Lending, FakeEarnProductPreset.Vault)
}