package com.tangem.domain.qrscanning

/**
 * Whether a scanned link is one the app also opens from outside (a screen or a Tangem web page), never a service link
 * that acts on data it carries, such as WalletConnect pairing or MCP authorization.
 */
interface QrDeepLinkMatcher {

    fun isDeepLink(link: String): Boolean
}