package com.tangem.features.commonfeatures.impl.userportfolio.state

import com.tangem.common.ui.userwallet.converter.WalletIconUMConverter
import com.tangem.domain.account.status.usecase.IsAccountsModeEnabledUseCase
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.wallets.usecase.GetWalletIconUseCase
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.commonfeatures.api.addtoportfolio.AvailableToAddData
import com.tangem.features.commonfeatures.impl.userportfolio.model.UserPortfolioUM
import com.tangem.features.commonfeatures.impl.userportfolio.transformer.UserPortfolioSectionsTransformer
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

@Suppress("LongParameterList")
internal class UserPortfolioStateController @AssistedInject constructor(
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    isAccountsModeEnabledUseCase: IsAccountsModeEnabledUseCase,
    private val getWalletIconUseCase: GetWalletIconUseCase,
    private val walletIconUMConverter: WalletIconUMConverter,
    @Assisted private val modelScope: CoroutineScope,
    @Assisted private val onTokenSelected: (AddToPortfolioManager.Result) -> Unit,
) {

    private val requiredDataFlow = MutableSharedFlow<RequiredData>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val uiState: StateFlow<UserPortfolioUM?> = combine(
        flow = requiredDataFlow.distinctUntilChanged(),
        flow2 = getSelectedAppCurrencyUseCase.invokeOrDefault(),
        flow3 = getBalanceHidingSettingsUseCase.isBalanceHidden(),
        flow4 = isAccountsModeEnabledUseCase(),
    ) { requiredData, appCurrency, isBalanceHidden, isAccountsModeEnabled ->
        UserPortfolioSectionsTransformer(
            availableData = requiredData.allAvailableData,
            rawCurrencyId = requiredData.rawCurrencyId,
            networkIds = requiredData.networkIds,
            appCurrency = appCurrency,
            isBalanceHidden = isBalanceHidden,
            isAccountsModeEnabled = isAccountsModeEnabled,
            resolveWalletDeviceIcon = {
                walletIconUMConverter.convert(getWalletIconUseCase(it))
            },
            onTokenSelected = onTokenSelected,
        ).transform()
    }
        .distinctUntilChanged()
        .stateIn(modelScope, SharingStarted.Lazily, null)

    /**
     * @param networkIds when not `null`, only the holdings on these networks are listed
     */
    suspend fun updateAndWaitNotNullState(
        allAvailableData: AvailableToAddData,
        rawCurrencyId: CryptoCurrency.RawID,
        networkIds: Set<String>? = null,
    ) {
        requiredDataFlow.tryEmit(
            RequiredData(
                allAvailableData = allAvailableData,
                rawCurrencyId = rawCurrencyId,
                networkIds = networkIds,
            ),
        )
        uiState.filterNotNull().firstOrNull()
    }

    private data class RequiredData(
        val allAvailableData: AvailableToAddData,
        val rawCurrencyId: CryptoCurrency.RawID,
        val networkIds: Set<String>?,
    )

    @AssistedFactory
    interface Factory {
        fun create(
            modelScope: CoroutineScope,
            onTokenSelected: (AddToPortfolioManager.Result) -> Unit,
        ): UserPortfolioStateController
    }
}