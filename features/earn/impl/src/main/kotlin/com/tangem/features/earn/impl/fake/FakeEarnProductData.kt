package com.tangem.features.earn.impl.fake

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.tangem.core.res.R as ResR
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.ImageReference
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.features.earn.api.overview.EarnBadgeAccent
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.product.api.analytics.EarnProductAnalytics
import com.tangem.features.earn.product.api.product.EarnActionItem
import com.tangem.features.earn.product.api.product.EarnApyHistory
import com.tangem.features.earn.product.api.product.EarnApyPoint
import com.tangem.features.earn.product.api.product.EarnBlock
import com.tangem.features.earn.product.api.product.EarnBlockAction
import com.tangem.features.earn.product.api.product.EarnHorizon
import com.tangem.features.earn.product.api.product.EarnHowItWorks
import com.tangem.features.earn.product.api.product.EarnLimits
import com.tangem.features.earn.product.api.product.EarnPeriod
import com.tangem.features.earn.product.api.product.EarnPosition
import com.tangem.features.earn.product.api.product.EarnPositionState
import com.tangem.features.earn.product.api.product.EarnPositionSummary
import com.tangem.features.earn.product.api.product.EarnProductState
import com.tangem.features.earn.product.api.product.EarnProductSummary
import com.tangem.features.earn.product.api.product.EarnProjection
import com.tangem.features.earn.product.api.product.EarnProjectionPoint
import com.tangem.features.earn.product.api.product.EarnProjectionResult
import com.tangem.features.earn.product.api.product.EarnProvider
import com.tangem.features.earn.product.api.product.EarnRewards
import com.tangem.features.earn.product.api.product.EarnRisk
import com.tangem.features.earn.product.api.product.EarnRow
import com.tangem.features.earn.product.api.product.EarnTextBlock
import com.tangem.features.earn.product.api.product.EarnValue
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_20
import com.tangem.core.ui.res.generated.icons.ic_lightning_20
import com.tangem.features.earn.product.api.product.ProductAvailability
import com.tangem.features.earn.product.api.session.Deposit
import com.tangem.features.earn.product.api.session.EarnAction
import com.tangem.features.earn.product.api.session.Withdraw
import java.math.BigDecimal
import kotlin.time.Duration.Companion.days

private val FAKE_PRODUCT_POSITION = BigDecimal("1.482")
private const val FAKE_FEE_PERCENT = "15%"
private const val FAKE_APY_POINTS = 3

internal data class FakeEarnProductSpec(
    val id: EarnProductId,
    val rank: Int,
    val badgeAccent: EarnBadgeAccent,
    val title: TextReference,
    val provider: EarnProvider,
    val analytics: EarnProductAnalytics,
)

internal val FakeEarnProductPreset.spec: FakeEarnProductSpec
    get() = when (this) {
        FakeEarnProductPreset.Lending -> fakeSpec(
            id = "lending",
            rank = 0,
            title = resourceReference(ResR.string.earn_product_lending_on_aave),
            provider = fakeProvider(
                title = resourceReference(ResR.string.yield_module_provider),
                logo = R.drawable.ic_aave_36,
            ),
            product = "Lending",
        )
        FakeEarnProductPreset.Staking -> fakeSpec(
            id = "staking",
            rank = 1,
            title = resourceReference(ResR.string.earn_product_pooled_staking),
            provider = fakeProvider(title = stringReference("P2P"), logo = R.drawable.ic_p2p_logo),
            product = "Staking",
        ).copy(badgeAccent = EarnBadgeAccent.Info)
        FakeEarnProductPreset.Vault -> fakeSpec(
            id = "vault",
            rank = 2,
            title = stringReference("Vault"),
            provider = fakeProvider(title = stringReference("Vault"), logo = R.drawable.ic_p2p_logo),
            product = "Vault",
        )
    }

internal sealed interface FakeNavigationAction : EarnAction {

    data object OpenInfo : FakeNavigationAction {
        override val id: String = "open_info"
        override val label: TextReference = stringReference("Open")
    }

    data object OpenPicker : FakeNavigationAction {
        override val id: String = "open_picker"
        override val label: TextReference = stringReference("Choose")
    }
}

internal class FakeEarnProjection : EarnProjection {

    override val netRate: BigDecimal = BigDecimal("0.04675")

    override fun project(
        initialFiat: BigDecimal,
        monthlyTopUpFiat: BigDecimal,
        horizon: EarnHorizon,
    ): EarnProjectionResult = EarnProjectionResult(
        total = BigDecimal("24255.24"),
        earned = BigDecimal("4250"),
        points = listOf(
            EarnProjectionPoint(index = 0, capital = BigDecimal("20005.24"), income = BigDecimal.ZERO),
            EarnProjectionPoint(index = 1, capital = BigDecimal("22000"), income = BigDecimal("2000")),
            EarnProjectionPoint(index = 2, capital = BigDecimal("24255.24"), income = BigDecimal("4250")),
        ),
    )

