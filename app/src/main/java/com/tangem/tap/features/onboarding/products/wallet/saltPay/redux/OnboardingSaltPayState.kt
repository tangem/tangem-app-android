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
import com.tangem.tap.features.onboarding.products.wallet.saltPay.SaltPayConfig
import com.tangem.tap.features.onboarding.products.wallet.saltPay.SaltPayRegistrationManager
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.persistence.SaltPayRegistrationStorage
import com.tangem.tap.preferencesStorage
import com.tangem.tap.store
import java.math.BigDecimal

/**
* [REDACTED_AUTHOR]
 */
data class OnboardingSaltPayState(
    @Transient
    val saltPayManager: SaltPayRegistrationManager = SaltPayRegistrationManager.stub(),
    val saltPayConfig: SaltPayConfig = SaltPayConfig.stub(),
    val pinCode: String? = null,
    val accessCode: String? = null,
    val amountToClaim: Amount? = null,
    val tokenAmount: Amount = Amount(SaltPayWorkaround.tokenFrom(Blockchain.SaltPay), BigDecimal.ZERO),
    val step: SaltPayRegistrationStep = SaltPayRegistrationStep.None,
    val saltPayCardArtworkUrl: String? = null,
    val inProgress: Boolean = false,
    val claimInProgress: Boolean = false,
) {

    val mainButtonState: WidgetState
        get() = if (inProgress) ProgressState.Loading else ProgressState.Done

    fun readyToClaim(): Boolean = amountToClaim != null

    val pinLength: Int = 4

    companion object {
        fun initDependency(scanResponse: ScanResponse): Pair<SaltPayRegistrationManager, SaltPayConfig> {
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
                registrationStorage = preferencesStorage.saltPayRegistrationStorage,
                kycProvider = saltPayConfig.kycProvider,
            )
            test(scanResponse, gnosisRegistrator)
            return registrationManager to saltPayConfig
        }
// [REDACTED_TODO_COMMENT]
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
            registrationStorage: SaltPayRegistrationStorage,
            kycProvider: KYCProvider,
        ): SaltPayRegistrationManager {
            if (!scanResponse.isSaltPay()) {
                throw IllegalArgumentException("Can't initialize the OnboardingSaltPayMiddleware if card is not SalPay")
            }
            val saltPaySingleWallet = scanResponse.card.wallets.firstOrNull().guard {
                throw NullPointerException("SaltPay card must have one wallet at least")
            }

            return SaltPayRegistrationManager(
                cardId = scanResponse.card.cardId,
                cardPublicKey = scanResponse.card.cardPublicKey,
                walletPublicKey = saltPaySingleWallet.publicKey,
                kycProvider = kycProvider,
                paymentologyService = paymentologyService,
                gnosisRegistrator = gnosisRegistrator,
                registrationStorage = registrationStorage,
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

enum class SaltPayRegistrationStep {
    None,
    NoGas,
    NeedPin,
    CardRegistration,
    KycIntro,
    KycStart,
    KycWaiting,
    Claim,
    ClaimInProgress,
    ClaimSuccess;
}
