package com.tangem.tap.features.wallet.redux

import android.content.Context
import com.tangem.blockchain.common.*
import com.tangem.blockchain.common.address.AddressType
import com.tangem.common.card.Card
import com.tangem.tap.common.redux.ErrorAction
import com.tangem.tap.common.redux.NotificationAction
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.domain.tokens.CardCurrencies
import com.tangem.tap.domain.twins.TwinCardNumber
import com.tangem.wallet.R
import org.rekotlin.Action
import java.math.BigDecimal

sealed class WalletAction : Action {

    object ResetState : WalletAction()

    data class SetIfTestnetCard(val isTestnet: Boolean) : WalletAction()

    object LoadData : WalletAction() {
        data class Failure(val error: TapError) : WalletAction()
    }


    data class LoadWallet(
        val allowToSell: Boolean? = null, val allowToBuy: Boolean? = null,
        val blockchain: Blockchain? = null,
    ) :
        WalletAction() {
        data class Success(val wallet: Wallet) : WalletAction()
        data class NoAccount(val wallet: Wallet, val amountToCreateAccount: String) : WalletAction()
        data class Failure(val wallet: Wallet, val errorMessage: String? = null) : WalletAction()
    }

    data class SetArtworkId(val artworkId: String?) : WalletAction()


    sealed class MultiWallet : WalletAction() {
        data class SetIsMultiwalletAllowed(val isMultiwalletAllowed: Boolean) : MultiWallet()
        data class AddWalletManagers(val walletManagers: List<WalletManager>) : MultiWallet() {
            constructor(walletManager: WalletManager) : this(listOf(walletManager))
        }

        data class AddBlockchain(val blockchain: Blockchain) : MultiWallet()
        data class AddBlockchains(val blockchains: List<Blockchain>) : MultiWallet()
        data class AddTokens(val tokens: List<Token>) : MultiWallet()
        data class AddToken(val token: Token) : MultiWallet()
        data class SaveCurrencies(val cardCurrencies: CardCurrencies) : MultiWallet()
        object FindTokensInUse : MultiWallet()
        data class FindBlockchainsInUse(val card: Card, val factory: WalletManagerFactory) :
            MultiWallet()

        data class TokenLoaded(val amount: Amount, val token: Token) : MultiWallet()
        data class SelectWallet(val walletData: WalletData?) : MultiWallet()
        data class RemoveWallet(val walletData: WalletData) : MultiWallet()
        data class SetPrimaryBlockchain(val blockchain: Blockchain) : MultiWallet()
        data class SetPrimaryToken(val token: Token) : MultiWallet()
    }

    sealed class Warnings : WalletAction() {
        object CheckHashesCount : Warnings() {
            object CheckHashesCountOnline : Warnings()
            object NeedToCheckHashesCountOnline : Warnings()
            object ConfirmHashesCount : Warnings()
            object SaveCardId : Warnings()
        }

        object CheckIfNeeded : Warnings()
        object Update : Warnings()
        data class Set(val warningList: List<WarningMessage>) : Warnings()

        object AppRating : Warnings() {
            object SetNeverToShow : Warnings()
            object RemindLater : Warnings()
        }

        class CheckRemainingSignatures(val remainingSignatures: Int?) : Warnings()
    }

    data class UpdateWallet(val blockchain: Blockchain? = null) : WalletAction() {
        object ScheduleUpdatingWallet : WalletAction()
        data class Success(val wallet: Wallet) : WalletAction()
        data class Failure(val errorMessage: String? = null) : WalletAction()
    }

    data class LoadFiatRate(
        val wallet: Wallet? = null, val currency: Currency? = null,
    ) : WalletAction() {
        data class Success(val fiatRate: Pair<Currency, BigDecimal?>) : WalletAction()
        object Failure : WalletAction()
    }

    class LoadCardInfo(val card: Card) : WalletAction()

    data class LoadArtwork(val card: Card, val artworkId: String?) : WalletAction() {
        data class Success(val artwork: Artwork) : WalletAction()
        object Failure : WalletAction()
    }

    object Scan : WalletAction()

    data class Send(val amount: Amount? = null) : WalletAction() {
        data class ChooseCurrency(val amounts: List<Amount>?) : WalletAction()
        object Cancel : WalletAction()
    }


    object EmptyField : WalletAction(), ErrorAction {
        override val error = TapError.PayIdEmptyField
    }

    data class CopyAddress(val address: String, val context: Context) : WalletAction() {
        object Success : WalletAction(), NotificationAction {
            override val messageResource = R.string.wallet_notification_address_copied
        }
    }

    data class ShareAddress(val address: String, val context: Context) : WalletAction()

    object ShowDialog : WalletAction() {
        object QrCode : WalletAction()
        object SignedHashesMultiWalletDialog : WalletAction()
        object ChooseTradeActionDialog : WalletAction()
    }

    object HideDialog : WalletAction()

    data class ExploreAddress(val exploreUrl: String, val context: Context) : WalletAction()

    object CreateWallet : WalletAction()
    object EmptyWallet : WalletAction()

    sealed class TradeCryptoAction : WalletAction() {
        object Buy : TradeCryptoAction()
        object Sell : TradeCryptoAction()
        data class FinishSelling(val transactionId: String) : TradeCryptoAction()
        data class SendCrypto(
            val currencyId: String,
            val amount: String,
            val destinationAddress: String,
            val transactionId: String
        ) : TradeCryptoAction()
    }

    data class ChangeSelectedAddress(val type: AddressType) : WalletAction()

    sealed class TwinsAction : WalletAction() {
        object ShowOnboarding : TwinsAction()
        object SetOnboardingShown : TwinsAction()
        data class SetTwinCard(
            val secondCardId: String?, val number: TwinCardNumber,
            val isCreatingTwinCardsAllowed: Boolean,
        ) : TwinsAction()
    }
}