package com.tangem.scenarios

import android.net.Uri
import com.tangem.common.BaseTestCase
import com.tangem.common.constants.TestConstants.SVS_SEED_PHRASE_12
import com.tangem.common.constants.TestConstants.WAIT_UNTIL_TIMEOUT_LONG
import com.tangem.common.extensions.clickWithAssertion
import com.tangem.common.utils.findWireMockRequestBodies
import com.tangem.common.utils.findWireMockRequestHeaders
import com.tangem.common.utils.findWireMockRequestTimestamps
import com.tangem.common.utils.findWireMockRequestUrls
import com.tangem.screens.onMarketsScreen
import com.tangem.screens.onPolymarketMainScreen
import com.tangem.screens.onPolymarketOnboardingScreen
import com.tangem.screens.onSwapStoriesScreen
import io.github.kakaocup.kakao.common.utilities.getResourceString
import io.qameta.allure.kotlin.Allure.step
import com.tangem.core.res.R as CoreResR
import org.json.JSONObject
import org.junit.Assert

internal const val POLYMARKET_WALLET_SCENARIO = "polymarket_wallet"
internal const val POLYMARKET_GEOBLOCK_SCENARIO = "polymarket_geoblock"
internal const val POLYMARKET_CREDENTIALS_SCENARIO = "polymarket_clob_credentials"
internal const val POLYMARKET_BALANCE_SCENARIO = "polymarket_clob_balance"
internal const val POLYMARKET_BRIDGE_SCENARIO = "polymarket_bridge"
internal const val POLYMARKET_FEED_SCENARIO = "polymarket_feed"
internal const val POLYMARKET_STORIES_SCENARIO = "stories_polymarket_onboarding"
internal const val POLYMARKET_AB_TEST_PROPERTY = "uitest.abtests.twi_1576_polymarket"
internal const val POLYMARKET_AB_TEST_ON = "on"

internal const val POLYMARKET_WALLET_PATH = "/api/predictions/v1/wallet"
internal const val POLYMARKET_GEOBLOCK_PATH = "/polymarket.com/api/geoblock"
internal const val POLYMARKET_DEPLOY_PATH = "/api/predictions/v1/wallet/deploy"
internal const val POLYMARKET_APPROVALS_PATH = "/api/predictions/v1/wallet/approvals"
internal const val POLYMARKET_DERIVE_KEY_PATH = "/clob.polymarket.com/auth/derive-api-key"
internal const val POLYMARKET_CREATE_KEY_PATH = "/clob.polymarket.com/auth/api-key"
internal const val POLYMARKET_RELAYER_NONCE_PATH = "/relayer-v2.polymarket.com/nonce"
internal const val POLYMARKET_BRIDGE_DEPOSIT_PATH = "/bridge.polymarket.com/deposit"
internal const val POLYMARKET_BALANCE_PATH = "/clob.polymarket.com/balance-allowance"

internal const val POLYMARKET_WALLET_ID = "916A3887E37D720A7D8FBC8CD08EF4B1793E40B70C30D81259BB6E858B9FBA24"
internal const val POLYMARKET_OWNER_ADDRESS = "0x7E5F4552091A69125d5DfCb7b8C2659029395Bdf"
internal const val POLYMARKET_DEPOSIT_WALLET = "0x6F908B3B67b9F9C40413775fFf48b286aeF9a081"
internal const val POLYMARKET_DERIVED_API_KEY = "00000000-0000-4000-8000-000000000001"
internal const val POLYMARKET_CREATED_API_KEY = "00000000-0000-4000-8000-000000000002"
internal const val POLYMARKET_API_PASSPHRASE = "mock-polymarket-passphrase"
internal const val POLYMARKET_RELAYER_NONCE = "7"
private const val POLYMARKET_APPROVE_SELECTOR = "0x095ea7b3"
private const val POLYMARKET_SET_APPROVAL_FOR_ALL_SELECTOR = "0xa22cb465"
private const val POLYMARKET_SELECTOR_LENGTH = 10
private const val POLYMARKET_WORD_HEX_LENGTH = 64
private const val POLYMARKET_APPROVE_DATA_LENGTH = POLYMARKET_SELECTOR_LENGTH + 2 * POLYMARKET_WORD_HEX_LENGTH
private const val POLYMARKET_ADDRESS_HEX_LENGTH = 40
private const val POLYMARKET_MAX_UINT256_WORD = "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff"
private const val POLYMARKET_TRUE_WORD = "0000000000000000000000000000000000000000000000000000000000000001"
private const val POLYMARKET_COLLATERAL = "0xC011a7E12a19f7B1f670d46F03B03f3342E82DFB"
private const val POLYMARKET_CONDITIONAL_TOKENS = "0x4D97DCd97eC945f40cF65F87097ACe5EA0476045"
private const val POLYMARKET_POSITION_MANAGER = "0x006F54F7f9A22e0000CC2AB60031000000ae9fEF"

