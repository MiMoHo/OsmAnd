package net.osmand.wear.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import net.osmand.wear.data.OsmAndDataLayerClient
import net.osmand.wear.model.NavigationState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class MainViewModel(
    private val dataLayerClient: OsmAndDataLayerClient
) : ViewModel() {

    var navState by mutableStateOf(NavigationState())
        private set

    init {
        dataLayerClient.getNavigationUpdates()
            .onEach { state ->
                navState = state
            }
            .launchIn(viewModelScope)
    }
}

class MainViewModelFactory(
    private val dataLayerClient: OsmAndDataLayerClient
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(dataLayerClient) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
