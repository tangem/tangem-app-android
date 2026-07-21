package com.tangem.data.pay

/**
 * Test-only switch for the mocked Tangem Pay graph.
 *
 * Kept in the `main` source set (not `mocked`) on purpose: it is read only by the `mocked`-only
 * [com.tangem.data.pay.repository.MockAwareOnboardingRepository], but it is also written from `app`
 * `androidTest` sources, which are shared across build-type variants. Restricting it to the `mocked` source set
 * would leave it off the classpath of any non-mocked androidTest variant and break their compilation. Being in
 * `main`, it is unreferenced in production/release (only the mocked repository reads it) and is dropped by R8.
 *
 * [hasTangemPayInWallet] gates whether a wallet is treated as an existing Tangem Pay customer. It defaults to
 * `false` so that the many generic `openMainScreen*` UI tests keep a Payment-account-free wallet (and thus stay
 * out of accounts mode). Tangem Pay scenarios opt in by flipping it to `true` before the wallet is loaded; the
 * value is reset to the default at the start of every test in `BaseTestCase.setupHooks`.
 *
 * @see com.tangem.data.pay.repository.MockAwareOnboardingRepository
 */
object TangemPayMockControl {

    private const val DEFAULT_HAS_TANGEM_PAY_IN_WALLET = false

    @Volatile
    var hasTangemPayInWallet: Boolean = DEFAULT_HAS_TANGEM_PAY_IN_WALLET

    /** Restores every switch to its default. Called between tests to prevent state leaking across the process. */
    fun reset() {
        hasTangemPayInWallet = DEFAULT_HAS_TANGEM_PAY_IN_WALLET
    }
}