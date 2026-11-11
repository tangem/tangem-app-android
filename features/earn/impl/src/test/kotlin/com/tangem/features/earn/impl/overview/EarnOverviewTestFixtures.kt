package com.tangem.features.earn.impl.overview

import arrow.core.Either
import arrow.core.right
import com.tangem.common.test.domain.wallet.MockUserWalletFactory
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.earn.api.overview.EarnBadgeAccent
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.impl.fake.FakeEarnProductPreset
import com.tangem.features.earn.impl.fake.createFakeEarnProduct
import com.tangem.features.earn.product.api.product.EarnPositionSummary
import com.tangem.features.earn.product.api.product.EarnProduct
import com.tangem.features.earn.product.api.product.EarnProductSummary
import com.tangem.features.earn.product.api.product.EarnRateEntry
import com.tangem.features.earn.product.api.product.EarnRateSource
import com.tangem.features.earn.product.api.product.EarnRisk
import com.tangem.features.earn.product.api.product.ProductAvailability
import com.tangem.features.earn.product.api.session.EarnAction
import com.tangem.hot.sdk.model.HotWalletId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flow
import java.math.BigDecimal

internal val ETH_ID: CryptoCurrency.ID = CryptoCurrency.ID.fromValue("coin⟨ETH⟩ethereum")

internal val USER_WALLET_ID = UserWalletId("0123456789abcdef")

internal data object TestAction : EarnAction {
    override val id: String = "test_action"
    override val label: TextReference = stringReference("Test action")
}

internal data class TestRoute(val name: String) : Route

internal fun createRate(fraction: String = "0.05", type: EarnRateType = EarnRateType.APY) =
    EarnProductRate(fraction = BigDecimal(fraction), type = type)

internal fun createSummary(
    productId: EarnProductId = EarnProductId("product"),
    rate: EarnProductRate? = createRate(),
    availability: ProductAvailability = ProductAvailability.Available,
    position: EarnPositionSummary? = null,
    source: StatusSource = StatusSource.ACTUAL,
    route: Route? = null,
    rateText: TextReference = stringReference("rate"),
    risk: EarnRisk? = EarnRisk.Low,
    onClickAction: EarnAction = TestAction,
) = EarnProductSummary(
    productId = productId,
    rateText = rateText,
    risk = risk,
    availability = availability,
    rate = rate,
    position = position,
    source = source,
    onClickAction = onClickAction,
    route = route,
)

internal fun createRateEntry(
    rate: EarnProductRate = createRate(),
    isActive: Boolean = false,
    source: StatusSource = StatusSource.ACTUAL,
    route: Route? = null,
) = EarnRateEntry(rate = rate, isActive = isActive, source = source, route = route)

/**
 * A product whose summaries, rank, legacy flag and refresh result are set by the test; everything else comes from the
 * Lending fake. [summariesCalls] counts calls of `summaries`, [collectCount] counts collections of the returned flow,
 * [refreshCalls] counts calls of `refresh`.
 */
internal class TestEarnProduct(
    override val id: EarnProductId,
    override val rank: Int = 0,
    override val hasLegacyBlock: Boolean = false,
    private val summariesFlow: Flow<Map<CryptoCurrency.ID, EarnProductSummary>> = emptyFlow(),
    private val refreshResult: Either<Throwable, Unit> = Unit.right(),
    override val badgeAccent: EarnBadgeAccent = EarnBadgeAccent.Success,
    delegate: EarnProduct = createFakeEarnProduct(FakeEarnProductPreset.Lending),
) : EarnProduct by delegate {

    var summariesCalls: Int = 0
        private set

    var collectCount: Int = 0
        private set

    var refreshCalls: Int = 0
        private set

    private val summariesWalletIdsLog = mutableListOf<UserWalletId>()

    private val refreshParamsLog = mutableListOf<EarnProduct.Params>()

    /** The wallet id of every call of `summaries`. */
    val summariesWalletIds: List<UserWalletId> get() = summariesWalletIdsLog

    /** The params of every call of `refresh`. */
    val refreshParams: List<EarnProduct.Params> get() = refreshParamsLog

    override fun summaries(userWalletId: UserWalletId): Flow<Map<CryptoCurrency.ID, EarnProductSummary>> {
        summariesCalls++
        summariesWalletIdsLog += userWalletId
        return flow {
            collectCount++
            emitAll(summariesFlow)
        }
    }

    override suspend fun refresh(params: EarnProduct.Params): Either<Throwable, Unit> {
        refreshCalls++
        refreshParamsLog += params
        return refreshResult
    }
}

internal class TestEarnRateSource(
    override val id: EarnProductId,
    override val rank: Int = 0,
    private val ratesFlow: Flow<Map<CryptoCurrency.ID, EarnRateEntry>> = emptyFlow(),
    override val badgeAccent: EarnBadgeAccent = EarnBadgeAccent.Success,
) : EarnRateSource {

    private val ratesWalletIdsLog = mutableListOf<UserWalletId>()

    /** The wallet id of every call of `rates`. */
    val ratesWalletIds: List<UserWalletId> get() = ratesWalletIdsLog

    override fun rates(userWalletId: UserWalletId): Flow<Map<CryptoCurrency.ID, EarnRateEntry>> {
        ratesWalletIdsLog += userWalletId
        return ratesFlow
    }
}

/** A multi-currency cold wallet; a locked one has no currency wallets on its card. */
internal fun createColdWallet(walletId: UserWalletId = USER_WALLET_ID, isLocked: Boolean = false): UserWallet.Cold {
    val wallet = MockUserWalletFactory.create()
    val scanResponse = wallet.scanResponse
    return wallet.copy(
        walletId = walletId,
        scanResponse = if (isLocked) scanResponse.copy(card = scanResponse.card.copy(wallets = emptyList())) else scanResponse,
    )
}

/** A single-currency cold wallet such as Note or Twin. */
internal fun createSingleCurrencyColdWallet(walletId: UserWalletId = USER_WALLET_ID): UserWallet.Cold =
    MockUserWalletFactory.createSingleWalletWithToken().copy(walletId = walletId)

/** A hot wallet is always multi-currency; a locked one has no wallets loaded. */
internal fun createHotWallet(walletId: UserWalletId = USER_WALLET_ID, isLocked: Boolean = false) = UserWallet.Hot(
    name = "Hot",
    walletId = walletId,
    hotWalletId = HotWalletId(value = "hot", authType = HotWalletId.AuthType.NoPassword),
    wallets = if (isLocked) null else emptyList(),
    isBackedUp = false,
)