    override fun yearlyEstimate(amountFiat: BigDecimal): BigDecimal = amountFiat.multiply(netRate)
}

internal val fakeApyPeriods: List<EarnPeriod> = listOf(
    EarnPeriod(id = "1m", label = stringReference("1M")),
    EarnPeriod(id = "3m", label = stringReference("3M")),
    EarnPeriod(id = "6m", label = stringReference("6M")),
)

private val fakeApyAverages = listOf(BigDecimal("0.052"), BigDecimal("0.0505"), BigDecimal("0.0512"))

internal fun fakeRate(): EarnProductRate = EarnProductRate(fraction = BigDecimal("0.055"), type = EarnRateType.APY)

internal fun fakeSummary(preset: FakeEarnProductPreset, position: FakeEarnPosition): EarnProductSummary {
    val isActive = position == FakeEarnPosition.Known
    return EarnProductSummary(
        productId = preset.spec.id,
        // Written by the product, unlike the overview badge built from the rate: the formats differ by design
        rateText = resourceReference(ResR.string.earn_apy_value, wrappedList("5.5%")),
        risk = EarnRisk.Low,
        availability = ProductAvailability.Available,
        rate = fakeRate(),
        position = EarnPositionSummary(FAKE_PRODUCT_POSITION).takeIf { isActive },
        source = if (position == FakeEarnPosition.Unknown) StatusSource.ONLY_CACHE else StatusSource.ACTUAL,
        onClickAction = if (isActive) FakeNavigationAction.OpenInfo else FakeNavigationAction.OpenPicker,
        route = FakeForeignRoute.takeIf { preset == FakeEarnProductPreset.Staking },
    )
}

internal fun fakeReadyState(preset: FakeEarnProductPreset, position: FakeEarnPosition): EarnProductState.Ready {
    val isStaking = preset == FakeEarnProductPreset.Staking
    return EarnProductState.Ready(
        rate = fakeRate(),
        description = if (preset == FakeEarnProductPreset.Lending) {
            resourceReference(ResR.string.earn_yield_picker_lending_description)
        } else {
            stringReference("Earn interest on your ETH while keeping it liquid")
        },
        projection = FakeEarnProjection().takeUnless { isStaking },
        benefits = listOf(
            EarnTextBlock(
                title = resourceReference(ResR.string.yield_module_promo_screen_cash_out_title),
                description = resourceReference(ResR.string.yield_module_promo_screen_cash_out_subtitle),
                icon = TangemIconUM.Icon(Icons.ic_lightning_20),
            ),
            EarnTextBlock(
                title = resourceReference(ResR.string.earn_product_details_lending_withdraw_anytime_title),
                description = resourceReference(ResR.string.earn_product_details_lending_withdraw_anytime_subtitle),
                icon = TangemIconUM.Icon(Icons.ic_arrow_refresh_20),
            ),
        ),
        details = fakeDetails(),
        limits = EarnLimits(
            minDeposit = BigDecimal("0.01"),
            remainingCap = BigDecimal("1000").takeIf { preset == FakeEarnProductPreset.Vault },
        ),
        apyPeriods = fakeApyPeriods,
        primary = fakePrimary(position),
        secondary = fakeSecondary(preset, position),
        freshness = null,
        position = fakePositionState(preset, position),
        incentiveRate = EarnProductRate(fraction = BigDecimal("0.014"), type = EarnRateType.APY)
            .takeIf { preset == FakeEarnProductPreset.Lending },
    )
}

internal fun fakeApyHistory(period: EarnPeriod): Either<Throwable, EarnApyHistory> {
    val index = fakeApyPeriods.indexOfFirst { it.id == period.id }
    if (index < 0) return IllegalArgumentException("Unknown period").left()

    val average = fakeApyAverages[index]
    val rate = EarnProductRate(fraction = average, type = EarnRateType.APY)
    return EarnApyHistory(
        points = List(FAKE_APY_POINTS) { EarnApyPoint(at = FAKE_INSTANT - it.days, rate = rate) },
        average = average,
    ).right()
}

private fun fakeProvider(title: TextReference, logo: Int) = EarnProvider(
    title = title,
    icon = ImageReference.Res(logo),
)

private fun fakeSpec(id: String, rank: Int, title: TextReference, provider: EarnProvider, product: String) =
    FakeEarnProductSpec(
        id = EarnProductId(id),
        rank = rank,
        badgeAccent = EarnBadgeAccent.Success,
        title = title,
        provider = provider,
        analytics = EarnProductAnalytics(category = "Earn", product = product),
    )

