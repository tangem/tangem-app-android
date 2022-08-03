package com.tangem.tap.features.details.ui.cardsettings

import android.content.Context
import com.tangem.domain.common.getTwinCardIdForUser
import com.tangem.domain.common.isTangemTwins
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.features.details.redux.CardSettingsState
import com.tangem.tap.features.details.redux.DetailsAction
import com.tangem.tap.features.details.ui.securitymode.toTitleRes
import com.tangem.wallet.R
import org.rekotlin.Store

class CardSettingsViewModel(private val store: Store<AppState>) {

    fun updateState(state: CardSettingsState?, context: Context?): CardSettingsScreenState {

        return if (state?.manageSecurityState == null) {
            CardSettingsScreenState(
                cardDetails = null,
                onElementClick = {},
                onScanCardClick = {
                    store.dispatch(DetailsAction.ScanCard)
                },
            )
        } else {
            val cardId = if (state.card.isTangemTwins()) {
                state.card.getTwinCardIdForUser()
            } else {
                state.cardInfo.cardId
            }
            val cardDetails: MutableList<CardInfo> = listOfNotNull(
                CardInfo.CardId(cardId),
                CardInfo.Issuer(state.cardInfo.issuer),
            ).toMutableList()

            context?.let { context ->
                if (!state.card.isTangemTwins()) {
                    cardDetails.add(
                        CardInfo.SignedHashes(
                            context
                                .getString(
                                    R.string.details_row_subtitle_signed_hashes_format,
                                    state.cardInfo.signedHashes.toString(),
                                ),
                        ),
                    )
                }
                cardDetails.add(
                    CardInfo.SecurityMode(
                        subtitle = context.getString(state.manageSecurityState.currentOption.toTitleRes()),
                        clickable = state.manageSecurityState.allowedOptions.size > 1,
                    ),
                )
                if (state.card.backupStatus?.isActive == true && state.card.isAccessCodeSet) {
                    cardDetails.add(
                        CardInfo.ChangeAccessCode(
                            subtitle = context.getString(R.string.card_settings_change_access_code_footer),
                        ),
                    )
                }
                if (state.resetCardAllowed) {
                    cardDetails.add(
                        CardInfo.ResetToFactorySettings(
                            subtitle = context.getString(R.string.card_settings_reset_factory_footer),
                        ),
                    )
                }
            }

            CardSettingsScreenState(
                cardDetails = cardDetails,
                onScanCardClick = { },
                onElementClick = {
                    handleClickingItem(it)
                },
            )
        }
    }

    private fun handleClickingItem(item: CardInfo) {
        when (item) {
            is CardInfo.ChangeAccessCode -> {
                store.dispatch(DetailsAction.ManageSecurity.ChangeAccessCode)
            }
            is CardInfo.ResetToFactorySettings -> {
                store.dispatch(NavigationAction.NavigateTo(AppScreen.ResetToFactory))
            }
            is CardInfo.SecurityMode -> {
                store.dispatch(DetailsAction.ManageSecurity.OpenSecurity)
            }
            else -> {}
        }
    }
}