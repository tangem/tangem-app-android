package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.common.ui.cosign.CosignRequestBannerUM
import com.tangem.common.ui.cosign.SignatureProgressUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.orMaskWithStars
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.jointaccount.model.JointOperationSignerState.Answer
import com.tangem.domain.jointaccount.model.JointPendingOperation
import com.tangem.feature.wallet.presentation.wallet.state.model.JointOperationBannerUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import com.tangem.core.ui.R as CoreUiR

internal class JointOperationBannerConverter(
    private val isBalanceHidden: Boolean,
    private val onBannerClick: (JointPendingOperation) -> Unit,
) : Converter<List<JointPendingOperation>, ImmutableList<JointOperationBannerUM>> {

    override fun convert(value: List<JointPendingOperation>): ImmutableList<JointOperationBannerUM> {
        return value
            .sortedWith(compareBy({ it.isAnswered() }, { it.threshold - it.collected }))
            .map { operation ->
                JointOperationBannerUM(
                    operationId = operation.operationId,
                    banner = CosignRequestBannerUM(
                        title = title(operation),
                        subtitle = resourceReference(
                            id = CoreUiR.string.joint_operation_banner_subtitle,
                            formatArgs = wrappedList(
                                amountOf(operation).orMaskWithStars(isBalanceHidden),
                                shorten(operation.transfer.intent.recipient),
                            ),
                        ),
                        progress = SignatureProgressUM(
                            collected = operation.collected,
                            total = operation.threshold,
                        ),
                        onClick = { onBannerClick(operation) },
                    ),
                )
            }
            .toImmutableList()
    }

    /**
     * Not answering is [Answer.WAITING], not `null`, so a null check would put a waiting member's operations in
     * the answered group and undo the ordering. `null` itself means the owner address is unresolvable here, which
     * is not an answer either.
     */
    private fun JointPendingOperation.isAnswered(): Boolean = ownAnswer == Answer.SIGNED || ownAnswer == Answer.CREATED

    private fun title(operation: JointPendingOperation): TextReference {
        val currencyName = operation.transfer.currency.name

        return if (operation.isOwn) {
            resourceReference(
                id = CoreUiR.string.joint_operation_banner_title_own,
                formatArgs = wrappedList(currencyName),
            )
        } else {
            resourceReference(
                id = CoreUiR.string.joint_operation_banner_title,
                formatArgs = wrappedList(initiatorOf(operation), currencyName),
            )
        }
    }

    /**
     * The roster may not name the initiator yet; their address says who it is without inventing a name. The
     * backend can also report no initiator at all, and an empty actor would render the title as " wants to
     * send Ethereum" — so the banner says a member did it rather than nobody.
     */
    private fun initiatorOf(operation: JointPendingOperation): TextReference {
        val named = operation.initiatorName ?: operation.initiatorAddress?.let(::shorten)

        return named?.let(::stringReference)
            ?: resourceReference(CoreUiR.string.joint_account_invite_members_member)
    }

    private fun amountOf(operation: JointPendingOperation): String {
        val currency = operation.transfer.currency

        return operation.transfer.intent.amountWei
            .toBigDecimal()
            .movePointLeft(currency.decimals)
            .format { crypto(currency) }
    }

    private fun shorten(address: String): String {
        if (address.length <= PREFIX_LENGTH + SUFFIX_LENGTH) return address

        return address.take(PREFIX_LENGTH) + ELLIPSIS + address.takeLast(SUFFIX_LENGTH)
    }

    private companion object {
        const val PREFIX_LENGTH = 5
        const val SUFFIX_LENGTH = 4
        const val ELLIPSIS = "..."
    }
}