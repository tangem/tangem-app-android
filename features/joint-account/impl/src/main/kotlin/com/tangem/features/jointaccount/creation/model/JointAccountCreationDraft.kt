package com.tangem.features.jointaccount.creation.model

import com.tangem.domain.models.account.CryptoPortfolioIcon
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Everything the creator has entered across the flow steps so far. The final step aggregates it into the
 * creation payload.
 *
 * Each step owns its chunk: going back and pressing Continue again simply overwrites that step's part.
 */
internal data class JointAccountCreationDraft(
    val config: Config? = null,
    val composition: Composition? = null,
    val displayName: String? = null,
) {

    /**
     * The config step.
     *
     * @property name raw input exactly as typed; never blank — the step's Continue is gated by account name validation
     */
    data class Config(
        val name: String,
        val icon: CryptoPortfolioIcon.Icon,
        val color: CryptoPortfolioIcon.Color,
        val walletId: UserWalletId,
    )

    /** The composition step: "Total members" (`m`) and "Required to sign" (`threshold`) */
    data class Composition(
        val totalMembers: Int,
        val requiredToSign: Int,
    )
}