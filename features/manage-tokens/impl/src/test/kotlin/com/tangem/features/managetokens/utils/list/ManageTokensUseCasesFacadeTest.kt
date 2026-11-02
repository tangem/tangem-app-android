package com.tangem.features.managetokens.utils.list

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.managetokens.GetManageTokensAllowedNetworksUseCase
import com.tangem.domain.managetokens.model.ManageTokensListConfig
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.managetokens.component.ManageTokensMode
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ManageTokensUseCasesFacadeTest {

    private val getManageTokensAllowedNetworksUseCase: GetManageTokensAllowedNetworksUseCase = mockk()

    private val accountId = AccountId.forMainCryptoPortfolio(UserWalletId("1234567890ABCDEF"))

    private val facade = ManageTokensUseCasesFacade(
        getManagedTokensUseCase = mockk(),
        getDistinctManagedTokensUseCase = mockk(),
        checkCurrencyUnsupportedUseCase = mockk(),
        coldWalletAndHasMissedDerivationsUseCase = mockk(),
        manageCryptoCurrenciesUseCase = mockk(),
        customTokensRepository = mockk(),
        singleAccountSupplier = mockk(),
        getManageTokensAllowedNetworksUseCase = getManageTokensAllowedNetworksUseCase,
        mode = ManageTokensMode.Account(accountId),
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(getManageTokensAllowedNetworksUseCase)
    }

    @Test
    fun `GIVEN tokens loaded from remote WHEN manageTokensListConfig THEN allowlist is not awaited`() = runTest {
        // Act
        val actual = facade.manageTokensListConfig(searchText = null, loadUserTokensFromRemote = true)

        // Assert
        val expected = ManageTokensListConfig(accountId = accountId, searchText = null, allowedNetworkIds = null)
        assertThat(actual).isEqualTo(expected)
        coVerify(exactly = 0) { getManageTokensAllowedNetworksUseCase(any()) }
    }

    @Test
    fun `GIVEN tokens loaded from cache WHEN manageTokensListConfig THEN allowlist is applied`() = runTest {
        // Arrange
        val allowedNetworkIds = setOf(Network.RawID("ethereum"))
        coEvery { getManageTokensAllowedNetworksUseCase(accountId) } returns allowedNetworkIds

        // Act
        val actual = facade.manageTokensListConfig(searchText = "eth", loadUserTokensFromRemote = false)

        // Assert
        val expected = ManageTokensListConfig(
            accountId = accountId,
            searchText = "eth",
            allowedNetworkIds = allowedNetworkIds,
        )
        assertThat(actual).isEqualTo(expected)
    }
}