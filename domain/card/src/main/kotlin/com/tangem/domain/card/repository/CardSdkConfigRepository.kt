package com.tangem.domain.card.repository

import com.tangem.TangemSdk
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.domain.models.scan.ProductType

/**
 * Repository for managing with CardSDK config
 *
 * @author Andrew Khokhlov on 12/07/2023
 */
interface CardSdkConfigRepository {

    /** Tangem SDK instance */
    @Deprecated("Use CardSdkConfigRepository's methods instead of this property")
    val sdk: TangemSdk

    /** Set access code request policy by [isBiometricsRequestPolicy] */
    fun setAccessCodeRequestPolicy(isBiometricsRequestPolicy: Boolean)

    /** Reset the card ID display format to start value */
    fun resetCardIdDisplayFormat()

    /** Update the card ID display format according to the [productType] of the scanned card */
    fun updateCardIdDisplayFormat(productType: ProductType)

    /** Get common signer by [cardId] */
    fun getCommonSigner(cardId: String?): TransactionSigner

    /** Check if linked terminal is enabled */
    fun isLinkedTerminal(): Boolean?

    /** Set linked terminal by [isLinked] */
    fun setLinkedTerminal(isLinked: Boolean?)
}
