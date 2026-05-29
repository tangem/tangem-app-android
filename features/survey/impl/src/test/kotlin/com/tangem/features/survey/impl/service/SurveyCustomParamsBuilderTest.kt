package com.tangem.features.survey.impl.service

import com.google.common.truth.Truth.assertThat
import com.tangem.common.extensions.calculateSha256
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.core.analytics.AppInstanceIdProvider
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.utils.info.AppInfoProvider
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.Locale

internal class SurveyCustomParamsBuilderTest {

    private val appInstanceIdProvider = mockk<AppInstanceIdProvider>()
    private val appInfoProvider = mockk<AppInfoProvider>()

    private val builder = SurveyCustomParamsBuilder(
        appInstanceIdProvider = appInstanceIdProvider,
        appInfoProvider = appInfoProvider,
    )

    @BeforeEach
    fun setup() {
        Locale.setDefault(Locale.ENGLISH)
        every { appInfoProvider.platform } returns "Android"
        every { appInfoProvider.appVersion } returns "5.40"
        coEvery { appInstanceIdProvider.getAppInstanceId() } returns "device-123"
    }

    @Test
    fun `builds all params for a cold wallet`() = runTest {
        val wallet = coldWallet(WALLET_ID_HEX)

        val params = builder.build(userWallet = wallet, token = TOKEN, displayId = "42")

        assertThat(params).containsExactlyEntriesIn(
            mapOf(
                "survey_key" to TOKEN,
                "wallet_id" to expectedWalletIdHash(WALLET_ID_HEX),
                "wallet_type" to "cold",
                "display_id" to "42",
                "device_id" to "device-123",
                "platform" to "android",
                "app_version" to "5.40",
                "language" to "en",
            ),
        )
    }

    @Test
    fun `wallet_id hash is uppercase hex`() = runTest {
        val params = builder.build(userWallet = coldWallet(WALLET_ID_HEX), token = TOKEN, displayId = null)

        val walletId = params.getValue("wallet_id")
        assertThat(walletId).isEqualTo(walletId.uppercase())
        assertThat(walletId).matches("[0-9A-F]+")
    }

    @Test
    fun `wallet_type is hot for a hot wallet`() = runTest {
        val wallet = mockk<UserWallet.Hot> { every { walletId } returns UserWalletId(WALLET_ID_HEX) }

        val params = builder.build(userWallet = wallet, token = TOKEN, displayId = null)

        assertThat(params["wallet_type"]).isEqualTo("hot")
    }

    @Test
    fun `device_id is omitted when app instance id is null`() = runTest {
        coEvery { appInstanceIdProvider.getAppInstanceId() } returns null

        val params = builder.build(userWallet = coldWallet(WALLET_ID_HEX), token = TOKEN, displayId = "42")

        assertThat(params).doesNotContainKey("device_id")
    }

    @Test
    fun `display_id is omitted when null or blank`() = runTest {
        val nullCase = builder.build(userWallet = coldWallet(WALLET_ID_HEX), token = TOKEN, displayId = null)
        val blankCase = builder.build(userWallet = coldWallet(WALLET_ID_HEX), token = TOKEN, displayId = "  ")

        assertThat(nullCase).doesNotContainKey("display_id")
        assertThat(blankCase).doesNotContainKey("display_id")
    }

    private fun coldWallet(walletIdHex: String): UserWallet.Cold = mockk {
        every { walletId } returns UserWalletId(walletIdHex)
    }

    private fun expectedWalletIdHash(walletIdHex: String): String =
        walletIdHex.hexToBytes().calculateSha256().toHexString()

    private companion object {
        const val TOKEN = "ntt-84iF22PDajmervYneMW4kv"
        const val WALLET_ID_HEX = "0123456789ABCDEF"
    }
}