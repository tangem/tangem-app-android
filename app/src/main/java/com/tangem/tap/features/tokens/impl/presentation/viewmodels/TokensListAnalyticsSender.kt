package com.tangem.tap.features.tokens.impl.presentation.viewmodels

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.ManageTokens

/** Analytics sender for tokens list screen */
class TokensListAnalyticsSender(private val analyticsEventHandler: AnalyticsEventHandler) {

    fun sendWhenScreenOpened() {
        analyticsEventHandler.send(event = ManageTokens.ScreenOpened())
    }

    fun sendWhenTokenAdded(token: Token) {
        analyticsEventHandler.send(
            event = ManageTokens.TokenSwitcherChanged(
                type = AnalyticsParam.CurrencyType.Token(token),
                state = AnalyticsParam.OnOffState.On,
            ),
        )
    }

    fun sendWhenBlockchainAdded(blockchain: Blockchain) {
        analyticsEventHandler.send(
            event = ManageTokens.TokenSwitcherChanged(
                type = AnalyticsParam.CurrencyType.Blockchain(blockchain),
                state = AnalyticsParam.OnOffState.On,
            ),
        )
    }

    fun sendWhenTokenRemoved(token: Token) {
        analyticsEventHandler.send(
            event = ManageTokens.TokenSwitcherChanged(
                type = AnalyticsParam.CurrencyType.Token(token),
                state = AnalyticsParam.OnOffState.Off,
            ),
        )
    }

    fun sendWhenBlockchainRemoved(blockchain: Blockchain) {
        analyticsEventHandler.send(
            event = ManageTokens.TokenSwitcherChanged(
                type = AnalyticsParam.CurrencyType.Blockchain(blockchain),
                state = AnalyticsParam.OnOffState.Off,
            ),
        )
    }

    fun sendWhenSaveButtonClicked() {
        analyticsEventHandler.send(ManageTokens.ButtonSaveChanges())
    }

    fun sendWhenTokenSearched() {
        analyticsEventHandler.send(ManageTokens.TokenSearched())
    }

    fun sendWhenAddCustomTokenClicked() {
        analyticsEventHandler.send(ManageTokens.ButtonCustomToken())
    }
}