package com.tangem.core.deeplink

/**
 * Represents a deep link.
 */
interface DeepLink {

    /**
     * ID of the deep link.
     *
     * By default, it is the same as the [uri].
     * */
    val id: String get() = uri

    /**
     * URI of the deep link.
     *
     * **Note: Remember to add the URI in the AndroidManifest.xml file in the `app` module.**
     *
     * Query parameters will be received automatically.
     *
     * Path parameters can be added using the following syntax:
     *  ```kotlin
     * "tangem://link" // Without parameters
     * "tangem://link/{param1}/{param2}" // With path parameters
     * ```
     * */
    val uri: String

    /**
     * Method to be called when this deep link is received.
     *
     * @param params Map of parameters received from the deep link.
     * */
    fun onReceive(params: Map<String, String>)
}