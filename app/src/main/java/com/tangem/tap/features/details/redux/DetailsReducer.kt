package com.tangem.tap.features.details.redux

import com.tangem.core.navigation.AppScreen
import com.tangem.domain.apptheme.model.AppThemeMode
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.extensions.signedHashesCount
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.rekotlin.Action
import java.util.EnumSet

object DetailsReducer {
    fun reduce(action: Action, state: AppState): DetailsState = internalReduce(action, state)
}

@Suppress("CyclomaticComplexMethod")
private fun internalReduce(action: Action, state: AppState): DetailsState {
    if (action !is DetailsAction) return state.detailsState
    val detailsState = state.detailsState
    return when (action) {
        is DetailsAction.PrepareScreen -> {
            handlePrepareScreen(action, state)
        }
        is DetailsAction.PrepareCardSettingsData -> {
            handlePrepareCardSettingsScreen(scanResponse = action.scanResponse, state = detailsState)
        }
        is DetailsAction.ResetCardSettingsData -> detailsState.copy(cardSettingsState = null)
        is DetailsAction.ResetToFactory -> {
            handleEraseWallet(action, detailsState)
        }
        is DetailsAction.ManageSecurity -> {
            handleSecurityAction(action, detailsState)
        }
        is DetailsAction.AppSettings -> {
            handlePrivacyAction(action, detailsState)
        }
        is DetailsAction.ChangeAppCurrency -> detailsState.copy(
            appSettingsState = detailsState.appSettingsState.copy(
                selectedAppCurrency = action.currency,
            ),
        )
        is DetailsAction.AccessCodeRecovery -> handleAccessCodeRecoveryAction(action, detailsState)
        is DetailsAction.ScanAndSaveUserWallet -> detailsState.copy(
            isScanningInProgress = true,
        )
        is DetailsAction.ScanAndSaveUserWallet.Error -> detailsState.copy(
            isScanningInProgress = false,
            error = action.error,
        )
        is DetailsAction.ScanAndSaveUserWallet.Success -> detailsState.copy(
            isScanningInProgress = false,
        )
        is DetailsAction.DismissError -> detailsState.copy(
            error = null,
        )
        else -> detailsState
    }
}

private fun handlePrepareScreen(action: DetailsAction.PrepareScreen, state: AppState): DetailsState {
    return DetailsState(
        scanResponse = action.scanResponse,
        // If the current screen is ResetToFactory, we must save the ResetToFactory's state
        cardSettingsState = if (store.state.navigationState.backStack.lastOrNull() == AppScreen.ResetToFactory) {
            state.detailsState.cardSettingsState
        } else {
            null
        },
        createBackupAllowed = action.scanResponse.card.backupStatus == CardDTO.BackupStatus.NoBackup,
        appSettingsState = AppSettingsState(
            isBiometricsAvailable = runBlocking {
                tangemSdkManager.checkCanUseBiometry()
            },
            saveWallets = action.shouldSaveUserWallets,
            saveAccessCodes = runBlocking {
                store.inject(DaggerGraphState::settingsRepository).shouldSaveAccessCodes()
            },
            selectedAppCurrency = store.state.globalState.appCurrency,
            selectedThemeMode = runBlocking {
                store.inject(DaggerGraphState::appThemeModeRepository).getAppThemeMode().firstOrNull()
                    ?: AppThemeMode.DEFAULT
            },
            isHidingEnabled = runBlocking {
                store.inject(DaggerGraphState::balanceHidingRepository)
                    .getBalanceHidingSettings().isHidingEnabledInSettings
            },
        ),
    )
}

private fun handlePrepareCardSettingsScreen(scanResponse: ScanResponse, state: DetailsState): DetailsState {
    val cardTypesResolver = scanResponse.cardTypesResolver
    val card = scanResponse.card
    val isTangemWallet = cardTypesResolver.isTangemWallet() || cardTypesResolver.isWallet2()
    val isShowPasswordResetRadioButton = isTangemWallet && card.backupStatus is CardDTO.BackupStatus.Active

    val cardSettingsState = CardSettingsState(
        cardInfo = card.toCardInfo(cardTypesResolver),
        scanResponse = scanResponse,
        manageSecurityState = prepareSecurityOptions(card, cardTypesResolver),
        card = scanResponse.card,
        resetCardAllowed = isResetToFactoryAllowedByCard(card, cardTypesResolver),
        resetButtonEnabled = false,
        condition1Checked = false,
        condition2Checked = false,
        accessCodeRecovery = if (cardTypesResolver.isWallet2()) {
            val enabled = card.userSettings?.isUserCodeRecoveryAllowed ?: false
            AccessCodeRecoveryState(
                enabledOnCard = enabled,
                enabledSelection = enabled,
            )
        } else {
            null
        },
        isShowPasswordResetRadioButton = isShowPasswordResetRadioButton,
        dialog = null,
    )

    return state.copy(cardSettingsState = cardSettingsState)
}

