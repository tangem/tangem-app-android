package com.tangem.tap.features.wallet.redux

import android.content.Context
import com.tangem.blockchain.common.Amount
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.Wallet
import com.tangem.blockchain.common.WalletManager
import com.tangem.blockchain.common.address.AddressType
import com.tangem.common.card.Card
import com.tangem.tap.common.entities.FiatCurrency
import com.tangem.tap.common.redux.ErrorAction
import com.tangem.tap.common.redux.NotificationAction
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.domain.tokens.BlockchainNetwork
import com.tangem.wallet.R
import java.math.BigDecimal
import org.rekotlin.Action

sealed class WalletAction : Action {

    object ResetState : WalletAction()

    data class SetIfTestnetCard(val isTestnet: Boolean) : WalletAction()

    object LoadData : WalletAction() {
        data class Failure(val error: TapError) : WalletAction()
    }


    data class LoadWallet(
        val blockchain: BlockchainNetwork? = null,
        val walletManager: WalletManager? = null
    ) : WalletAction() {
        data class Success(val wallet: Wallet, val blockchain: BlockchainNetwork) : WalletAction()
        data class NoAccount(
            val wallet: Wallet,
            val blockchain: BlockchainNetwork,
            val amountToCreateAccount: String
        ) : WalletAction()

        data class Failure(val wallet: Wallet, val errorMessage: String? = null) : WalletAction()
    }

    data class SetArtworkId(val artworkId: String?) : WalletAction()


    sealed class MultiWallet : WalletAction() {
        data class SetIsMultiwalletAllowed(val isMultiwalletAllowed: Boolean) : MultiWallet()

        data class AddBlockchain(
            val blockchain: BlockchainNetwork,
            val walletManager: WalletManager?
        ) : MultiWallet()

        data class AddBlockchains(
            val blockchains: List<BlockchainNetwork>, val walletManagers: List<WalletManager>
        ) : MultiWallet()

        data class AddTokens(val tokens: List<Token>, val blockchain: BlockchainNetwork) :
            MultiWallet()

        data class AddToken(val token: Token, val blockchain: BlockchainNetwork) : MultiWallet()
        data class SaveCurrencies(val blockchainNetworks: List<BlockchainNetwork>) : MultiWallet()
//        object FindTokensInUse : MultiWallet()
//        object FindBlockchainsInUse : MultiWallet()

        data class TokenLoaded(
            val amount: Amount,
            val token: Token,
            val blockchain: BlockchainNetwork
        ) : MultiWallet()

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

        object RestoreFundsWarningClosed : Warnings()
    }

    data class LoadFiatRate(
        val wallet: Wallet? = null, val coinsList: List<Currency>? = null,
    ) : WalletAction() {
        data class Success(
            val fiatRates: Map<Currency, BigDecimal?>
        ) : WalletAction()

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

    sealed class DialogAction : WalletAction() {
        data class QrCode(
            val currency: Currency,
            val selectedAddress: AddressData,
        ) : DialogAction()

        object SignedHashesMultiWalletDialog : DialogAction()
        object ChooseTradeActionDialog : DialogAction()
        data class ChooseCurrency(val amounts: List<Amount>?) : DialogAction()

        object Hide : DialogAction()
    }

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

    data class SetWalletRent(
        val wallet: Wallet,
        val minRent: String,
        val rentExempt: String
    ) : WalletAction()

    data class RemoveWalletRent(val wallet: Wallet) : WalletAction()

    sealed class AppCurrencyAction : WalletAction() {
        object ChooseAppCurrency : AppCurrencyAction()
        data class SelectAppCurrency(val fiatCurrency: FiatCurrency) : AppCurrencyAction()
    }
}
