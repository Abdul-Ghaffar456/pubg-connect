package com.pubgconnect.detection

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object SimulationController {
    private val _isSimulatedPubgActive = MutableStateFlow(false)
    val isSimulatedPubgActive: StateFlow<Boolean> = _isSimulatedPubgActive.asStateFlow()

    fun setSimulatedState(active: Boolean) {
        _isSimulatedPubgActive.value = active
    }

    fun toggleSimulatedState() {
        _isSimulatedPubgActive.value = !_isSimulatedPubgActive.value
    }
}
