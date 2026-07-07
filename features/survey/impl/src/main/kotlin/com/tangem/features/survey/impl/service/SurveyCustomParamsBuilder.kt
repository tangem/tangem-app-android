package com.tangem.features.survey.impl.service

import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.core.analytics.AppInstanceIdProvider
import com.tangem.datasource.api.tangemTech.models.WalletType
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.utils.SupportedLanguages
import com.tangem.utils.info.AppInfoProvider
import javax.inject.Inject

internal class SurveyCustomParamsBuilder @Inject constructor(
    private val appInstanceIdProvider: AppInstanceIdProvider,
    private val appInfoProvider: AppInfoProvider,
) {

    suspend fun build(userWallet: UserWallet, token: String, displayId: String?): Map<String, String> {
        return buildMap {
            put(KEY_SURVEY_KEY, token)
            put(KEY_WALLET_ID, hashWalletId(userWallet))
            WalletType.from(userWallet)?.let { put(KEY_WALLET_TYPE, it.name.lowercase()) }
            displayId?.takeIf { it.isNotBlank() }?.let { put(KEY_DISPLAY_ID, it) }
            appInstanceIdProvider.getAppInstanceId()?.let { put(KEY_DEVICE_ID, it) }
            put(KEY_PLATFORM, appInfoProvider.platform.lowercase())
            put(KEY_APP_VERSION, appInfoProvider.appVersion)
            put(KEY_LANGUAGE, SupportedLanguages.getCurrentSupportedLanguageCode())
        }
    }

    private fun hashWalletId(userWallet: UserWallet): String {
        return userWallet.walletId.stringValue
            .hexToBytes()
            .calculateSha256()
            .toHexString()
    }

    private companion object {
        const val KEY_SURVEY_KEY = "survey_key"
        const val KEY_WALLET_ID = "wallet_id"
        const val KEY_WALLET_TYPE = "wallet_type"
        const val KEY_DISPLAY_ID = "display_id"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_PLATFORM = "platform"
        const val KEY_APP_VERSION = "app_version"
        const val KEY_LANGUAGE = "language"
    }
}