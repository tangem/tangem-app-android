package com.tangem.feature.wallet.presentation.wallet.viewmodels.intents

import com.tangem.blockchain.blockchains.cardano.CardanoUtils
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.toMapKey
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.card.DerivePublicKeysUseCase
import com.tangem.domain.card.SetCardWasScannedUseCase
import com.tangem.domain.common.configs.CardConfig
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.redux.LegacyAction
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.settings.NeverToSuggestRateAppUseCase
import com.tangem.domain.settings.RemindToRateAppLaterUseCase
import com.tangem.domain.tokens.FetchTokenListUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UnlockWalletsError
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.domain.wallets.usecase.UnlockWalletsUseCase
import com.tangem.domain.wallets.usecase.UpdateWalletUseCase
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent.Basic
import com.tangem.feature.wallet.presentation.wallet.analytics.WalletScreenAnalyticsEvent.MainScreen
import com.tangem.feature.wallet.presentation.wallet.domain.ScanCardToUnlockWalletClickHandler
import com.tangem.feature.wallet.presentation.wallet.domain.ScanCardToUnlockWalletError
import com.tangem.feature.wallet.presentation.wallet.domain.unwrap
import com.tangem.feature.wallet.presentation.wallet.state.WalletAlertState
import com.tangem.feature.wallet.presentation.wallet.state.WalletEvent
import com.tangem.feature.wallet.presentation.wallet.state2.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state2.transformers.CloseBottomSheetTransformer
import com.tangem.feature.wallet.presentation.wallet.state2.utils.WalletEventSender
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.launch
import javax.inject.Inject

internal interface WalletWarningsClickIntents {

    fun onAddBackupCardClick()

    fun onCloseAlreadySignedHashesWarningClick()

    fun onGenerateMissedAddressesClick(missedAddressCurrencies: List<CryptoCurrency>)

    fun onOpenUnlockWalletsBottomSheetClick()

    fun onUnlockWalletClick()

    fun onScanToUnlockWalletClick()

    fun onLikeAppClick()

    fun onDislikeAppClick()

    fun onCloseRateAppWarningClick()
}

