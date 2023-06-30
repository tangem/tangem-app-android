package com.tangem.tap.features.details.redux

import com.tangem.domain.card.CardTypeResolver
import com.tangem.domain.models.scan.CardDTO
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.domain.extensions.signedHashesCount
import com.tangem.tap.preferencesStorage
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import org.rekotlin.Action
import java.util.EnumSet

object DetailsReducer {
    fun reduce(action: Action, state: AppState): DetailsState = internalReduce(action, state)
}

private fun internalReduce(action: Action, state: AppState): DetailsState {
    if (action !is DetailsAction) return state.detailsState
    val detailsState = state.detailsState
    return when (action) {
        is DetailsAction.PrepareScreen -> {
            handlePrepareScreen(action)
        }
        is DetailsAction.PrepareCardSettingsData -> {
            handlePrepareCardSettingsScreen(card = action.card, state = detailsState)
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
        is DetailsAction.ChangeAppCurrency ->
            detailsState.copy(appCurrency = action.fiatCurrency)
        is DetailsAction.AccessCodeRecovery -> handleAccessCodeRecoveryAction(action, detailsState)
        else -> detailsState
    }
}

private fun handlePrepareScreen(action: DetailsAction.PrepareScreen): DetailsState {
    return DetailsState(
        scanResponse = action.scanResponse,
        wallets = action.wallets,
        createBackupAllowed = action.scanResponse.card.backupStatus == CardDTO.BackupStatus.NoBackup,
        appCurrency = store.state.globalState.appCurrency,
        appSettingsState = AppSettingsState(
            isBiometricsAvailable = tangemSdkManager.canUseBiometry,
            saveWallets = preferencesStorage.shouldSaveUserWallets,
            saveAccessCodes = preferencesStorage.shouldSaveAccessCodes,
        ),
    )
}

private fun handlePrepareCardSettingsScreen(card: CardDTO, state: DetailsState): DetailsState {
    val cardTypeResolver = store.state.daggerGraphState.get(DaggerGraphState::cardTypeResolver)
    val cardSettingsState = CardSettingsState(
        cardInfo = card.toCardInfo(cardTypeResolver),
        manageSecurityState = prepareSecurityOptions(card, cardTypeResolver),
        card = card,
        resetCardAllowed = isResetToFactoryAllowedByCard(card, cardTypeResolver),
        accessCodeRecovery = if (cardTypeResolver.isWallet2()) {
            val enabled = card.userSettings?.isUserCodeRecoveryAllowed ?: false
            AccessCodeRecoveryState(
                enabledOnCard = enabled,
                enabledSelection = enabled,
            )
        } else {
            null
        },
    )
    return state.copy(cardSettingsState = cardSettingsState)
}

private fun prepareSecurityOptions(card: CardDTO, cardTypeResolver: CardTypeResolver): ManageSecurityState {
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
        cardTypeResolver.isStart2Coin() || cardTypeResolver.isTangemNote() -> {
            EnumSet.of(SecurityOption.LongTap)
        }
        card.settings.isBackupAllowed -> {
            EnumSet.of(securityOption)
        }
        else -> {
            prepareAllowedSecurityOptions(cardTypeResolver = cardTypeResolver, currentSecurityOption = securityOption)
        }
    }
    return ManageSecurityState(
        currentOption = securityOption,
        allowedOptions = allowedSecurityOptions,
        selectedOption = securityOption,
    )
}

private fun isResetToFactoryAllowedByCard(card: CardDTO, cardTypeResolver: CardTypeResolver): Boolean {
    val hasPermanentWallet = card.wallets.any { it.settings.isPermanent }
    val isNotAllowed = hasPermanentWallet || cardTypeResolver.isStart2Coin()
    return !isNotAllowed
}

private fun handleEraseWallet(action: DetailsAction.ResetToFactory, state: DetailsState): DetailsState {
    return when (action) {
        is DetailsAction.ResetToFactory.Confirm ->
            state.copy(cardSettingsState = state.cardSettingsState?.copy(resetConfirmed = action.confirmed))
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
                allowedOptions = prepareAllowedSecurityOptions(
                    cardTypeResolver = store.state.daggerGraphState.get(DaggerGraphState::cardTypeResolver),
                    currentSecurityOption = state.cardSettingsState.manageSecurityState.selectedOption,
                ),
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
    cardTypeResolver: CardTypeResolver,
    currentSecurityOption: SecurityOption?,
): EnumSet<SecurityOption> {
    val allowedSecurityOptions = EnumSet.of(SecurityOption.LongTap)

    if (cardTypeResolver.isTangemTwins()) {
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

private fun CardDTO.toCardInfo(cardTypeResolver: CardTypeResolver): CardInfo {
    val cardId = this.cardId.chunked(size = 4).joinToString(separator = " ")
    val issuer = this.issuer.name
    val signedHashes = this.signedHashesCount()

    return CardInfo(
        cardId = cardId,
        issuer = issuer,
        signedHashes = signedHashes,
        isTwin = cardTypeResolver.isTangemTwins(),
        hasBackup = backupStatus?.isActive == true,
    )
}