internal fun fakeHowItWorks(preset: FakeEarnProductPreset): EarnHowItWorks? = EarnHowItWorks(
    title = stringReference("How it works"),
    entries = listOf(
        EarnTextBlock(stringReference("Supply"), stringReference("Your ETH is supplied to the protocol")),
        EarnTextBlock(stringReference("Earn"), stringReference("Interest accrues automatically")),
    ),
).takeUnless { preset == FakeEarnProductPreset.Staking }

private fun fakeRow(label: TextReference, value: TextReference, info: TextReference? = null) = EarnRow(
    label = label,
    value = EarnValue.Text(value),
    info = info?.let { EarnTextBlock(label, it) },
)

private fun fakeAmountRow(label: TextReference, amount: BigDecimal, info: TextReference? = null) = EarnRow(
    label = label,
    value = EarnValue.Amount(amount),
    info = info?.let { EarnTextBlock(label, it) },
)

private fun fakeDetails(): List<EarnRow> {
    val withdrawals = resourceReference(ResR.string.earn_product_details_withdrawals)
    val fee = resourceReference(ResR.string.earn_product_details_performance_fee)
    val percent = wrappedList<Any>(FAKE_FEE_PERCENT)
    return listOf(
        EarnRow(
            label = resourceReference(ResR.string.earn_product_details_risk),
            value = EarnValue.Text(resourceReference(ResR.string.earn_risk_level_low)),
            info = EarnTextBlock(
                resourceReference(ResR.string.earn_product_details_risk_info_title),
                resourceReference(ResR.string.earn_product_details_risk_low_description),
            ),
        ),
        EarnRow(
            label = resourceReference(ResR.string.earn_product_details_protocol),
            value = EarnValue.Link(
                text = resourceReference(ResR.string.yield_module_provider),
                url = "https://aave.com",
            ),
        ),
        fakeRow(
            label = withdrawals,
            value = resourceReference(ResR.string.earn_product_details_usually_instant),
            info = resourceReference(ResR.string.earn_product_details_lending_withdrawals_info),
        ),
        fakeRow(
            label = fee,
            value = resourceReference(ResR.string.earn_product_details_performance_fee_value, percent),
            info = resourceReference(ResR.string.earn_product_details_performance_fee_info, percent),
        ),
    )
}

private fun fakePrimary(position: FakeEarnPosition): EarnActionItem = when (position) {
    FakeEarnPosition.None -> EarnActionItem(
        action = Deposit(),
        label = resourceReference(ResR.string.eth_yield_start_earning),
    )
    FakeEarnPosition.Known -> EarnActionItem(
        action = Deposit(),
        label = resourceReference(ResR.string.common_add_funds),
    )
    FakeEarnPosition.Unknown -> EarnActionItem(
        action = Deposit(),
        block = EarnBlock(reason = stringReference("Position is not loaded"), action = EarnBlockAction.Retry),
        label = resourceReference(ResR.string.eth_yield_start_earning),
    )
}

private fun fakeSecondary(preset: FakeEarnProductPreset, position: FakeEarnPosition): List<EarnActionItem> =
    if (position == FakeEarnPosition.Known && preset != FakeEarnProductPreset.Staking) {
        listOf(EarnActionItem(action = Withdraw()))
    } else {
        emptyList()
    }

private fun fakePositionState(preset: FakeEarnProductPreset, position: FakeEarnPosition): EarnPositionState =
    when (position) {
        FakeEarnPosition.None -> EarnPositionState.None
        FakeEarnPosition.Unknown -> EarnPositionState.Unknown
        FakeEarnPosition.Known -> EarnPositionState.Known(
            EarnPosition(
                cryptoAmount = FAKE_PRODUCT_POSITION,
                earned = BigDecimal("0.03"),
                description = resourceReference(ResR.string.lending_position_about),
                rows = listOf(
                    fakeAmountRow(
                        label = resourceReference(ResR.string.token_details_balance_total),
                        amount = FAKE_PRODUCT_POSITION,
                        info = resourceReference(
                            ResR.string.lending_position_total_balance_info,
                            wrappedList<Any>(FAKE_FEE_PERCENT),
                        ),
                    ),
                    fakeAmountRow(
                        label = resourceReference(ResR.string.token_details_balance_available),
                        amount = FAKE_PRODUCT_POSITION,
                        info = resourceReference(ResR.string.lending_position_available_balance_info),
                    ),
                ),
                rewards = EarnRewards(amount = BigDecimal("0.02"), claimAction = fakeClaimAction())
                    .takeIf { preset == FakeEarnProductPreset.Lending },
                freshness = null,
            ),
        )
    }