package com.tangem.features.hotwallet.manualbackup.phrase.model

import androidx.compose.runtime.Stable
import arrow.core.Either
import arrow.core.flatMap
import arrow.core.getOrElse
import arrow.core.right
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.components.grid.entity.EnumeratedTwoColumnGridItem
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.usecase.ExportSeedPhraseUseCase
import com.tangem.domain.wallets.usecase.GetHotWalletContextualUnlockUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.UnlockHotWalletContextualUseCase
import com.tangem.features.hotwallet.manualbackup.phrase.ManualBackupPhraseComponent
import com.tangem.features.hotwallet.manualbackup.phrase.entity.ManualBackupPhraseUM
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class ManualBackupPhraseModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val exportSeedPhraseUseCase: ExportSeedPhraseUseCase,
    private val getHotWalletContextualUnlockUseCase: GetHotWalletContextualUnlockUseCase,
    private val unlockHotWalletContextualUseCase: UnlockHotWalletContextualUseCase,
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
            val userWallet = getUserWalletUseCase(params.userWalletId).getOrElse { null }
            if (userWallet !is UserWallet.Hot) {
                TangemLogger.e("Hot wallet ${params.userWalletId} not found for a manual backup")
                return@launch
            }

            ensureWalletUnlocked(userWallet.hotWalletId)
                .flatMap { exportSeedPhraseUseCase.invoke(userWallet.hotWalletId) }
                .fold(
                    ifLeft = { TangemLogger.e("Error on exporting the seed phrase for a manual backup", it) },
                    ifRight = { seedPhrasePrivateInfo ->
                        uiState.update {
                            it.copy(
                                words = seedPhrasePrivateInfo.mnemonic.mnemonicComponents.mapIndexed { index, s ->
                                    EnumeratedTwoColumnGridItem(index + 1, s)
                                }.toImmutableList(),
                            )
                        }
                    },
                )
        }
    }

    /**
     * Obtains a contextual unlock unless one is already held, so the export works for wallets protected
     * with an access code or biometry (e.g. after a cloud backup) and the confirmation step that follows
     * reuses the same unlock without prompting again. The unlock is cleared by the flow owner.
     */
    private suspend fun ensureWalletUnlocked(hotWalletId: HotWalletId): Either<Throwable, Unit> =
        getHotWalletContextualUnlockUseCase(hotWalletId).flatMap { heldUnlock ->
            if (heldUnlock == null) {
                unlockHotWalletContextualUseCase(hotWalletId).map { }
            } else {
                Unit.right()
            }
        }
}