package com.tangem.features.txhistory.converter.fields

import com.tangem.common.ui.account.toUM
import com.tangem.common.ui.components.currency.icon.converter.CryptoCurrencyToIconStateConverter
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.components.transactions.state.TransactionItemUM
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.Content.Status
import com.tangem.core.ui.components.transactions.state.TransactionItemUM.ContentSubtitle
import com.tangem.core.ui.components.transactions.state.TxIcon
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_swap_horizontal_20
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_20
import com.tangem.core.ui.res.generated.icons.ic_card_20
import com.tangem.domain.express.models.ExpressExchangeStatus
import com.tangem.domain.express.models.ExpressOnrampStatus
import com.tangem.domain.jointaccount.model.FinishedJointOperation
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.TxInfo
import com.tangem.domain.models.network.TxInfo.TransactionType
import com.tangem.domain.txhistory.model.ExpressTx
import com.tangem.domain.txhistory.model.JointOperationTx
import com.tangem.domain.txhistory.model.OnChainTx
import com.tangem.domain.txhistory.model.TxHistoryInfo
import com.tangem.domain.txhistory.model.explorerHash
import com.tangem.features.txhistory.converter.ApproveSpec
import com.tangem.features.txhistory.converter.ExpressExchangeStatusToUiStatusConverter
import com.tangem.features.txhistory.converter.ExpressOnrampStatusToUiStatusConverter
import com.tangem.features.txhistory.converter.IncentiveClaimSpec
import com.tangem.features.txhistory.converter.PillSpec
import com.tangem.features.txhistory.converter.RestakeSpec
import com.tangem.features.txhistory.converter.StakeSpec
import com.tangem.features.txhistory.converter.TxHistoryStatusPillConverter
import com.tangem.features.txhistory.converter.UnstakeSpec
import com.tangem.features.txhistory.converter.VoteSpec
import com.tangem.features.txhistory.converter.WithdrawSpec
import com.tangem.features.txhistory.converter.YieldEnterSpec
import com.tangem.features.txhistory.converter.YieldExitSpec
import com.tangem.features.txhistory.converter.YieldSupplySpec
import com.tangem.features.txhistory.converter.YieldWithdrawSpec
import com.tangem.features.txhistory.converter.displaySymbol
import com.tangem.features.txhistory.converter.isFeeOnlyOperation
import com.tangem.features.txhistory.converter.moneyAccountOwnerIcon
import com.tangem.features.txhistory.converter.moneyAccountOwnerName
import com.tangem.features.txhistory.converter.toContactAvatar
import com.tangem.features.txhistory.converter.toPortfolioAvatar
import com.tangem.features.txhistory.converter.toSubtitle
import com.tangem.features.txhistory.converter.toTitle
import com.tangem.features.txhistory.converter.toUiStatus
import com.tangem.features.txhistory.impl.R
import com.tangem.features.txhistory.model.ResolvedOwner
import com.tangem.features.txhistory.model.TxHistoryLookupContext
import com.tangem.features.txhistory.model.resolveCounterparty
import com.tangem.features.txhistory.model.resolveSwapLegOwners
import com.tangem.utils.StringsSigns
import com.tangem.utils.converter.Converter
import java.math.BigDecimal

/**
 * Converts a merged [TxHistoryInfo] row to [TransactionItemUM] **cell by cell**.
 *
 * Where the row-shaped converters dispatch once on the row type and then build a whole UM per type, here every cell
 * of [TransactionItemUM.Content] has a mapping function of its own, and that function is the single place that knows
 * what each row type puts into that cell.
 *
 * ### The rule this file exists for: no `else`
 *
 * Every `when` here is exhaustive over the [TxHistoryInfo] leaves. An `else` (or an `is OnChainTx ->` catch-all over
 * subtypes that differ) turns the next history source into a silent wrong answer — it would render as an operation,
 * count as outgoing, or vanish. Without one, a new row type stops compiling in every cell that has to decide
 * something about it, which is exactly the list of decisions its author needs.
 *
 * So, when adding a row type or a cell:
 *  - group branches that share an answer (`is ExpressTx.Swap, is ExpressTx.Onramp, -> …`); never collapse them into
 *    an `else`;
 *  - a case that cannot be mapped yet gets `TODO(…)`, not a quiet default (see the TangemPay branches);
 *  - keep [pillSpecOrNull] the only place deciding whether the row is a [TransactionItemUM.Pill] or a
 *    [TransactionItemUM.Content], so the content cells never re-derive the shape;
 *  - a `when` mirroring a big hierarchy legitimately trips `CyclomaticComplexMethod` — suppress it rather than
 *    splitting the dispatch into two parallel `when`s over the same hierarchy.
 *
 * The on-chain leaves need a second level of dispatch (on [TxInfo.TransactionType]); it lives in
 * [OnChainContentFields], under the same rule, one function per cell there too.
 */
