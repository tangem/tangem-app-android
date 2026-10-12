package com.tangem.domain.card.repository

import com.tangem.TangemSdk
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.common.core.CardSession
import com.tangem.domain.card.models.TwinKey
import com.tangem.domain.models.scan.ProductType
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Repository for managing with CardSDK config
 *
[REDACTED_AUTHOR]
 */
interface CardSdkConfigRepository {

    /** Tangem SDK instance */
    @Deprecated("Use CardSdkConfigRepository's methods instead of this property")
    val sdk: TangemSdk

    /** Allow requesting access codes from biometrically protected storage */
    var isBiometricsRequestPolicy: Boolean

    /** Set access code request policy by [isBiometricsRequestPolicy] */
    fun setAccessCodeRequestPolicy(isBiometricsRequestPolicy: Boolean)

    /**
     * Restore the access code request policy when a screen that changed it goes away.
     *
     * Unlike [setAccessCodeRequestPolicy], does nothing without a registered SDK: the teardown also runs while the
     * activity is being destroyed, where asking for the instance re-registers against a dead one and throws.
     */
    fun restoreAccessCodeRequestPolicy(isBiometricsRequestPolicy: Boolean)

    /** Reset the card ID display format to start value */
    fun resetCardIdDisplayFormat()

    /** Update the card ID display format according to the [productType] of the scanned card */
    fun updateCardIdDisplayFormat(productType: ProductType)

    /**
     * Get common signer by [cardId].
     *
     * @param userWalletId ID of the user wallet being signed. Used to persist the updated number of signed hashes
     * back into the wallet after a successful signing operation.
     */
    fun getCommonSigner(
        cardId: String?,
        twinKey: TwinKey?,
        userWalletId: UserWalletId,
        session: CardSession? = null,
    ): TransactionSigner

    /** Check if linked terminal is enabled */
    fun isLinkedTerminal(): Boolean?

    /** Set linked terminal by [isLinked] */
    fun setLinkedTerminal(isLinked: Boolean?)
}