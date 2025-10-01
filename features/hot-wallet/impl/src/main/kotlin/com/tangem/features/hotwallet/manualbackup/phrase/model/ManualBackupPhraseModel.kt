package com.tangem.features.hotwallet.manualbackup.phrase.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.grid.entity.EnumeratedTwoColumnGridItem
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.hotwallet.manualbackup.phrase.ManualBackupPhraseComponent
import com.tangem.features.hotwallet.manualbackup.phrase.entity.ManualBackupPhraseUM
import com.tangem.hot.sdk.TangemHotSdk
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.UnlockHotWallet
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Stable
@ModelScoped
internal class ManualBackupPhraseModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val tangemHotSdk: TangemHotSdk,
) : Model() {

    private val params = paramsContainer.require<ManualBackupPhraseComponent.Params>()
    private val callbacks = params.callbacks

    internal val uiState: StateFlow<ManualBackupPhraseUM>
    field = MutableStateFlow(
        ManualBackupPhraseUM(
            onContinueClick = callbacks::onContinueClick,
        ),
    )

    init {
        modelScope.launch {
            runCatching {
                val userWallet = getUserWalletUseCase(params.userWalletId)
                    .getOrElse { error("User wallet with id ${params.userWalletId} not found") }
                if (userWallet is UserWallet.Hot) {
                    val unlockHotWallet = UnlockHotWallet(userWallet.hotWalletId, HotAuth.NoAuth)
                    val seedPhrasePrivateInfo = tangemHotSdk.exportMnemonic(unlockHotWallet)
                    uiState.update {
                        it.copy(
                            words = seedPhrasePrivateInfo.mnemonic.mnemonicComponents.mapIndexed { index, s ->
                                EnumeratedTwoColumnGridItem(index + 1, s)
                            }.toImmutableList(),
                        )
                    }
                }
            }.onFailure {
                Timber.e(it)
            }
        }
    }
}