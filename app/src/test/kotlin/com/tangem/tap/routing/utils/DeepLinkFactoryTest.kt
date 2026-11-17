package com.tangem.tap.routing.utils

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import org.junit.jupiter.api.Test

internal class DeepLinkFactoryTest {

    private val scope = mockk<CoroutineScope>()
    private val deepLinkFactory = DeepLinkFactory()

    @Test
    fun `GIVEN deep link WHEN handleDeeplink THEN it is parked with its source and scope`() {
        // Arrange
        val uri = tangemUri(host = "main")

        // Act
        deepLinkFactory.handleDeeplink(uri, scope, isFromOnNewIntent = true, source = DeeplinkSource.Push)

        // Assert
        assertThat(deepLinkFactory.parkedDeepLink.value).isEqualTo(
            ParkedDeepLink(uri = uri, source = DeeplinkSource.Push, scope = scope, isFromOnNewIntent = true),
        )
    }

    @Test
    fun `GIVEN parked deep link WHEN another one arrives THEN it replaces the parked one`() {
        // Arrange
        val newUri = tangemUri(host = "main")
        deepLinkFactory.handleDeeplink(tangemUri(host = "main"), scope, isFromOnNewIntent = false)

        // Act
        deepLinkFactory.handleDeeplink(newUri, scope, isFromOnNewIntent = false)

        // Assert
        assertThat(deepLinkFactory.parkedDeepLink.value?.uri).isSameInstanceAs(newUri)
    }

    @Test
    fun `GIVEN parked deep link WHEN consume THEN it is taken once`() {
        // Arrange
        deepLinkFactory.handleDeeplink(tangemUri(host = "main"), scope, isFromOnNewIntent = false)
        val parked = requireNotNull(deepLinkFactory.parkedDeepLink.value)

        // Act
        val isFirstConsumed = deepLinkFactory.consume(parked)
        val isSecondConsumed = deepLinkFactory.consume(parked)

        // Assert
        assertThat(isFirstConsumed).isTrue()
        assertThat(isSecondConsumed).isFalse()
        assertThat(deepLinkFactory.parkedDeepLink.value).isNull()
    }

    @Test
    fun `GIVEN replaced deep link WHEN consume THEN the new one stays parked`() {
        // Arrange
        deepLinkFactory.handleDeeplink(tangemUri(host = "main"), scope, isFromOnNewIntent = false)
        val replaced = requireNotNull(deepLinkFactory.parkedDeepLink.value)
        deepLinkFactory.handleDeeplink(tangemUri(host = "main"), scope, isFromOnNewIntent = false)
        val parked = deepLinkFactory.parkedDeepLink.value

        // Act
        val isConsumed = deepLinkFactory.consume(replaced)

        // Assert
        assertThat(isConsumed).isFalse()
        assertThat(deepLinkFactory.parkedDeepLink.value).isSameInstanceAs(parked)
    }

    private fun tangemUri(host: String): Uri = mockk(relaxed = true) {
        every { scheme } returns "tangem"
        every { this@mockk.host } returns host
    }
}