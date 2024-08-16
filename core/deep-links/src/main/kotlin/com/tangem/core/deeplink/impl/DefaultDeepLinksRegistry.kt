package com.tangem.core.deeplink.impl

import android.content.Intent
import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.tangem.core.deeplink.DeepLink
import com.tangem.core.deeplink.DeepLinksRegistry
import com.tangem.core.deeplink.utils.DeepLinksLifecycleObserver
import timber.log.Timber

internal class DefaultDeepLinksRegistry : DeepLinksRegistry {

    private var registries: List<DeepLink> = emptyList()

    override fun launch(intent: Intent): Boolean {
        val received = intent.data ?: return false
        var hasMatch = false

        Timber.i(
            """
                Received deep link intent
                |- Received URI: $received
                |- Registries: $registries
            """.trimIndent(),
        )
        registries.forEach { deepLink ->
            val expected = deepLink.uri.toUri()

            if (!isMatches(expected, received)) return@forEach
            hasMatch = true

            val params = getParams(expected, received)

            Timber.i(
                """
                    Matched deep link
                    |- Expected URI: $expected
                    |- Received URI: $received
                    |- Params: $params
                """.trimIndent(),
            )
            deepLink.onReceive(params)
        }

        if (!hasMatch) {
            Timber.i(
                """
                    No match found for deep link
                    |- Received URI: $received
                    |- Registries: $registries
                """.trimIndent(),
            )
        }

        return hasMatch
    }

    override fun register(deepLinks: Collection<DeepLink>) {
        registries = (registries + deepLinks).distinctBy(DeepLink::id)

        Timber.d(
            """
                Registered deep links
                |- Registries: $registries
            """.trimIndent(),
        )
    }

    override fun register(deepLink: DeepLink) {
        registries = (registries + deepLink).distinctBy(DeepLink::id)

        Timber.d(
            """
                Registered deep link
                |- Registries: $registries
            """.trimIndent(),
        )
    }

    override fun unregister(deepLinks: Collection<DeepLink>) {
        registries = registries.filter { it !in deepLinks }

        Timber.d(
            """
                Unregistered deep links
                |- Registries: $registries
            """.trimIndent(),
        )
    }

    override fun unregister(deepLink: DeepLink) {
        registries = registries.filter { it.id != deepLink.id }

        Timber.d(
            """
                Unregistered deep link
                |- Registries: $registries
            """.trimIndent(),
        )
    }

    override fun unregisterByIds(ids: Collection<String>) {
        registries = registries.filter { it.id !in ids }

        Timber.d(
            """
                Unregistered deep links
                |- Registries: $registries
            """.trimIndent(),
        )
    }

    override fun registerWithLifecycle(owner: LifecycleOwner, deepLinks: Collection<DeepLink>) {
        val observer = DeepLinksLifecycleObserver(deepLinksRegistry = this, deepLinks)
        owner.lifecycle.addObserver(observer)
    }

    override fun registerWithViewModel(viewModel: ViewModel, deepLinks: Collection<DeepLink>) {
        viewModel.addCloseable {
            unregister(deepLinks)
        }

        register(deepLinks)
    }

    private fun isMatches(received: Uri, expected: Uri): Boolean {
        if (received == expected) return true
        if (received.authority != expected.authority ||
            received.pathSegments.size != expected.pathSegments.size
        ) {
            return false
        }

        received.pathSegments.forEachIndexed { index, receivedSegment ->
            val expectedSegment = expected.pathSegments[index]
            if (receivedSegment != expectedSegment &&
                !(receivedSegment.startsWith(prefix = "{") && receivedSegment.endsWith(suffix = "}"))
            ) {
                return false
            }
        }

        return true
    }

    private fun getParams(received: Uri, expected: Uri): Map<String, String> {
        val params = mutableMapOf<String, String>()

        received.pathSegments.forEachIndexed { index, receivedSegment ->
            val expectedSegment = expected.pathSegments[index]
            if (receivedSegment != expectedSegment &&
                receivedSegment.startsWith(prefix = "{") &&
                receivedSegment.endsWith(suffix = "}")
            ) {
                val path = receivedSegment
                    .replace(oldValue = "{", newValue = "")
                    .replace(oldValue = "}", newValue = "")

                params[path] = expectedSegment
            }
        }

        expected.queryParameterNames.forEach { paramName ->
            expected.getQueryParameter(paramName)?.let { param ->
                params[paramName] = param
            }
        }

        return params
    }
}
