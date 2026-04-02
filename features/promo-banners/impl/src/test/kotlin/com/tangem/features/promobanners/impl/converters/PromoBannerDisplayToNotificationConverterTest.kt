package com.tangem.features.promobanners.impl.converters

import com.tangem.core.ui.extensions.TextReference
import com.tangem.features.promobanners.impl.model.PromoBannerDisplay
import com.tangem.features.promobanners.impl.model.PromoBannerPriority
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class PromoBannerDisplayToNotificationConverterTest {

    private val converter = PromoBannerDisplayToNotificationConverter()

    @Test
    fun `should convert banner with all fields`() {
        val banner = createBanner(
            id = 1,
            deeplink = "tangem://wallet",
            isButtonEnabled = true,
            buttonText = "Open",
            isDismissable = true,
        )
        var clickedDeeplink: String? = "not_called"
        var dismissedId: Int? = null

        val result = converter.convert(
            banner = banner,
            onDeeplinkClick = { clickedDeeplink = it },
            onDismiss = { dismissedId = it },
        )

        assertThat(result.displayId).isEqualTo(1)
        assertThat(result.config.title).isEqualTo(TextReference.Str("Title"))
        assertThat(result.config.subtitle).isEqualTo(TextReference.Str("Subtitle"))
        assertThat(result.config.iconUrl).isEqualTo("https://icon.png")
        assertThat(result.config.onClick).isNull()
        assertThat(result.config.onCloseClick).isNotNull()
        assertThat(result.config.buttonsState).isNotNull()

        // Button click navigates via deeplink
        val buttonState = result.config.buttonsState as com.tangem.core.ui.components.notifications.NotificationConfig.ButtonsState.SecondaryButtonConfig
        buttonState.onClick.invoke()
        assertThat(clickedDeeplink).isEqualTo("tangem://wallet")

        result.config.onCloseClick!!.invoke()
        assertThat(dismissedId).isEqualTo(1)
    }

    @Test
    fun `should always have null onClick - banner itself is not clickable`() {
        val banner = createBanner(deeplink = "tangem://wallet")

        val result = converter.convert(
            banner = banner,
            onDeeplinkClick = {},
            onDismiss = {},
        )

        assertThat(result.config.onClick).isNull()
    }

    @Test
    fun `should not set onCloseClick when not dismissable`() {
        val banner = createBanner(isDismissable = false)

        val result = converter.convert(
            banner = banner,
            onDeeplinkClick = {},
            onDismiss = {},
        )

        assertThat(result.config.onCloseClick).isNull()
    }

    @Test
    fun `should not set button when button is disabled`() {
        val banner = createBanner(isButtonEnabled = false, buttonText = "Open")

        val result = converter.convert(
            banner = banner,
            onDeeplinkClick = {},
            onDismiss = {},
        )

        assertThat(result.config.buttonsState).isNull()
    }

    @Test
    fun `should not set button when buttonText is null`() {
        val banner = createBanner(isButtonEnabled = true, buttonText = null)

        val result = converter.convert(
            banner = banner,
            onDeeplinkClick = {},
            onDismiss = {},
        )

        assertThat(result.config.buttonsState).isNull()
    }

    private fun createBanner(
        id: Int = 0,
        deeplink: String? = "https://example.com",
        isButtonEnabled: Boolean = false,
        buttonText: String? = null,
        isDismissable: Boolean = true,
    ) = PromoBannerDisplay(
        id = id,
        placeholder = "main",
        priority = PromoBannerPriority.MEDIUM,
        title = "Title",
        subtitle = "Subtitle",
        iconUrl = "https://icon.png",
        deeplink = deeplink,
        isButtonEnabled = isButtonEnabled,
        buttonText = buttonText,
        isDismissable = isDismissable,
    )
}