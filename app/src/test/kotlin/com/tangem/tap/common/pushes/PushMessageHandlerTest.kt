package com.tangem.tap.common.pushes

import android.net.Uri
import com.tangem.common.routing.deeplink.DeeplinkConst.CUSTOMER_WALLET_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.DEEPLINK_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.NETWORK_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TOKEN_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.TYPE_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.WALLET_ID_KEY
import com.tangem.common.routing.deeplink.DeeplinkConst.WEBLINK_KEY
import com.tangem.common.routing.deeplink.PayloadToDeeplinkConverter
import com.tangem.test.core.ProvideTestModels
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.net.URI

/**
 * Characterization of the foreground push path.
 *
 * A `link` payload reaching this class at all is new: before it was routed like `deeplink`, the converter
 * returned `null` for one and `onMessageReceived` bailed out. These cases pin both that addition and the
 * pre-existing flat-key reaction it must not have displaced.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class PushMessageHandlerTest {

    private val tokenDetailsPushHandler: TokenDetailsPushHandler = mockk(relaxed = true)

    private val handler = PushMessageHandler(tokenDetailsPushHandler = tokenDetailsPushHandler)

    /**
     * `Uri.parse` is not available on the JVM, so it is redirected to a [URI]-based parser. The authority is read
     * instead of the host because hosts such as `redirect_sell` contain an underscore, which [URI] refuses to
     * expose as a host.
     *
     * The fakes are built up front rather than inside the `answers` block: creating a mock while MockK is
     * answering another one corrupts its recording state. [PayloadToDeeplinkConverter] is pure, so the exact set
     * of URLs the cases produce can be computed here without touching Android.
     */
    @BeforeAll
    fun mockUriParsing() {
        val urisByString = provideTestModels()
            .mapNotNull { PayloadToDeeplinkConverter.convert(it.payload) }
            .distinct()
            .associateWith(::fakeUri)

        mockkStatic(Uri::class)
        every { Uri.parse(any()) } answers { urisByString.getValue(firstArg()) }
    }

    @AfterAll
    fun unmockUriParsing() {
        unmockkStatic(Uri::class)
    }

    @BeforeEach
    fun resetMocks() {
        clearMocks(tokenDetailsPushHandler)
    }

    @ParameterizedTest
    @ProvideTestModels
    fun onMessageReceived(model: Model) {
        // Act
        handler.onMessageReceived(model.payload)

        // Assert
        val expectedParams = model.expectedTokenParams
        if (expectedParams != null) {
            verify(exactly = 1) { tokenDetailsPushHandler.handle(expectedParams) }
        } else {
            verify(inverse = true) { tokenDetailsPushHandler.handle(any()) }
        }
    }

    data class Model(val payload: Map<String, String>, val expectedTokenParams: Map<String, String>?)

    private fun provideTestModels() = listOf(
        // The token flat keys the backend sends — the reaction that existed before `link` was routed
        Model(
            payload = mapOf(
                TYPE_KEY to "token",
                NETWORK_ID_KEY to "ethereum",
                TOKEN_ID_KEY to "0x123",
                WALLET_ID_KEY to "wallet123",
            ),
            expectedTokenParams = mapOf(
                NETWORK_ID_KEY to "ethereum",
                TOKEN_ID_KEY to "0x123",
                TYPE_KEY to "token",
                WALLET_ID_KEY to "wallet123",
            ),
        ),
        // A ready-made deeplink still reacts the same way
        Model(
            payload = mapOf(DEEPLINK_KEY to "tangem://token?network_id=ethereum&token_id=0x123"),
            expectedTokenParams = mapOf(NETWORK_ID_KEY to "ethereum", TOKEN_ID_KEY to "0x123"),
        ),
        // New: a `link` pointing at a routable host reacts like a `deeplink`
        Model(
            payload = mapOf(WEBLINK_KEY to "tangem://token?network_id=ethereum&token_id=0x123"),
            expectedTokenParams = mapOf(NETWORK_ID_KEY to "ethereum", TOKEN_ID_KEY to "0x123"),
        ),
        // A web page has no in-app reaction — a background push must not open a browser
        Model(payload = mapOf(WEBLINK_KEY to "https://tangem.com/pricing/"), expectedTokenParams = null),
        // Another routable host has no reaction in this path either
        Model(payload = mapOf(WEBLINK_KEY to "tangem://main"), expectedTokenParams = null),
        // Blocked by PushDeeplinkPolicy before any routing happens
        Model(payload = mapOf(WEBLINK_KEY to "tangem://wc?uri=wc:topic@2"), expectedTokenParams = null),
        Model(payload = mapOf(DEEPLINK_KEY to "tangem://redirect_sell?transactionId=1"), expectedTokenParams = null),
        // Tangem Pay keys win over `link` and route to their own host, so no token reaction
        Model(
            payload = mapOf(
                TYPE_KEY to "card_ready",
                CUSTOMER_WALLET_ID_KEY to "wallet123",
                WEBLINK_KEY to "tangem://token?network_id=ethereum&token_id=0x123",
            ),
            expectedTokenParams = null,
        ),
        // Nothing to convert
        Model(payload = emptyMap(), expectedTokenParams = null),
        Model(payload = mapOf("title" to "Hi", "body" to "There"), expectedTokenParams = null),
    )

    private fun fakeUri(url: String): Uri {
        val parsed = URI.create(url)
        val queryParams = parsed.rawQuery.orEmpty()
            .split("&")
            .filter(String::isNotBlank)
            .associate { param -> param.substringBefore("=") to param.substringAfter("=", missingDelimiterValue = "") }

        return mockk {
            every { scheme } returns parsed.scheme
            every { host } returns parsed.authority
            every { queryParameterNames } returns queryParams.keys
            every { getQueryParameter(any()) } answers { queryParams[firstArg()] }
        }
    }
}