@Suppress("TooManyFunctions")
internal class TxHistoryInfoFieldConverter(
    private val currency: CryptoCurrency,
    private val onRowClick: (TxHistoryInfo) -> Unit,
    private val lookup: TxHistoryLookupContext,
    private val networkTokens: Set<CryptoCurrency.Token> = emptySet(),
) : Converter<TxHistoryInfo, TransactionItemUM> {

    private val onChain = OnChainContentFields(currency, lookup, networkTokens)
    private val pillConverter = TxHistoryStatusPillConverter(currency)
    private val exchangeStatusConverter = ExpressExchangeStatusToUiStatusConverter()
    private val onrampStatusConverter = ExpressOnrampStatusToUiStatusConverter()
    private val iconStateConverter = CryptoCurrencyToIconStateConverter()

    override fun convert(value: TxHistoryInfo): TransactionItemUM = value.pillOrNull() ?: value.content()

    private fun TxHistoryInfo.content(): TransactionItemUM.Content = TransactionItemUM.Content(
        txHash = txHash(this),
        amount = amount(this),
        currencySymbol = currencySymbol(this),
        status = status(this),
        onClick = onClick(this),
        icon = icon(this),
        title = title(this),
        subtitle = subtitle(this),
        warning = warning(this),
    )

    // region Cells

    /** Row identity (Compose key): the on-chain hash, with the express / operation id standing in while unmatched. */
    private fun txHash(row: TxHistoryInfo): String = when (row) {
        is OnChainTx.BSDK -> row.txInfo.txHash
        is OnChainTx.TangemPay -> TODO(TANGEM_PAY_TODO)
        is ExpressTx.Swap,
        is ExpressTx.Onramp,
        is JointOperationTx,
        -> row.explorerHash ?: row.txId
    }

    private fun amount(row: TxHistoryInfo): String? = when (row) {
        is OnChainTx.BSDK -> onChain.amount(row.txInfo)
        is OnChainTx.TangemPay -> TODO(TANGEM_PAY_TODO)
        // The viewed leg: an outgoing swap shows what was paid in, an incoming one what was received.
        is ExpressTx.Swap -> formatAmount(
            amount = if (row.isOutgoing) row.tx.fromAsset.amount else row.tx.toAsset.amount,
            prefix = row.amountPrefix(),
        )
        is ExpressTx.Onramp -> formatAmount(amount = row.tx.toAsset.amount, prefix = row.amountPrefix())
        is JointOperationTx -> formatAmount(amount = row.transferredAmount(), prefix = row.amountPrefix())
    }

    private fun currencySymbol(row: TxHistoryInfo): String = when (row) {
        is OnChainTx.TangemPay -> TODO(TANGEM_PAY_TODO)
        is OnChainTx.BSDK,
        is ExpressTx.Swap,
        is ExpressTx.Onramp,
        is JointOperationTx,
        -> currency.symbol
    }

    private fun status(row: TxHistoryInfo): Status = when (row) {
        is OnChainTx.BSDK -> onChain.status(row.txInfo)
        is OnChainTx.TangemPay -> TODO(TANGEM_PAY_TODO)
        is ExpressTx.Swap -> exchangeStatusConverter.convert(row.tx.status)
        is ExpressTx.Onramp -> onrampStatusConverter.convert(row.tx.status)
        // A mined relayer transaction does not mean the transfer went through; the backend status does.
        is JointOperationTx -> row.operation.status.toUiStatus()
    }

    private fun onClick(row: TxHistoryInfo): () -> Unit {
        val clickRow = { onRowClick(row) }

        return when (row) {
            is OnChainTx.TangemPay -> TODO(TANGEM_PAY_TODO)
            is OnChainTx.BSDK,
            is ExpressTx.Swap,
            is ExpressTx.Onramp,
            is JointOperationTx,
            -> clickRow
        }
    }

    private fun icon(row: TxHistoryInfo): TxIcon = when (row) {
        is OnChainTx.BSDK -> onChain.icon(row.txInfo)
        is OnChainTx.TangemPay -> TODO(TANGEM_PAY_TODO)
        is ExpressTx.Swap -> TxIcon.Vector(Icons.ic_arrow_swap_horizontal_20)
        is ExpressTx.Onramp -> TxIcon.Vector(Icons.ic_card_20)
        is JointOperationTx -> TxIcon.Vector(Icons.ic_arrow_up_20)
    }

    private fun title(row: TxHistoryInfo): TextReference = when (row) {
        is OnChainTx.BSDK -> onChain.title(row.txInfo)
        is OnChainTx.TangemPay -> TODO(TANGEM_PAY_TODO)
        is ExpressTx.Swap -> when (status(row)) {
            is Status.Confirmed, is Status.PendingSignature -> resourceReference(R.string.common_swapped)
            is Status.Unconfirmed -> resourceReference(R.string.common_swapping)
            is Status.Failed -> resourceReference(R.string.transaction_history_status_swap_failed)
        }
        is ExpressTx.Onramp -> when (status(row)) {
            is Status.Confirmed, is Status.PendingSignature -> resourceReference(R.string.tx_history_onramp_topped_up)
            is Status.Unconfirmed -> resourceReference(R.string.transaction_history_status_topping_up)
            is Status.Failed -> resourceReference(R.string.transaction_history_status_top_up_failed)
        }
        is JointOperationTx -> row.operation.status.toTitle()
    }

    private fun subtitle(row: TxHistoryInfo): ContentSubtitle = when (row) {
        is OnChainTx.BSDK -> onChain.subtitle(row.txInfo)
        is OnChainTx.TangemPay -> TODO(TANGEM_PAY_TODO)
        is ExpressTx.Swap -> row.counterpartyAssetSubtitle()
        is ExpressTx.Onramp -> ContentSubtitle.Asset(
            direction = ContentSubtitle.Direction.FROM,
            symbol = row.tx.fromFiat.currencySymbol,
            icon = CurrencyIconState.FiatIcon(
                url = row.tx.fiatCurrency?.image,
                fallbackResId = R.drawable.ic_currency_24,
            ),
        )
        is JointOperationTx -> lookup
            .resolveCounterparty(
                address = row.transfer.intent.recipient,
                networkRawId = currency.network.id.rawId,
            )
            .toSubtitle(ContentSubtitle.Direction.TO)
    }

    /**
     * KYC-verification warning. The other "problem" statuses (failed / refunded / expired) already surface as the red
     * [Status.Failed] title, so only the in-flight verification needs a line of its own.
     */
    private fun warning(row: TxHistoryInfo): TextReference? = when (row) {
        is OnChainTx.TangemPay -> TODO(TANGEM_PAY_TODO)
        is ExpressTx.Swap -> verificationWarning(row.tx.status == ExpressExchangeStatus.Verifying)
        is ExpressTx.Onramp -> verificationWarning(row.tx.status == ExpressOnrampStatus.Verifying)
        is OnChainTx.BSDK,
        is JointOperationTx,
        -> null
    }

    // endregion

    // region Row shape

    private fun TxHistoryInfo.pillOrNull(): TransactionItemUM.Pill? = when (this) {
        is OnChainTx.BSDK -> txInfo.pillSpecOrNull()?.let { spec ->
            pillConverter.convert(
                TxHistoryStatusPillConverter.Input(
                    tx = txInfo,
                    uiStatus = onChain.status(txInfo),
                    spec = spec,
                    onClick = onClick(this),
                ),
            )
        }
        is OnChainTx.TangemPay,
        is ExpressTx.Swap,
        is ExpressTx.Onramp,
        is JointOperationTx,
        -> null
    }

    /** The single place deciding that a row is a pill rather than content; `null` keeps it on the content cells. */
    @Suppress("CyclomaticComplexMethod")
    private fun TxInfo.pillSpecOrNull(): PillSpec? {
        if (isFeeOnlyOperation(currency)) return null

        return when (val type = type) {
            is TransactionType.Approve -> ApproveSpec
            is TransactionType.Staking.Stake -> StakeSpec
            is TransactionType.Staking.Unstake -> UnstakeSpec
            is TransactionType.Staking.Restake -> RestakeSpec
            is TransactionType.Staking.Vote -> VoteSpec
            is TransactionType.Staking.Withdraw -> WithdrawSpec
            is TransactionType.YieldSupply.Enter -> YieldEnterSpec
            is TransactionType.YieldSupply.Exit -> YieldExitSpec
            is TransactionType.YieldSupply.Topup -> YieldSupplySpec
            // A withdraw takes funds out of the yield position; any other Send is a plain transfer.
            is TransactionType.YieldSupply.Send -> YieldWithdrawSpec.takeIf { type.isYieldSupplyWithdraw }
            // In a token's history the claim is the reward it delivered, in the coin's the call that claimed it.
            is TransactionType.IncentiveClaim -> IncentiveClaimSpec.takeIf { currency is CryptoCurrency.Coin }
            is TransactionType.Transfer,
            is TransactionType.Joint.Transfer,
            is TransactionType.Swap,
            is TransactionType.Operation,
            is TransactionType.UnknownOperation,
            is TransactionType.Staking.ClaimRewards,
            is TransactionType.YieldSupply.DeployContract,
            is TransactionType.YieldSupply.InitializeToken,
            is TransactionType.YieldSupply.ReactivateToken,
            is TransactionType.GaslessFee,
            is TransactionType.NetworkFee,
            -> null
        }
    }

    // endregion

    // region Express / joint cell helpers

    private fun ExpressTx.Swap.amountPrefix(): String = when {
        status(this) is Status.Failed -> ""
        isOutgoing -> StringsSigns.MINUS_SIGN
        else -> StringsSigns.PLUS
    }

    private fun ExpressTx.Onramp.amountPrefix(): String = when (status(this)) {
        is Status.Failed -> ""
        is Status.Confirmed -> StringsSigns.PLUS
        is Status.Unconfirmed, is Status.PendingSignature -> StringsSigns.TILDE_SIGN
    }

    /** What the Safe transaction moved, decoded in the smallest unit of the viewed currency. */
    private fun JointOperationTx.transferredAmount(): BigDecimal =
        transfer.intent.amountWei.toBigDecimal().movePointLeft(currency.decimals)

    private fun JointOperationTx.amountPrefix(): String =
        if (operation.status == FinishedJointOperation.Status.EXECUTED) StringsSigns.MINUS else ""

    private fun formatAmount(amount: BigDecimal?, prefix: String): String? =
        amount?.let { prefix + it.format { crypto(symbol = "", decimals = currency.displayDecimals) }.trim() }

    /**
     * The ticker of the leg the swap settled against — "to: ◎ POL" — with the owner it settled with as a tail when
     * that owner is worth naming (another own portfolio, or a foreign payout address).
     */
    private fun ExpressTx.Swap.counterpartyAssetSubtitle(): ContentSubtitle.Asset {
        val counterparty = if (isOutgoing) tx.toAsset else tx.fromAsset
        val legOwners = lookup.resolveSwapLegOwners(this)

        return ContentSubtitle.Asset(
            direction = if (isOutgoing) ContentSubtitle.Direction.TO else ContentSubtitle.Direction.FROM,
            symbol = counterparty.displaySymbol,
            icon = counterparty.cryptoCurrency?.let(iconStateConverter::convert),
            owner = (if (isOutgoing) legOwners.to else legOwners.from)?.toAssetOwner(),
        )
    }

    /**
     * The subtitle tail: another own portfolio reads as "in {owner}", a foreign payout address as "for {contact}" —
     * by its address-book name, or as the address itself when no book holds it.
     */
    private fun ResolvedOwner.toAssetOwner(): ContentSubtitle.AssetOwner = when (this) {
        is ResolvedOwner.OwnAccount -> ContentSubtitle.AssetOwner.Account(
            name = account.accountName.toUM().value,
            avatar = account.icon.toPortfolioAvatar(name = account.accountName.toUM().value),
        )
        is ResolvedOwner.OwnPaymentAccount -> ContentSubtitle.AssetOwner.PaymentAccount(
            name = account.accountName.toUM().value,
        )
        is ResolvedOwner.OwnMoneyAccount -> ContentSubtitle.AssetOwner.Account(
            name = moneyAccountOwnerName,
            avatar = moneyAccountOwnerIcon.toPortfolioAvatar(name = moneyAccountOwnerName),
        )
        is ResolvedOwner.OwnWallet -> ContentSubtitle.AssetOwner.Wallet(
            name = walletInfo.name,
            deviceIconUM = walletInfo.deviceIconUM,
        )
        is ResolvedOwner.SavedContact -> ContentSubtitle.AssetOwner.Contact(
            name = contact.name.value,
            avatar = contact.toContactAvatar(),
        )
        is ResolvedOwner.External -> ContentSubtitle.AssetOwner.Address(rawAddress = address)
    }

    private fun verificationWarning(isVerifying: Boolean): TextReference? = if (isVerifying) {
        resourceReference(R.string.express_exchange_notification_verification_title)
    } else {
        null
    }

    // endregion

    private companion object {
        const val TANGEM_PAY_TODO = "TangemPay on-chain row rendering is not implemented yet"
    }
}