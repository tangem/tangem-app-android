package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import com.tangem.domain.models.equalsEvmAddress
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.normalizeEvmAddress
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.yield.supply.YieldModuleAddressProvider
import com.tangem.domain.yield.supply.models.incentives.YieldClaimCalldata
import com.tangem.domain.yield.supply.models.incentives.YieldClaimValidationError
import com.tangem.utils.coroutines.runSuspendCatching
import com.tangem.utils.logging.TangemLogger

/**
 * What the claim is checked against, gathered by the caller before the payload is fetched.
 *
 * @property moduleAddress module the incentives response reports the rewards under
 * @property supportedRewardTokens lowercased address of every reward that response reports as routable; a payload
 * paying out anything else is refused whole
 */
data class YieldClaimValidationContext(
    val userWalletId: UserWalletId,
    val network: Network,
    val chainId: Int,
    val userAddress: String,
    val moduleAddress: String,
    val supportedRewardTokens: Set<String>,
)

/**
 * Gate every claim passes right before signing, against freshly fetched calldata.
 *
 * The payload is opaque: never parsed, never displayed, never rewritten. One paying out a reward this version cannot
 * route fails closed rather than being narrowed to the supported subset, because the proof is cumulative and a
 * filtered replacement would not be a valid claim.
 *
 * The destination is pinned against the module derived from the factory contract, not against the two addresses the
 * backend sends — those travel together, so agreeing with each other proves nothing. An address that cannot be
 * derived leaves nothing to pin against, and the claim is refused rather than signed on the backend's word alone.
 */
class ValidateYieldClaimCalldataUseCase(
    private val yieldModuleAddressProvider: YieldModuleAddressProvider,
) {

    suspend operator fun invoke(
        calldata: YieldClaimCalldata,
        context: YieldClaimValidationContext,
    ): Either<YieldClaimValidationError, YieldClaimCalldata> = either {
        ensure(calldata.hasClaimable) { YieldClaimValidationError.NothingToClaim }
        ensure(calldata.isClaimRecommended) { YieldClaimValidationError.NotRecommended }

        val responseModule = ensureNotNull(calldata.moduleAddress?.takeIf(String::isNotBlank)) {
            YieldClaimValidationError.IncompleteCalldata
        }
        val to = ensureNotNull(calldata.to?.takeIf(String::isNotBlank)) {
            YieldClaimValidationError.IncompleteCalldata
        }
        val payload = ensureNotNull(calldata.data?.takeIf(String::isNotBlank)) {
            YieldClaimValidationError.IncompleteCalldata
        }

        // A contract call is at least its method selector, in whole bytes of hex; anything else would be signed
        // as a bare value transfer or fail far later, inside the call data parser.
        val digits = payload.removePrefix(HEX_PREFIX)
        ensure(
            digits.length >= METHOD_SELECTOR_HEX_LENGTH &&
                digits.length % 2 == 0 &&
                digits.all { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' },
        ) {
            YieldClaimValidationError.IncompleteCalldata
        }

        ensure(calldata.chainId == context.chainId && calldata.userAddress.equalsEvmAddress(context.userAddress)) {
            YieldClaimValidationError.ContextMismatch
        }

        val derivedModule = ensureNotNull(deriveModuleAddress(context)) {
            YieldClaimValidationError.ModuleAddressUnavailable
        }
        // Rewards reported under another module say nothing about what this one may claim, so the reported module
        // is held to the derived one as well.
        ensure(
            to.equalsEvmAddress(responseModule) &&
                to.equalsEvmAddress(derivedModule) &&
                context.moduleAddress.equalsEvmAddress(derivedModule),
        ) {
            YieldClaimValidationError.DestinationMismatch
        }

        ensure(calldata.rewardTokens.isNotEmpty() && calldata.rewardTokens.size == calldata.cumulativeAmounts.size) {
            YieldClaimValidationError.MalformedProof
        }

        // An allow-list, not a deny-list: a token the incentives response says nothing about is as unroutable as one
        // it names unsupported, and the wrapped rewards the service is gaining pay out under an address of their own.
        ensure(calldata.rewardTokens.all { token -> token.normalizeEvmAddress() in context.supportedRewardTokens }) {
            YieldClaimValidationError.UnsupportedRewardToken
        }

        calldata
    }

    /** The provider reaches a wallet manager and an RPC, neither of which answers with a value when it is absent. */
    private suspend fun deriveModuleAddress(context: YieldClaimValidationContext): String? {
        val derived = runSuspendCatching {
            yieldModuleAddressProvider.getOrFetch(userWalletId = context.userWalletId, network = context.network)
        }.getOrElse { error ->
            TangemLogger.e("Yield module address could not be derived for the claim gate", error)
            null
        }

        return derived?.takeIf(String::isNotBlank)
    }

    private companion object {
        const val HEX_PREFIX = "0x"
        const val METHOD_SELECTOR_HEX_LENGTH = 8
    }
}