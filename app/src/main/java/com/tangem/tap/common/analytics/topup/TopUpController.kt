package com.tangem.tap.common.analytics.topup

import com.tangem.common.extensions.guard
import com.tangem.common.extensions.isZero
import com.tangem.core.analytics.Analytics
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.util.UserWalletId
import com.tangem.tap.common.analytics.converters.TopUpEventConverter
import com.tangem.tap.common.extensions.copy
import com.tangem.tap.domain.model.UserWallet
import com.tangem.tap.domain.model.WalletDataModel
import com.tangem.tap.domain.model.WalletStoreModel
import com.tangem.tap.domain.model.builders.UserWalletIdBuilder
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.domain.walletCurrencies.WalletCurrenciesManager
import com.tangem.tap.domain.walletStores.WalletStoresManager
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.redux.ProgressState
import com.tangem.tap.scope
import kotlinx.coroutines.launch
import timber.log.Timber

/**
* [REDACTED_AUTHOR]
 */
class TopUpController(
    var scanResponseProvider: (() -> ScanResponse?)? = null,
    var walletStoresManagerProvider: (() -> WalletStoresManager)? = null,
) : WalletCurrenciesManager.Listener {

    private var hadMissedDerivations: Boolean = false
    private val addedCurrencies = mutableListOf<Currency>()

    override fun willUpdate(userWallet: UserWallet, currency: Currency) {
    }

    override fun didUpdate(userWallet: UserWallet, currency: Currency) {
        tryToNotify()
    }

    override fun willCurrenciesAdd(userWallet: UserWallet, currenciesToAdd: List<Currency>) {
        addedCurrencies.addAll(currenciesToAdd.distinct())
        log("addCurrencies = [${addedCurrencies.joinToString()}]")
    }

    override fun willCurrenciesRemove(userWallet: UserWallet, currenciesToRemove: List<Currency>) {
    }

    override fun willCurrencyRemove(userWallet: UserWallet, currencyToRemove: Currency) {
    }

    fun walletStoresChanged(walletStores: List<WalletStoreModel>) {
        val missedDerivations = walletStores
            .flatMap { it.walletsData }
            .map { it.status }
            .filterIsInstance<WalletDataModel.MissedDerivation>()

        hadMissedDerivations = missedDerivations.isNotEmpty()
        log("walletStoresChanged: hadMissedDerivations = [${hadMissedDerivations}]")
    }

    fun scanToGetDerivations() {
        hadMissedDerivations = true
        log("scanAndUpdateCard: hadMissedDerivations = [${hadMissedDerivations}]")
    }

    fun addMissingDerivations(blockchains: List<BlockchainNetwork>) {
        hadMissedDerivations = blockchains.isNotEmpty()
        log("addMissingDerivations: hadMissedDerivations = [${hadMissedDerivations}]")
    }

    fun totalBalanceStateChanged(state: ProgressState) {
        log("totalBalanceStateChanged = [${state.name}]")
        if (state == ProgressState.Done) {
            tryToNotify()
        }
    }

    fun loadDataSuccess() {
        log("loadDataSuccess")
        tryToNotify()
    }

    private fun tryToNotify() {
        if (hadMissedDerivations) {
            log("tryToNotify: FAILED: derivationsCheckIsScheduled")
            return
        }
        val scanResponse = scanResponseProvider?.invoke() ?: return
        val userWalletId = UserWalletIdBuilder.scanResponse(scanResponse).build() ?: return
        val walletStoresManager = walletStoresManagerProvider?.invoke() ?: return

        scope.launch {
            val walletDataModels = walletStoresManager.getSync(userWalletId).flatMap { it.walletsData }
            if (walletDataModels.isEmpty()) {
                log("tryToNotify: FAILED: walletDataModels.size = [0]")
                return@launch
            }
            val isCorrectStatus = walletDataModels.any {
                it.status is WalletDataModel.Loading ||
                    it.status is WalletDataModel.NoAccount ||
                    it.status is WalletDataModel.Unreachable ||
                    it.status is WalletDataModel.MissedDerivation ||
                    it.status.isErrorStatus
            }
            if (isCorrectStatus) {
                log("tryToNotify: FAILED: by status")
                return@launch
            }
            notify(userWalletId, walletDataModels, scanResponse.cardTypesResolver)
        }
    }

    private fun notify(
        userWalletId: UserWalletId,
        walletDataModels: List<WalletDataModel>,
        cardTypesResolver: CardTypesResolver,
    ) {
        val isToppedUpInPast = findToppedUpCurrenciesInPast(walletDataModels).isNotEmpty()
        log("notify: currencies from manage tokens had toppedUp in the past = [${isToppedUpInPast}]")

        val data = TopUpEventConverter.Data(
            cardTypesResolver = cardTypesResolver,
            walletDataModels = walletDataModels,
            userWalletIdValue = userWalletId.stringValue,
            isToppedUpInPast = isToppedUpInPast,
        )
        val event = TopUpEventConverter().convert(data).guard {
            log("notify: TopUpEventConverter can't convert a data to an event")
            return
        }

        log("notify: Analytics.send(event)")
        Analytics.send(event)
    }

    private fun findToppedUpCurrenciesInPast(walletDataModels: List<WalletDataModel>): List<WalletDataModel> {
        log("findToppedUpCurrenciesInPast: added new currencies = [${addedCurrencies.size}]")
        val currenciesToppedUpInPast = addedCurrencies.copy()
            .mapNotNull { currency ->
                val foundCurrencyModel = walletDataModels
                    .find { it.currency == currency }
                    ?: return@mapNotNull null

                if (foundCurrencyModel.status.amount.isZero()) null else foundCurrencyModel
            }
        addedCurrencies.clear()

        return currenciesToppedUpInPast
    }
}

fun log(log: String) {
    Timber.d("TopUp: %s", log)
}
