package com.tangem.data.card

import com.tangem.TangemSdk
import com.tangem.blockchain.common.TransactionSigner
import com.tangem.common.UserCodeType
import com.tangem.common.core.CardIdDisplayFormat
import com.tangem.common.core.CardSession
import com.tangem.common.core.UserCodeRequestPolicy
import com.tangem.data.card.sdk.CardSdkProvider
import com.tangem.domain.card.models.TwinKey
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Implementation of repository for managing of CardSDK config
 *
 * @property cardSdkProvider       CardSDK instance provider
 *
[REDACTED_AUTHOR]
 */
internal class DefaultCardSdkConfigRepository(
    private val cardSdkProvider: CardSdkProvider,
    private val transactionSignerFactory: TransactionSignerFactory,
) : CardSdkConfigRepository {

    @Deprecated("Use CardSdkConfigRepository's methods instead of this property")
    override val sdk: TangemSdk
        get() = cardSdkProvider.sdk

    override var isBiometricsRequestPolicy: Boolean
        get() = cardSdkProvider.sdk.config.userCodeRequestPolicy is UserCodeRequestPolicy.AlwaysWithBiometrics
        set(value) {
            cardSdkProvider.sdk.config.userCodeRequestPolicy = value.toUserCodeRequestPolicy()
        }

    override val cardIdDisplayFormat: CardIdDisplayFormat
        get() = cardSdkProvider.sdk.config.cardIdDisplayFormat

    override fun restoreAccessCodeRequestPolicy(isBiometricsRequestPolicy: Boolean) {
        val config = cardSdkProvider.sdkOrNull?.config ?: return
        config.userCodeRequestPolicy = isBiometricsRequestPolicy.toUserCodeRequestPolicy()
    }

    override fun restoreCardIdDisplayFormat(format: CardIdDisplayFormat) {
        val config = cardSdkProvider.sdkOrNull?.config ?: return
        config.cardIdDisplayFormat = format
    }

    override fun setAccessCodeRequestPolicy(isBiometricsRequestPolicy: Boolean) {
        cardSdkProvider.sdk.config.userCodeRequestPolicy = isBiometricsRequestPolicy.toUserCodeRequestPolicy()
    }

    private fun Boolean.toUserCodeRequestPolicy(): UserCodeRequestPolicy = if (this) {
        UserCodeRequestPolicy.AlwaysWithBiometrics(codeType = UserCodeType.AccessCode)
    } else {
        UserCodeRequestPolicy.Default
    }

    override fun getCommonSigner(
        cardId: String?,
        twinKey: TwinKey?,
        userWalletId: UserWalletId,
        session: CardSession?,
    ): TransactionSigner {
        return transactionSignerFactory.createTransactionSigner(
            cardId = cardId,
            sdk = cardSdkProvider.sdk,
            twinKey = twinKey,
            userWalletId = userWalletId,
            session = session,
        )
    }

    override fun isLinkedTerminal() = cardSdkProvider.sdk.config.linkedTerminal

    override fun setLinkedTerminal(isLinked: Boolean?) {
        cardSdkProvider.sdk.config.linkedTerminal = isLinked
    }
}