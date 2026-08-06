package com.tangem.datasource.local.token

import com.google.common.truth.Truth.assertThat
import com.tangem.core.local.datastore.RuntimeSharedMapStore
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.stakekit.action.StakingAction
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

internal class DefaultStakingActionsStoreTest {

    private val store = DefaultStakingActionsStore(store = RuntimeSharedMapStore())

    private val walletId = mockk<UserWalletId>()
    private val currencyId = mockk<CryptoCurrency.ID>()

    @Test
    fun `GIVEN stored actions WHEN get THEN flow emits them`() = runTest {
        // Arrange
        val actions = listOf(mockk<StakingAction>())
        store.store(walletId, currencyId, actions)

        // Assert
        assertThat(store.get(walletId, currencyId).first()).isEqualTo(actions)
    }

    @Test
    fun `GIVEN different wallet-currency keys WHEN get THEN each is independent`() = runTest {
        // Arrange
        val otherCurrencyId = mockk<CryptoCurrency.ID>()
        val actions = listOf(mockk<StakingAction>())
        val otherActions = listOf(mockk<StakingAction>(), mockk<StakingAction>())
        store.store(walletId, currencyId, actions)
        store.store(walletId, otherCurrencyId, otherActions)

        // Assert
        assertThat(store.get(walletId, currencyId).first()).isEqualTo(actions)
        assertThat(store.get(walletId, otherCurrencyId).first()).isEqualTo(otherActions)
    }
}