@Suppress("LongParameterList")
@ViewModelScoped
internal class WalletWarningsClickIntentsImplementer @Inject constructor(
    private val stateHolder: WalletStateController,
    private val walletEventSender: WalletEventSender,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val updateWalletUseCase: UpdateWalletUseCase,
    private val unlockWalletsUseCase: UnlockWalletsUseCase,
    private val derivePublicKeysUseCase: DerivePublicKeysUseCase,
    private val scanCardToUnlockWalletClickHandler: ScanCardToUnlockWalletClickHandler,
    private val fetchTokenListUseCase: FetchTokenListUseCase,
    private val setCardWasScannedUseCase: SetCardWasScannedUseCase,
    private val neverToSuggestRateAppUseCase: NeverToSuggestRateAppUseCase,
    private val remindToRateAppLaterUseCase: RemindToRateAppLaterUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val reduxStateHolder: ReduxStateHolder,
    private val dispatchers: CoroutineDispatcherProvider,
) : BaseWalletClickIntents(), WalletWarningsClickIntents {

    override fun onAddBackupCardClick() {
        analyticsEventHandler.send(MainScreen.NoticeBackupYourWalletTapped)

        prepareOnboardingProcess()
        router.openOnboardingScreen()
    }

    private fun prepareOnboardingProcess() {
        getSelectedWalletSyncUseCase.unwrap()?.let {
            reduxStateHolder.dispatch(
                LegacyAction.StartOnboardingProcess(
                    scanResponse = it.scanResponse,
                    canSkipBackup = false,
                ),
            )
        }
    }

    override fun onCloseAlreadySignedHashesWarningClick() {
        val userWallet = getSelectedWalletSyncUseCase.unwrap() ?: return

        viewModelScope.launch(dispatchers.main) {
            setCardWasScannedUseCase(cardId = userWallet.cardId)
        }
    }

    @Deprecated("Use DerivePublicKeysUseCase instead")
    // FIXME: Migration: [REDACTED_JIRA]
    override fun onGenerateMissedAddressesClick(missedAddressCurrencies: List<CryptoCurrency>) {
        val userWallet = getSelectedWalletSyncUseCase.unwrap() ?: return

        analyticsEventHandler.send(Basic.CardWasScanned(AnalyticsParam.ScannedFrom.Main))
        analyticsEventHandler.send(MainScreen.NoticeScanYourCardTapped)

        viewModelScope.launch(dispatchers.main) {
            deriveMissingCurrencies(
                scanResponse = userWallet.scanResponse,
                currencyList = missedAddressCurrencies,
            ) { scannedCardResponse ->
                updateWalletUseCase(
                    userWalletId = userWallet.walletId,
                    update = { it.copy(scanResponse = scannedCardResponse) },
                )
                    .onRight { fetchTokenListUseCase(userWalletId = it.walletId) }
            }
        }
    }

    private fun deriveMissingCurrencies(
        scanResponse: ScanResponse,
        currencyList: List<CryptoCurrency>,
        onSuccess: suspend (ScanResponse) -> Unit,
    ) {
        val config = CardConfig.createConfig(scanResponse.card)
        val derivationDataList = currencyList.mapNotNull {
            config.primaryCurve(blockchain = Blockchain.fromId(it.network.id.value))?.let { curve ->
                getNewDerivations(curve, scanResponse, it)
            }
        }

        val derivations = buildMap<ByteArrayKey, MutableList<DerivationPath>> {
            derivationDataList.forEach {
                val current = this[it.derivations.first]
                if (current != null) {
                    current.addAll(it.derivations.second)
                    current.distinct()
                } else {
                    this[it.derivations.first] = it.derivations.second.toMutableList()
                }
            }
        }.ifEmpty { return }

        viewModelScope.launch(dispatchers.io) {
            derivePublicKeysUseCase(cardId = null, derivations = derivations)
                .onRight {
                    val newDerivedKeys = it.entries
                    val oldDerivedKeys = scanResponse.derivedKeys

                    val walletKeys = (newDerivedKeys.keys + oldDerivedKeys.keys).toSet()

                    val updatedDerivedKeys = walletKeys.associateWith { walletKey ->
                        val oldDerivations = ExtendedPublicKeysMap(oldDerivedKeys[walletKey] ?: emptyMap())
                        val newDerivations = newDerivedKeys[walletKey] ?: ExtendedPublicKeysMap(emptyMap())
                        ExtendedPublicKeysMap(oldDerivations + newDerivations)
                    }
                    val updatedScanResponse = scanResponse.copy(derivedKeys = updatedDerivedKeys)

                    onSuccess(updatedScanResponse)
                }
        }
    }

    private fun getNewDerivations(
        curve: EllipticCurve,
        scanResponse: ScanResponse,
        currency: CryptoCurrency,
    ): DerivationData? {
        val wallet = scanResponse.card.wallets.firstOrNull { it.curve == curve } ?: return null

        val blockchain = Blockchain.fromId(currency.network.id.value)
        val supportedCurves = blockchain.getSupportedCurves()
        val path = blockchain.derivationPath(scanResponse.derivationStyleProvider.getDerivationStyle())
            .takeIf { supportedCurves.contains(curve) }

        val customPath = currency.network.derivationPath.value?.let {
            DerivationPath(it)
        }.takeIf { supportedCurves.contains(curve) }

        val bothCandidates = listOfNotNull(path, customPath).distinct().toMutableList()
        if (bothCandidates.isEmpty()) return null

        if (currency is CryptoCurrency.Coin && blockchain == Blockchain.Cardano) {
            currency.network.derivationPath.value?.let {
                bothCandidates.add(CardanoUtils.extendedDerivationPath(DerivationPath(it)))
            }
        }

        val mapKeyOfWalletPublicKey = wallet.publicKey.toMapKey()
        val alreadyDerivedKeys: ExtendedPublicKeysMap =
            scanResponse.derivedKeys[mapKeyOfWalletPublicKey] ?: ExtendedPublicKeysMap(emptyMap())
        val alreadyDerivedPaths = alreadyDerivedKeys.keys.toList()

        val toDerive = bothCandidates.filterNot { alreadyDerivedPaths.contains(it) }
        if (toDerive.isEmpty()) return null

        return DerivationData(derivations = mapKeyOfWalletPublicKey to toDerive)
    }

    class DerivationData(val derivations: Pair<ByteArrayKey, List<DerivationPath>>)

    override fun onOpenUnlockWalletsBottomSheetClick() {
        val config = requireNotNull(stateHolder.getSelectedWallet().bottomSheetConfig) {
            "Impossible to open unlock wallet bottom sheet if it's null"
        }

        stateHolder.showBottomSheet(config.content)
    }

    override fun onUnlockWalletClick() {
        analyticsEventHandler.send(MainScreen.NoticeWalletLocked)

        viewModelScope.launch(dispatchers.main) {
            unlockWalletsUseCase(throwIfNotAllWalletsUnlocked = true)
                .onRight { stateHolder.update(CloseBottomSheetTransformer(stateHolder.getSelectedWalletId())) }
                .onLeft(::handleUnlockWalletsError)
        }
    }

    private fun handleUnlockWalletsError(error: UnlockWalletsError) {
        val event = when (error) {
            is UnlockWalletsError.DataError,
            is UnlockWalletsError.UnableToUnlockWallets,
            -> WalletEvent.ShowToast(resourceReference(R.string.user_wallet_list_error_unable_to_unlock))
            is UnlockWalletsError.NoUserWalletSelected,
            is UnlockWalletsError.NotAllUserWalletsUnlocked,
            -> WalletEvent.ShowAlert(WalletAlertState.RescanWallets)
        }

        walletEventSender.send(event)
    }

    override fun onScanToUnlockWalletClick() {
        analyticsEventHandler.send(event = MainScreen.WalletUnlockTapped)

        viewModelScope.launch(dispatchers.main) {
            scanCardToUnlockWalletClickHandler(walletId = stateHolder.getSelectedWalletId())
                .onLeft { error ->
                    when (error) {
                        ScanCardToUnlockWalletError.WrongCardIsScanned -> {
                            walletEventSender.send(
                                event = WalletEvent.ShowAlert(WalletAlertState.WrongCardIsScanned),
                            )
                        }
                        ScanCardToUnlockWalletError.ManyScanFails -> router.openScanFailedDialog()
                    }
                }
        }
    }

    override fun onLikeAppClick() {
        analyticsEventHandler.send(MainScreen.NoticeRateAppButton(AnalyticsParam.RateApp.Liked))

        walletEventSender.send(
            event = WalletEvent.RateApp(
                onDismissClick = {
                    viewModelScope.launch(dispatchers.main) {
                        neverToSuggestRateAppUseCase()
                    }
                },
            ),
        )
    }

    override fun onDislikeAppClick() {
        analyticsEventHandler.send(MainScreen.NoticeRateAppButton(AnalyticsParam.RateApp.Disliked))

        viewModelScope.launch(dispatchers.main) {
            neverToSuggestRateAppUseCase()

            reduxStateHolder.dispatch(LegacyAction.SendEmailRateCanBeBetter)
        }
    }

    override fun onCloseRateAppWarningClick() {
        analyticsEventHandler.send(MainScreen.NoticeRateAppButton(AnalyticsParam.RateApp.Closed))

        viewModelScope.launch(dispatchers.main) {
            remindToRateAppLaterUseCase()
        }
    }
}