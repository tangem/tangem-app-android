package com.tangem.data.account.repository

import app.cash.turbine.test
import com.google.common.truth.Truth
import com.tangem.common.test.datastore.MockStateDataStore
import com.tangem.domain.account.models.AccountExpandedState
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DefaultAccountsExpandedRepositoryTest {

    private val walletId = UserWalletId("011")
    private val mainAccountId = AccountId.forMainCryptoPortfolio(walletId)
    private val secondAccountId = AccountId.forCryptoPortfolio(walletId, DerivationIndex(1).getOrNull()!!)

    @Test
    fun `expandedAccounts emits updated state when store changes`() = runTest {
        val dataStore = MockStateDataStore<Map<String, Set<AccountsExpandedDTO>>>(
            default = emptyMap()
        )

        val repository = DefaultAccountsExpandedRepository(dataStore)

        repository.expandedAccounts.test {
            // initial emission
            val initial = awaitItem()
            Truth.assertThat(initial.isEmpty()).isTrue()

            // update store
            dataStore.updateData {
                mapOf(
                    walletId.stringValue to setOf(
                        AccountsExpandedDTO(
                            accountId = mainAccountId.value,
                            isExpanded = true
                        )
                    )
                )
            }

            // next emission
            val updated = awaitItem()
            val states = updated[walletId]!!

            Truth.assertThat(states.size).isEqualTo(1)
            val state = states.first()

            Truth.assertThat(state.accountId).isEqualTo(mainAccountId)
            Truth.assertThat(state.isExpanded).isTrue()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `expandedAccounts emits when update is called`() = runTest {
        val dataStore = MockStateDataStore<Map<String, Set<AccountsExpandedDTO>>>(
            default = emptyMap()
        )

        val repository = DefaultAccountsExpandedRepository(dataStore)

        val state = AccountExpandedState(
            accountId = mainAccountId,
            isExpanded = true
        )

        repository.expandedAccounts.test {
            // initial
            awaitItem()

            // when
            repository.update(state)

            // then
            val updated = awaitItem()
            val states = updated[walletId]!!

            Truth.assertThat(states.size).isEqualTo(1)
            Truth.assertThat(states.first().isExpanded).isTrue()

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `expandedAccounts emits synced state after syncStore`() = runTest {
        val dataStore = MockStateDataStore(
            mapOf(
                walletId.stringValue to setOf(
                    AccountsExpandedDTO(mainAccountId.value, true),
                    AccountsExpandedDTO(secondAccountId.value, false)
                )
            )
        )

        val repository = DefaultAccountsExpandedRepository(dataStore)

        repository.expandedAccounts.test {
            // initial
            val initial = awaitItem()
            Truth.assertThat(initial[walletId]!!.size).isEqualTo(2)

            // when
            repository.syncStore(
                walletId = walletId,
                existAccounts = setOf(mainAccountId) // without secondAccountId
            )

            // then
            val synced = awaitItem()
            val states = synced[walletId]!!

            Truth.assertThat(states.size).isEqualTo(1)
            Truth.assertThat(states.first().accountId.value).isEqualTo(mainAccountId.value)

            cancelAndIgnoreRemainingEvents()
        }
    }
}