private fun prepareSecurityOptions(card: CardDTO, cardTypesResolver: CardTypesResolver): ManageSecurityState {
    val securityOption = when {
        card.isAccessCodeSet -> {
            SecurityOption.AccessCode
        }

        card.isPasscodeSet == true -> {
            SecurityOption.PassCode
        }

        else -> {
            SecurityOption.LongTap
        }
    }
    val allowedSecurityOptions = when {
        cardTypesResolver.isStart2Coin() || cardTypesResolver.isTangemNote() -> {
            EnumSet.of(SecurityOption.LongTap)
        }
        card.settings.isBackupAllowed -> {
            EnumSet.of(securityOption)
        }
        else ->
            prepareAllowedSecurityOptions(cardTypesResolver = cardTypesResolver, currentSecurityOption = securityOption)
    }
    return ManageSecurityState(
        currentOption = securityOption,
        allowedOptions = allowedSecurityOptions,
        selectedOption = securityOption,
    )
}

private fun isResetToFactoryAllowedByCard(card: CardDTO, cardTypesResolver: CardTypesResolver): Boolean {
    val hasPermanentWallet = card.wallets.any { it.settings.isPermanent }
    val isNotAllowed = hasPermanentWallet || cardTypesResolver.isStart2Coin()
    return !isNotAllowed
}

private fun handleEraseWallet(action: DetailsAction.ResetToFactory, state: DetailsState): DetailsState {
    val cardSettingsState = state.cardSettingsState
    return when (action) {
        is DetailsAction.ResetToFactory.AcceptCondition1 -> {
            val warning1Checked = action.accepted
            val resetButtonEnabled = if (cardSettingsState?.isShowPasswordResetRadioButton == true) {
                warning1Checked && cardSettingsState.condition2Checked
            } else {
                warning1Checked
            }
            state.copy(
                cardSettingsState = cardSettingsState?.copy(
                    condition1Checked = action.accepted,
                    resetButtonEnabled = resetButtonEnabled,
                ),
            )
        }
        is DetailsAction.ResetToFactory.AcceptCondition2 -> {
            val warning2Checked = action.accepted
            state.copy(
                cardSettingsState = cardSettingsState?.copy(
                    condition2Checked = action.accepted,
                    resetButtonEnabled = warning2Checked && cardSettingsState.condition1Checked,
                ),
            )
        }
        is DetailsAction.ResetToFactory.ShowDialog -> {
            state.copy(cardSettingsState = cardSettingsState?.copy(dialog = action.dialog))
        }
        is DetailsAction.ResetToFactory.DismissDialog -> {
            state.copy(cardSettingsState = cardSettingsState?.copy(dialog = null))
        }
        else -> state
    }
}

private fun handleSecurityAction(action: DetailsAction.ManageSecurity, state: DetailsState): DetailsState {
    return when (action) {
        is DetailsAction.ManageSecurity.SelectOption -> {
            val manageSecurityState = state.cardSettingsState?.manageSecurityState?.copy(
                selectedOption = action.option,
            )
            state.copy(cardSettingsState = state.cardSettingsState?.copy(manageSecurityState = manageSecurityState))
        }
        is DetailsAction.ManageSecurity.SaveChanges.Success -> {
            // Setting options to show only LongTap from now on for non-twins
            val manageSecurityState = state.cardSettingsState?.manageSecurityState?.copy(
                currentOption = state.cardSettingsState.manageSecurityState.selectedOption,
                allowedOptions = state.scanResponse?.cardTypesResolver?.let {
                    prepareAllowedSecurityOptions(
                        cardTypesResolver = it,
                        currentSecurityOption = state.cardSettingsState.manageSecurityState.selectedOption,
                    )
                } ?: EnumSet.of(SecurityOption.LongTap),
            )
            state.copy(
                cardSettingsState = state.cardSettingsState?.copy(
                    manageSecurityState = manageSecurityState,
                ),
            )
        }
        else -> state
    }
}