private data class PolymarketApprovalCallTriple(val target: String, val selector: String, val spender: String)

private fun approvalCall(selector: String, target: String, spender: String) =
    PolymarketApprovalCallTriple(target.lowercase(), selector, spender.lowercase())

private val POLYMARKET_EXPECTED_APPROVAL_CALLS = setOf(
    approvalCall(POLYMARKET_APPROVE_SELECTOR, POLYMARKET_COLLATERAL, "0x4D97DCd97eC945f40cF65F87097ACe5EA0476045"),
    approvalCall(POLYMARKET_APPROVE_SELECTOR, POLYMARKET_COLLATERAL, "0xE111180000d2663C0091e4f400237545B87B996B"),
    approvalCall(POLYMARKET_APPROVE_SELECTOR, POLYMARKET_COLLATERAL, "0xe2222d279d744050d28e00520010520000310F59"),
    approvalCall(POLYMARKET_APPROVE_SELECTOR, POLYMARKET_COLLATERAL, "0xd91E80cF2E7be2e162c6513ceD06f1dD0dA35296"),
    approvalCall(POLYMARKET_APPROVE_SELECTOR, POLYMARKET_COLLATERAL, "0xAdA100Db00Ca00073811820692005400218FcE1f"),
    approvalCall(POLYMARKET_APPROVE_SELECTOR, POLYMARKET_COLLATERAL, "0xadA2005600Dec949baf300f4C6120000bDB6eAab"),
    approvalCall(POLYMARKET_APPROVE_SELECTOR, POLYMARKET_COLLATERAL, "0xe3333700cA9d93003F00f0F71f8515005F6c00Aa"),
    approvalCall(POLYMARKET_APPROVE_SELECTOR, POLYMARKET_COLLATERAL, "0x12121212006e4CD160D18e3f00711DA5c3372600"),
    approvalCall(
        POLYMARKET_SET_APPROVAL_FOR_ALL_SELECTOR, POLYMARKET_CONDITIONAL_TOKENS,
        "0xE111180000d2663C0091e4f400237545B87B996B",
    ),
    approvalCall(
        POLYMARKET_SET_APPROVAL_FOR_ALL_SELECTOR, POLYMARKET_CONDITIONAL_TOKENS,
        "0xe2222d279d744050d28e00520010520000310F59",
    ),
    approvalCall(
        POLYMARKET_SET_APPROVAL_FOR_ALL_SELECTOR, POLYMARKET_CONDITIONAL_TOKENS,
        "0xd91E80cF2E7be2e162c6513ceD06f1dD0dA35296",
    ),
    approvalCall(
        POLYMARKET_SET_APPROVAL_FOR_ALL_SELECTOR, POLYMARKET_CONDITIONAL_TOKENS,
        "0xAdA100Db00Ca00073811820692005400218FcE1f",
    ),
    approvalCall(
        POLYMARKET_SET_APPROVAL_FOR_ALL_SELECTOR, POLYMARKET_CONDITIONAL_TOKENS,
        "0xadA2005600Dec949baf300f4C6120000bDB6eAab",
    ),
    approvalCall(
        POLYMARKET_SET_APPROVAL_FOR_ALL_SELECTOR, POLYMARKET_CONDITIONAL_TOKENS,
        "0x1000008dD9001B968442c1000017eaE6E0dA00Ba",
    ),
    approvalCall(
        POLYMARKET_SET_APPROVAL_FOR_ALL_SELECTOR, POLYMARKET_CONDITIONAL_TOKENS,
        "0x200000900045e3B6259600682756002200028933",
    ),
    approvalCall(
        POLYMARKET_SET_APPROVAL_FOR_ALL_SELECTOR, POLYMARKET_POSITION_MANAGER,
        "0xe3333700cA9d93003F00f0F71f8515005F6c00Aa",
    ),
    approvalCall(
        POLYMARKET_SET_APPROVAL_FOR_ALL_SELECTOR, POLYMARKET_POSITION_MANAGER,
        "0x12121212006e4CD160D18e3f00711DA5c3372600",
    ),
)
internal const val POLYMARKET_APPROVALS_DEADLINE_SECONDS = 600L

fun BaseTestCase.openMainScreenWithPolymarketHotWallet() {
    openMainScreenWithExistingHotWallet(SVS_SEED_PHRASE_12)
}

fun BaseTestCase.openPolymarketFromMarketsBanner() {
    expandFeed()
    awaitSuccess { onMarketsScreen { predictionsBanner.assertIsDisplayed() } }
    onMarketsScreen { predictionsBanner.performClick() }
}

