package com.tangem.features.marketing.impl.model

import com.tangem.common.routing.DeepLinkRoute
import com.tangem.common.routing.DeepLinkScheme
import com.tangem.common.routing.deeplink.OnrampDeeplinkTarget
import com.tangem.common.routing.deeplink.resolveOnrampDeeplinkTarget
import com.tangem.utils.extensions.uriValidate
import java.net.URI
import java.net.URLDecoder

/** How the button of a PSEUDO_PROVIDER / STANDALONE_CTA banner opens its campaign link. */
internal enum class CtaBannerLink {

    /** Routed by the deeplink launcher, as the links of the other banner types are */
    APP_LINK,

    /** An [APP_LINK] that opens the demand survey, so it is hidden once the wallet has answered it */
    CONCIERGE_WAITLIST,

    /**
     * A partner page. Partner names and links are configured on the backend and change without a release, so the page
     * skips the trusted-host check that other banner links go through.
     */
    PARTNER_PAGE,

    /** Nothing on Android opens it, so the banner is not shown rather than offering a button that does nothing */
    UNSUPPORTED,
}

/** Classifies a trimmed [link]. Never throws: malformed input is [CtaBannerLink.UNSUPPORTED]. */
internal fun resolveCtaBannerLink(link: String?, isSupportChatEnabled: Boolean): CtaBannerLink {
    val uri = link?.let { runCatching { URI(it) }.getOrNull() } ?: return CtaBannerLink.UNSUPPORTED

    return when (uri.scheme) {
        DeepLinkScheme.Https.scheme -> uri.resolveHttpsLink()
        DeepLinkScheme.Tangem.scheme -> uri.resolveTangemLink(isSupportChatEnabled)
        else -> CtaBannerLink.UNSUPPORTED
    }
}

// User info is rejected because parsers disagree on where it ends, see ExternalUrlValidator.isUriTrusted
private fun URI.resolveHttpsLink(): CtaBannerLink =
    if (userInfo == null && !host.isNullOrBlank()) CtaBannerLink.PARTNER_PAGE else CtaBannerLink.UNSUPPORTED

private fun URI.resolveTangemLink(isSupportChatEnabled: Boolean): CtaBannerLink =
    // A host with an underscore, e.g. token_chart, is not a valid URI host and is only in the authority
    when (val routeHost = host ?: rawAuthority) {
        null -> CtaBannerLink.UNSUPPORTED
        // The app routes the support chat link only behind this toggle, so the button would do nothing without it
        DeepLinkRoute.Support.host ->
            if (isSupportChatEnabled) CtaBannerLink.APP_LINK else CtaBannerLink.UNSUPPORTED
        DeepLinkRoute.Onramp.host ->
            if (isConciergeWaitlist()) CtaBannerLink.CONCIERGE_WAITLIST else CtaBannerLink.APP_LINK
        else -> if (routeHost.isBlank()) CtaBannerLink.UNSUPPORTED else CtaBannerLink.APP_LINK
    }

private fun URI.isConciergeWaitlist(): Boolean =
    resolveOnrampDeeplinkTarget(queryParams()) == OnrampDeeplinkTarget.ConciergeWaitlist

/** The params `DeepLinkFactory` hands the onramp handler: the first value of each, without malicious symbols */
private fun URI.queryParams(): Map<String, String> = firstQueryValues()
    .filter { (name, value) -> name.uriValidate() && value.uriValidate() }

private fun URI.firstQueryValues(): Map<String, String> = rawQuery.orEmpty()
    .split('&')
    .filter(String::isNotEmpty)
    .mapNotNull { pair ->
        val name = pair.substringBefore('=')
        val value = pair.substringAfter('=', missingDelimiterValue = "")
        runCatching { name.decodeQueryPart() to value.decodeQueryPart() }.getOrNull()
    }
    .distinctBy { (name, _) -> name }
    .toMap()

private fun String.decodeQueryPart(): String = URLDecoder.decode(this, Charsets.UTF_8.name())