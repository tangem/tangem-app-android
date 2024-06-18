package com.tangem.tap.proxy.redux

import com.tangem.core.navigation.email.EmailSender
import com.tangem.domain.card.ScanCardUseCase
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.feature.qrscanning.QrScanningRouter
import com.tangem.features.managetokens.navigation.ManageTokensUi
import com.tangem.features.send.api.navigation.SendRouter
import com.tangem.features.tester.api.TesterRouter
import com.tangem.features.tokendetails.navigation.TokenDetailsRouter
import com.tangem.features.wallet.navigation.WalletRouter
import com.tangem.tap.domain.walletconnect2.domain.WalletConnectInteractor
import org.rekotlin.Action

sealed interface DaggerGraphAction : Action {

    data class SetActivityDependencies(
        val testerRouter: TesterRouter,
        val scanCardUseCase: ScanCardUseCase,
        val walletRouter: WalletRouter,
        val walletConnectInteractor: WalletConnectInteractor,
        val tokenDetailsRouter: TokenDetailsRouter,
        val manageTokensUi: ManageTokensUi,
        val cardSdkConfigRepository: CardSdkConfigRepository,
        val sendRouter: SendRouter,
        val qrScanningRouter: QrScanningRouter,
        val emailSender: EmailSender,
    ) : DaggerGraphAction
}