fun BaseTestCase.clickPolymarketOnboardingActionButton() {
    awaitSuccess { onPolymarketOnboardingScreen { actionButton.assertIsDisplayed() } }
    onPolymarketOnboardingScreen { actionButton.clickWithAssertion() }
}

fun BaseTestCase.awaitPolymarketMainScreen() {
    awaitSuccess(timeoutMillis = WAIT_UNTIL_TIMEOUT_LONG) { onPolymarketMainScreen { screen.assertIsDisplayed() } }
}

fun BaseTestCase.assertPolymarketDeployRequestMatchesWallet(deployRequestsBefore: Int) {
    val deployBodies = findWireMockRequestBodies(method = "POST", urlPath = POLYMARKET_DEPLOY_PATH)
        .drop(deployRequestsBefore)
    Assert.assertEquals("Expected exactly one deploy request", 1, deployBodies.size)
    val actual = JSONObject(deployBodies.single())
    val expected = mapOf(
        "ownerAddress" to POLYMARKET_OWNER_ADDRESS,
        "walletId" to POLYMARKET_WALLET_ID,
        "depositWalletAddress" to POLYMARKET_DEPOSIT_WALLET,
    )
    Assert.assertEquals(expected, actual.keys().asSequence().associateWith { actual.getString(it) })
}

fun BaseTestCase.checkPolymarketOnboardingStories() {
    step("Assert 'Stories' screen is displayed") {
        awaitSuccess { onSwapStoriesScreen { container.assertIsDisplayed() } }
    }
    step("Assert the first story content is displayed") {
        checkStoriesContent(
            storyIndex = 0,
            storyTitle = getResourceString(CoreResR.string.prediction_onboarding_story_first_title),
            storySubtitle = getResourceString(CoreResR.string.prediction_onboarding_story_first_subtitle),
        )
    }
    step("Assert progress bar has the fourth item") {
        onSwapStoriesScreen { progressBarItem(POLYMARKET_STORIES_COUNT - 1).assertIsDisplayed() }
    }
    step("Assert progress bar has no fifth item") {
        onSwapStoriesScreen { progressBarItem(POLYMARKET_STORIES_COUNT).assertDoesNotExist() }
    }
    step("Click on 'Close' button") {
        onSwapStoriesScreen { closeButton.performClick() }
    }
}

fun BaseTestCase.assertPolymarketApprovalsRequestMatchesWallet(approvalsRequestsBefore: Int) {
    val approvalsBodies = findWireMockRequestBodies(method = "POST", urlPath = POLYMARKET_APPROVALS_PATH)
        .drop(approvalsRequestsBefore)
    Assert.assertEquals("Expected exactly one approvals request", 1, approvalsBodies.size)
    val actual = JSONObject(approvalsBodies.single())
    Assert.assertEquals(POLYMARKET_OWNER_ADDRESS, actual.getString("ownerAddress"))
    Assert.assertEquals(POLYMARKET_DEPOSIT_WALLET, actual.getString("depositWalletAddress"))
}

fun BaseTestCase.assertPolymarketRelayerNonceRequested(nonceRequestsBefore: Int) {
    val urls = findWireMockRequestUrls("GET", POLYMARKET_RELAYER_NONCE_PATH).drop(nonceRequestsBefore)
    Assert.assertEquals("Expected one nonce request", 1, urls.size)
    val url = Uri.parse(urls.single())
    Assert.assertEquals(POLYMARKET_OWNER_ADDRESS.lowercase(), url.getQueryParameter("address").orEmpty().lowercase())
    Assert.assertEquals("WALLET", url.getQueryParameter("type"))
}

fun BaseTestCase.assertPolymarketApprovalsNonce(approvalsRequestsBefore: Int) {
    val nonce = singlePolymarketApprovalsRequest(approvalsRequestsBefore).getString("nonce")
    Assert.assertEquals(POLYMARKET_RELAYER_NONCE, nonce)
}

fun BaseTestCase.assertPolymarketApprovalsDeadline(
    approvalsRequestsBefore: Int,
    signedNotBeforeSeconds: Long,
    signedNotAfterSeconds: Long,
) {
    val deadline = singlePolymarketApprovalsRequest(approvalsRequestsBefore).getString("deadline").toLong()
    Assert.assertTrue(
        "Deadline $deadline must be ${POLYMARKET_APPROVALS_DEADLINE_SECONDS}s after signing",
        deadline in (signedNotBeforeSeconds + POLYMARKET_APPROVALS_DEADLINE_SECONDS)..
            (signedNotAfterSeconds + POLYMARKET_APPROVALS_DEADLINE_SECONDS),
    )
}

