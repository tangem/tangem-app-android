package com.tangem.data.card

import com.tangem.TangemSdk
import com.tangem.common.UserCodeType
import com.tangem.common.core.CardIdDisplayFormat
import com.tangem.common.core.UserCodeRequestPolicy
import com.tangem.data.card.sdk.CardSdkProvider
import com.tangem.data.source.preferences.PreferencesDataSource
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.models.scan.ProductType

/**
 * Implementation of repository for managing of CardSDK config
 *
 * @property cardSdkProvider       CardSDK instance provider
 * @property preferencesDataSource application shared preferences
 *
[REDACTED_AUTHOR]
 */
internal class DefaultCardSdkConfigRepository(
    private val cardSdkProvider: CardSdkProvider,
    private val preferencesDataSource: PreferencesDataSource,
) : CardSdkConfigRepository {

    @Deprecated("Use CardSdkConfigRepository's methods instead of this property")
    override val sdk: TangemSdk
        get() = cardSdkProvider.sdk

    override fun setAccessCodeRequestPolicy(isBiometricsRequestPolicy: Boolean) {
        sdk.config.userCodeRequestPolicy = if (isBiometricsRequestPolicy) {
            UserCodeRequestPolicy.AlwaysWithBiometrics(codeType = UserCodeType.AccessCode)
        } else {
            UserCodeRequestPolicy.Default
        }
    }

    override fun isBiometricsRequestPolicy(): Boolean {
        return with(sdk.config.userCodeRequestPolicy) {
            this is UserCodeRequestPolicy.AlwaysWithBiometrics && codeType == UserCodeType.AccessCode
        }
    }

    override fun resetCardIdDisplayFormat() {
        sdk.config.cardIdDisplayFormat = CardIdDisplayFormat.Full
    }

    override fun updateCardIdDisplayFormat(productType: ProductType) {
        sdk.config.cardIdDisplayFormat = when (productType) {
            ProductType.Twins -> CardIdDisplayFormat.LastLuhn(numbers = 4)
            ProductType.Note,
            ProductType.Wallet,
            ProductType.Start2Coin,
            -> CardIdDisplayFormat.Full
        }
    }

    override fun isAccessCodeSavingEnabled(): Boolean = preferencesDataSource.shouldSaveAccessCodes
}