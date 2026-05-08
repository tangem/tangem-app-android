package com.tangem.tap.init

import com.tangem.datasource.utils.WireMockRedirectInterceptor

/**
 * Mocked-buildType variant: redirects all `wiremock.tests-d.com` traffic to the
 * local WireMock instance reachable via the Android emulator's host loopback alias
 * (`10.0.2.2`). Mirrors what `BaseTestCase.before {}` does for `androidTest` runs
 * (which receive the URL via `am instrument -e wiremockBaseUrl ...`); needed for
 * non-instrumentation launches such as Maestro, where `InstrumentationRegistry` is
 * empty.
 *
 * Port 8081 matches the `tangem-wiremock` docker container started by the e2e
 * workflows (`-p 8081:8080`).
 */
object WireMockOverride {
    fun apply() {
        WireMockRedirectInterceptor.overriddenBaseUrl = "http://10.0.2.2:8081"
    }
}