fun BaseTestCase.assertPolymarketApprovalsCalls(approvalsRequestsBefore: Int) {
    val calls = singlePolymarketApprovalsRequest(approvalsRequestsBefore).getJSONArray("calls")
    val actual = List(calls.length()) { index ->
        val call = calls.getJSONObject(index)
        val data = call.getString("data")
        Assert.assertEquals("Call $index value", "0", call.getString("value"))
        val selector = data.take(POLYMARKET_SELECTOR_LENGTH)
        val amountWord = data.drop(POLYMARKET_APPROVE_DATA_LENGTH - POLYMARKET_WORD_HEX_LENGTH)
        val expectedWord = if (selector == POLYMARKET_APPROVE_SELECTOR) {
            POLYMARKET_MAX_UINT256_WORD
        } else {
            POLYMARKET_TRUE_WORD
        }
        Assert.assertEquals("Call $index data length", POLYMARKET_APPROVE_DATA_LENGTH, data.length)
        Assert.assertEquals("Call $index amount or flag", expectedWord, amountWord)
        val spenderWord = data.substring(
            POLYMARKET_SELECTOR_LENGTH,
            POLYMARKET_SELECTOR_LENGTH + POLYMARKET_WORD_HEX_LENGTH,
        )
        PolymarketApprovalCallTriple(
            target = call.getString("target").lowercase(),
            selector = selector,
            spender = "0x" + spenderWord.takeLast(POLYMARKET_ADDRESS_HEX_LENGTH),
        )
    }
    Assert.assertEquals(POLYMARKET_EXPECTED_APPROVAL_CALLS.size, actual.size)
    Assert.assertEquals(POLYMARKET_EXPECTED_APPROVAL_CALLS, actual.toSet())
}

fun BaseTestCase.assertPolymarketApprovalsSignature(approvalsRequestsBefore: Int) {
    val signature = singlePolymarketApprovalsRequest(approvalsRequestsBefore).getString("signature")
    Assert.assertTrue(
        "Batch signature must be a hex string, was $signature",
        Regex("0x[0-9a-fA-F]{130}").matches(signature),
    )
}

private fun singlePolymarketApprovalsRequest(approvalsRequestsBefore: Int): JSONObject {
    val bodies = findWireMockRequestBodies(method = "POST", urlPath = POLYMARKET_APPROVALS_PATH)
        .drop(approvalsRequestsBefore)
    Assert.assertEquals("Expected exactly one approvals request", 1, bodies.size)
    return JSONObject(bodies.single())
}

fun BaseTestCase.assertPolymarketL1HeadersSent(method: String, urlPath: String, requestsBefore: Int) {
    val headers = findWireMockRequestHeaders(method = method, urlPath = urlPath).drop(requestsBefore).first()
    Assert.assertEquals(POLYMARKET_OWNER_ADDRESS.lowercase(), headers["poly_address"].orEmpty().lowercase())
    Assert.assertTrue(
        "L1 signature must be a hex string, was ${headers["poly_signature"]}",
        Regex("0x[0-9a-fA-F]{130}").matches(headers["poly_signature"].orEmpty()),
    )
    val timestamp = headers["poly_timestamp"].orEmpty()
    Assert.assertTrue(
        "L1 timestamp must be numeric, was $timestamp",
        timestamp.isNotEmpty() && timestamp.all { it.isDigit() },
    )
    Assert.assertTrue("L1 nonce must be present", headers["poly_nonce"] != null)
}

fun BaseTestCase.assertPolymarketL2CredentialsSent(apiKey: String, balanceRequestsBefore: Int = 0) {
    val headers = findWireMockRequestHeaders(method = "GET", urlPath = POLYMARKET_BALANCE_PATH)
        .drop(balanceRequestsBefore)
        .last()
    Assert.assertEquals(apiKey, headers["poly_api_key"])
    Assert.assertEquals(POLYMARKET_API_PASSPHRASE, headers["poly_passphrase"])
    Assert.assertTrue("L2 signature must be present", headers["poly_signature"].orEmpty().isNotEmpty())
}

fun BaseTestCase.assertPolymarketRequestOrder(
    firstMethod: String,
    firstPath: String,
    firstRequestsBefore: Int,
    secondMethod: String,
    secondPath: String,
    secondRequestsBefore: Int,
) {
    val first = findWireMockRequestTimestamps(firstMethod, firstPath).drop(firstRequestsBefore).first()
    val second = findWireMockRequestTimestamps(secondMethod, secondPath).drop(secondRequestsBefore).first()
    Assert.assertTrue("Expected $firstMethod $firstPath before $secondMethod $secondPath", first <= second)
}

private const val POLYMARKET_STORIES_COUNT = 4