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
        get() = sdk.config.userCodeRequestPolicy is UserCodeRequestPolicy.AlwaysWithBiometrics
        set(value) {
            sdk.config.userCodeRequestPolicy = value.toUserCodeRequestPolicy()
        }

    override val cardIdDisplayFormat: CardIdDisplayFormat
        get() = sdk.config.cardIdDisplayFormat

    override fun restoreAccessCodeRequestPolicy(isBiometricsRequestPolicy: Boolean) {
        val config = cardSdkProvider.sdkOrNull?.config ?: return
        config.userCodeRequestPolicy = isBiometricsRequestPolicy.toUserCodeRequestPolicy()
    }

    override fun restoreCardIdDisplayFormat(format: CardIdDisplayFormat) {
        val config = cardSdkProvider.sdkOrNull?.config ?: return
        config.cardIdDisplayFormat = format
    }

    override fun setAccessCodeRequestPolicy(isBiometricsRequestPolicy: Boolean) {
        sdk.config.userCodeRequestPolicy = isBiometricsRequestPolicy.toUserCodeRequestPolicy()
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
            sdk = sdk,
            twinKey = twinKey,
            userWalletId = userWalletId,
            session = session,
        )
    }

    override fun isLinkedTerminal() = sdk.config.linkedTerminal

    override fun setLinkedTerminal(isLinked: Boolean?) {
        sdk.config.linkedTerminal = isLinked
    }
}