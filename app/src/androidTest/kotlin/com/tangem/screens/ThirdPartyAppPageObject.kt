package com.tangem.screens

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until
import com.kaspersky.kaspresso.screens.KScreen

object ThirdPartyAppPageObject : KScreen<ThirdPartyAppPageObject>() {

    override val layoutId: Int? = null
    override val viewClass: Class<*>? = null

    private val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    private const val TIMEOUT = 10000L

    fun assertChromeIsOpened() {
        val chromePackage = "com.android.chrome"
        device.wait(Until.hasObject(By.pkg(chromePackage).depth(0)), TIMEOUT)
        assert(device.hasObject(By.pkg(chromePackage))) {
            "Chrome browser is not opened"
        }
    }

    fun assertUrlContains(expectedUrl: String) {
        val urlBar = device.findObject(
            By.res("com.android.chrome:id/url_bar")
        )
        urlBar?.let {
            assert(it.text.contains(expectedUrl)) {
                "URL doesn't contain expected: $expectedUrl, actual: ${it.text}"
            }
        }
    }

    private fun findElementByText(text: String): UiObject2? {
        device.wait(Until.hasObject(By.text(text)), TIMEOUT)
        return device.findObject(By.text(text))
    }

    fun assertElementWithTextExists(text: String) {
        val element = findElementByText(text)
        assert(element != null) { "Element with text '$text' not found" }
    }

    fun isElementWithTextExists(text: String): Boolean {
        return device.wait(Until.hasObject(By.text(text)), TIMEOUT)
    }

    fun clickOnElementWithText(text: String) {
        findElementByText(text)?.click()
            ?: throw AssertionError("Cannot click - element with text '$text' not found")
    }
}