private fun handlePrivacyAction(action: DetailsAction.AppSettings, state: DetailsState): DetailsState {
    return when (action) {
        is DetailsAction.AppSettings.SwitchPrivacySetting -> state.copy(
            appSettingsState = when (action.setting) {
                AppSetting.SaveWallets -> state.appSettingsState.copy(
                    isInProgress = true,
                    saveWallets = action.enable,
                )
                AppSetting.SaveAccessCode -> state.appSettingsState.copy(
                    isInProgress = true,
                    saveWallets = true, // User can't enable access codes saving without wallets saving
                    saveAccessCodes = action.enable,
                )
            },
        )
        is DetailsAction.AppSettings.SwitchPrivacySetting.Success -> state.copy(
            appSettingsState = state.appSettingsState.copy(
                isInProgress = false,
            ),
        )
        is DetailsAction.AppSettings.SwitchPrivacySetting.Failure -> state.copy(
            appSettingsState = when (action.setting) {
                AppSetting.SaveWallets -> state.appSettingsState.copy(
                    isInProgress = false,
                    saveWallets = action.prevState,
                )
                AppSetting.SaveAccessCode -> state.appSettingsState.copy(
                    isInProgress = false,
                    saveAccessCodes = action.prevState,
                )
            },
        )
        is DetailsAction.AppSettings.BiometricsStatusChanged -> state.copy(
            appSettingsState = state.appSettingsState.copy(
                needEnrollBiometrics = action.needEnrollBiometrics,
            ),
        )
        is DetailsAction.AppSettings.ChangeAppThemeMode -> state.copy(
            appSettingsState = state.appSettingsState.copy(
                selectedThemeMode = action.appThemeMode,
            ),
        )
        is DetailsAction.AppSettings.ChangeAppCurrency -> state.copy(
            appSettingsState = state.appSettingsState.copy(
                selectedAppCurrency = action.currency,
            ),
        )
        is DetailsAction.AppSettings.ChangeBalanceHiding -> state.copy(
            appSettingsState = state.appSettingsState.copy(
                isHidingEnabled = action.hideBalance,
            ),
        )
        is DetailsAction.AppSettings.EnrollBiometrics,
        is DetailsAction.AppSettings.CheckBiometricsStatus,
        -> state
    }
}

private fun handleAccessCodeRecoveryAction(
    action: DetailsAction.AccessCodeRecovery,
    state: DetailsState,
): DetailsState {
    return when (action) {
        DetailsAction.AccessCodeRecovery.Open -> {
            val accessCodeRecovery = state.cardSettingsState?.accessCodeRecovery?.copy(
                enabledSelection = state.cardSettingsState.accessCodeRecovery.enabledOnCard,
            )
            state.copy(cardSettingsState = state.cardSettingsState?.copy(accessCodeRecovery = accessCodeRecovery))
        }
        is DetailsAction.AccessCodeRecovery.SaveChanges -> state
        is DetailsAction.AccessCodeRecovery.SelectOption -> {
            val accessCodeRecovery = state.cardSettingsState?.accessCodeRecovery?.copy(
                enabledSelection = action.enabled,
            )
            state.copy(cardSettingsState = state.cardSettingsState?.copy(accessCodeRecovery = accessCodeRecovery))
        }
        is DetailsAction.AccessCodeRecovery.SaveChanges.Success -> {
            val accessCodeRecovery = state.cardSettingsState?.accessCodeRecovery?.copy(
                enabledOnCard = action.enabled,
                enabledSelection = action.enabled,
            )
            state.copy(cardSettingsState = state.cardSettingsState?.copy(accessCodeRecovery = accessCodeRecovery))
        }
    }
}

private fun prepareAllowedSecurityOptions(
    cardTypesResolver: CardTypesResolver,
    currentSecurityOption: SecurityOption?,
): EnumSet<SecurityOption> {
    val allowedSecurityOptions = EnumSet.of(SecurityOption.LongTap)

    if (cardTypesResolver.isTangemTwins()) {
        allowedSecurityOptions.add(SecurityOption.PassCode)
    }
    if (currentSecurityOption == SecurityOption.AccessCode) {
        allowedSecurityOptions.add(SecurityOption.AccessCode)
    }
    if (currentSecurityOption == SecurityOption.PassCode) {
        allowedSecurityOptions.add(SecurityOption.PassCode)
    }
    return allowedSecurityOptions
}

@Suppress("MagicNumber")
private fun CardDTO.toCardInfo(cardTypesResolver: CardTypesResolver): CardInfo {
    val cardId = this.cardId.chunked(4).joinToString(separator = " ")
    val issuer = this.issuer.name
    val signedHashes = this.signedHashesCount()

    return CardInfo(
        cardId = cardId,
        issuer = issuer,
        signedHashes = signedHashes,
        isTwin = cardTypesResolver.isTangemTwins(),
        hasBackup = backupStatus?.isActive == true,
    )
}
