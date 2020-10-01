package com.tangem.merchant.common.toggleWidget

import android.view.View
import android.view.ViewGroup


/**
* [REDACTED_AUTHOR]
 */
interface ToggleState

interface StateModifier {
    fun stateChanged(container: ViewGroup, view: View, state: ToggleState)
}

interface ToggleView {
    val mainViewModifiers: MutableList<StateModifier>
    val toggleViewModifiers: MutableList<StateModifier>

    fun setState(state: ToggleState, andApply: Boolean = true)
    fun applyState()
    fun getView(): View
    fun getMainView(): View
    fun getToggleView(): View
}

class ToggleWidget : ToggleView {
    private val container: ViewGroup
    private val mainView: View
    private val toggleView: View

    private var state: ToggleState

    constructor(
        container: ViewGroup,
        mainView: View,
        toggleView: View,
        initialState: ToggleState,
        mainViewModifier: List<StateModifier> = mutableListOf(),
        loadingViewModifier: List<StateModifier> = mutableListOf()
    ) {
        this.container = container
        this.mainView = mainView
        this.toggleView = toggleView
        this.state = initialState
        this.mainViewModifiers.addAll(mainViewModifier)
        this.toggleViewModifiers.addAll(loadingViewModifier)
    }

    constructor(
        container: ViewGroup,
        mainViewId: Int,
        toggleViewId: Int,
        initialState: ToggleState,
        mainViewModifier: List<StateModifier> = mutableListOf(),
        loadingViewModifier: List<StateModifier> = mutableListOf()
    ) {
        this.container = container
        this.mainView = container.findViewById(mainViewId)
        this.toggleView = container.findViewById(toggleViewId)
        this.state = initialState
        this.mainViewModifiers.addAll(mainViewModifier)
        this.toggleViewModifiers.addAll(loadingViewModifier)
    }

    override val mainViewModifiers: MutableList<StateModifier> = mutableListOf()

    override val toggleViewModifiers: MutableList<StateModifier> = mutableListOf()

    override fun setState(state: ToggleState, andApply: Boolean) {
        this.state = state
        if (andApply) applyState()
    }

    override fun applyState() {
        mainViewModifiers.forEach { it.stateChanged(container, mainView, state) }
        toggleViewModifiers.forEach { it.stateChanged(container, toggleView, state) }
    }

    override fun getView(): View = container

    override fun getMainView(): View = mainView

    override fun getToggleView(): View = toggleView
}