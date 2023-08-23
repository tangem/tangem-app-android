package com.tangem.domain.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TwinsHelperTest {

    private val pack1Twins = listOf(
        "CB610000021",
        "CB620000031",
    )

    private val pack2Twins = listOf(
        "CB640000012",
        "CB650000011",
    )

    @Test
    fun `twins compatibility pack 1 success`() {
        assertTrue(TwinsHelper.isTwinsCompatible(pack1Twins[0], pack1Twins[1]))
        assertTrue(TwinsHelper.isTwinsCompatible(pack1Twins[1], pack1Twins[0]))
    }

    @Test
    fun `twins compatibility pack 1 same cards`() {
        assertFalse(TwinsHelper.isTwinsCompatible(pack1Twins[0], pack1Twins[0]))
        assertFalse(TwinsHelper.isTwinsCompatible(pack1Twins[1], pack1Twins[1]))
    }

    @Test
    fun `twins compatibility pack 2 success`() {
        assertTrue(TwinsHelper.isTwinsCompatible(pack2Twins[0], pack2Twins[1]))
        assertTrue(TwinsHelper.isTwinsCompatible(pack2Twins[1], pack2Twins[0]))
    }

    @Test
    fun `twins compatibility pack 2 same cards`() {
        assertFalse(TwinsHelper.isTwinsCompatible(pack2Twins[0], pack2Twins[0]))
        assertFalse(TwinsHelper.isTwinsCompatible(pack2Twins[1], pack2Twins[1]))
    }

    @Test
    fun `twins compatibility false for different packs`() {
        assertFalse(TwinsHelper.isTwinsCompatible(pack2Twins[0], pack1Twins[1]))
        assertFalse(TwinsHelper.isTwinsCompatible(pack1Twins[1], pack2Twins[0]))
        assertFalse(TwinsHelper.isTwinsCompatible(pack1Twins[0], pack2Twins[1]))
        assertFalse(TwinsHelper.isTwinsCompatible(pack2Twins[1], pack1Twins[0]))
    }
}