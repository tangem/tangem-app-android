package com.tangem.features.onramp.tokenlist.model

import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.components.fields.InputManager
import com.tangem.core.ui.components.tokenlist.state.TokensListItemUM
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusListSupplier
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.BalanceHidingSettings
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.features.onramp.OnrampModelTestBase
import com.tangem.features.onramp.tokenlist.OnrampTokenListComponent
import com.tangem.features.onramp.tokenlist.entity.*
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class OnrampTokenListModelTest : OnrampModelTestBase() {
    @Test
    fun `GIVEN empty portfolio WHEN query entered THEN empty search is shown and destroy cancels collection`() = runTest {
        // Arrange
        val accounts = MutableStateFlow(mockk<AccountStatusList> {
            every { accountStatuses } returns emptyList()
        })
        val supplier = mockk<SingleAccountStatusListSupplier> {
            every { this@mockk.invoke(any<SingleAccountStatusListProducer.Params>()) } returns accounts
        }
        val appCurrency = mockk<GetSelectedAppCurrencyUseCase> {
            every { this@mockk.invoke() } returns flowOf(AppCurrency.Default.right())
        }
        val balance = mockk<GetBalanceHidingSettingsUseCase> {
            every { this@mockk.invoke() } returns flowOf(BalanceHidingSettings(true, true, false))
        }
        val mode = mockk<IsAccountsModeEnabledUseCase> { every { this@mockk.invoke() } returns flowOf(false) }
        val model = OnrampTokenListModel(
            paramsContainer = MutableParamsContainer(
                OnrampTokenListComponent.Params(OnrampOperation.BUY, userWalletId, { _, _ -> }),
            ),
            dispatchers = testDispatchers(),
            tokenListUMController = TokenListUMController(),
            searchManager = InputManager(),
            getSelectedAppCurrencyUseCase = appCurrency,
            getBalanceHidingSettingsUseCase = balance,
            getWalletsUseCase = mockk(),
            rampStateManager = mockk(),
            getUserCountryUseCase = mockk(),
            getAssetRequirementsUseCase = mockk(),
            singleAccountStatusListSupplier = supplier,
            isAccountsModeEnabledUseCase = mode,
            walletFeatureToggles = mockk { every { isRedesignedTokenRowsEnabled } returns false },
        )
        advanceUntilIdle()

        // Act
        model.state.value.searchBarUM.onQueryChange("missing")
        advanceUntilIdle()
        model.onDestroy()
        advanceUntilIdle()

        // Assert
        val data = model.state.value.tokensListData as TokenListUMData.TokenList
        assertThat(data.totalTokensCount).isEqualTo(0)
        assertThat(data.tokensList.single()).isInstanceOf(TokensListItemUM.Text::class.java)
        assertThat(model.state.value.isBalanceHidden).isTrue()
        assertThat(accounts.subscriptionCount.value).isEqualTo(0)
    }
}