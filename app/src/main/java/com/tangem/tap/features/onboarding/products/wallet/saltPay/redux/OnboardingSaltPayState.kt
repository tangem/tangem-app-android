package com.tangem.tap.features.onboarding.products.wallet.saltPay.redux

import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.WalletManagerFactory
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.toHexString
import com.tangem.common.json.MoshiJsonConverter
import com.tangem.domain.common.SaltPayWorkaround
import com.tangem.domain.common.ScanResponse
import com.tangem.network.api.paymentology.PaymentologyApiService
import com.tangem.tap.common.toggleWidget.WidgetState
import com.tangem.tap.copyToClipboard
import com.tangem.tap.domain.extensions.makeSaltPayWalletManager
import com.tangem.tap.features.onboarding.products.wallet.saltPay.GnosisRegistrator
import com.tangem.tap.features.onboarding.products.wallet.saltPay.KYCProvider
import com.tangem.tap.features.onboarding.products.wallet.saltPay.SaltPayActivationManager
import com.tangem.tap.features.onboarding.products.wallet.saltPay.SaltPayConfig
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.store
import java.math.BigDecimal

/**
[REDACTED_AUTHOR]
 */
data class OnboardingSaltPayState(
    @Transient
    val saltPayManager: SaltPayActivationManager = SaltPayActivationManager.stub(),
    val saltPayConfig: SaltPayConfig = SaltPayConfig.stub(),
    val pinCode: String? = null,
    val accessCode: String? = null,
    val amountToClaim: Amount? = null,
    val tokenAmount: Amount = Amount(SaltPayWorkaround.tokenFrom(Blockchain.SaltPay), BigDecimal.ZERO),
    val step: SaltPayActivationStep = SaltPayActivationStep.None,
    val saltPayCardArtworkUrl: String? = null,
    val inProgress: Boolean = false,
    val claimInProgress: Boolean = false,
) {

    val mainButtonState: WidgetState
        get() = if (inProgress) ProgressState.Loading else ProgressState.Done

    fun readyToClaim(): Boolean = amountToClaim != null

    val pinLength: Int = 4

    companion object {
        fun initDependency(scanResponse: ScanResponse): Pair<SaltPayActivationManager, SaltPayConfig> {
            val globalState = store.state.globalState
            val saltPayConfig = globalState.configManager?.config?.saltPayConfig.guard {
                throw NullPointerException("SaltPayConfig is not initialized")
            }
            val gnosisRegistrator = makeGnosisRegistrator(
                scanResponse = scanResponse,
                wmFactory = globalState.tapWalletManager.walletManagerFactory,
            )
            val registrationManager = makeSaltPayRegistrationManager(
                scanResponse = scanResponse,
                gnosisRegistrator = gnosisRegistrator,
                paymentologyService = store.state.domainNetworks.paymentologyService,
                kycProvider = saltPayConfig.kycProvider,
            )
            test(scanResponse, gnosisRegistrator)
            return registrationManager to saltPayConfig
        }

        //TODO: delete
        private fun test(scanResponse: ScanResponse, gnosisRegistrator: GnosisRegistrator) {
            val map = mapOf(
                "CID" to scanResponse.card.cardId,
                "cardPublicKey" to scanResponse.card.cardPublicKey.toHexString(),
                "walletPublicKey" to gnosisRegistrator.walletManager.wallet.publicKey.blockchainKey.toHexString(),
                "walletAddress" to gnosisRegistrator.walletManager.wallet.address,
            )
            val json = MoshiJsonConverter.INSTANCE.prettyPrint(map)
            copyToClipboard(json)
        }

        fun makeSaltPayRegistrationManager(
            scanResponse: ScanResponse,
            gnosisRegistrator: GnosisRegistrator,
            paymentologyService: PaymentologyApiService,
            kycProvider: KYCProvider,
        ): SaltPayActivationManager {
            if (!scanResponse.isSaltPay()) {
                throw IllegalArgumentException("Can't initialize the OnboardingSaltPayMiddleware if card is not SalPay")
            }
            val saltPaySingleWallet = scanResponse.card.wallets.firstOrNull().guard {
                throw NullPointerException("SaltPay card must have one wallet at least")
            }

            return SaltPayActivationManager(
                cardId = scanResponse.card.cardId,
                cardPublicKey = scanResponse.card.cardPublicKey,
                walletPublicKey = saltPaySingleWallet.publicKey,
                kycProvider = kycProvider,
                paymentologyService = paymentologyService,
                gnosisRegistrator = gnosisRegistrator,
            )
        }

        fun makeGnosisRegistrator(
            scanResponse: ScanResponse,
            wmFactory: WalletManagerFactory,
        ): GnosisRegistrator {
            return GnosisRegistrator(
                walletManager = wmFactory.makeSaltPayWalletManager(scanResponse),
            )
        }
    }
}

enum class SaltPayActivationStep {
    None,
    NoGas,
    NeedPin,
    CardRegistration,
    KycIntro,
    KycStart,
    KycWaiting,
    KycReject,
    Claim,
    ClaimInProgress,
    ClaimSuccess,
    Success,
    Finished;
}