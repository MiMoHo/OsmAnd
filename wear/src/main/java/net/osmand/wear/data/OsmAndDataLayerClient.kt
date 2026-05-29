package net.osmand.wear.data

import android.content.Context
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import net.osmand.wear.model.NavigationState
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class OsmAndDataLayerClient(context: Context) {
    private val dataClient: DataClient = Wearable.getDataClient(context)

    fun getNavigationUpdates(): Flow<NavigationState> = callbackFlow {
        val listener = DataClient.OnDataChangedListener { dataEvents ->
            for (event in dataEvents) {
                if (event.dataItem.uri.path == "/osmand/navigation") {
                    val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                    
                    val isActive = dataMap.getBoolean("isActive", false)
                    val distance = dataMap.getString("distance", "")
                    val turnType = dataMap.getInt("turnType", 0)
                    val isLeftSide = dataMap.getBoolean("isLeftSide", false)
                    
                    val iconRes = mapTurnToIcon(turnType, isLeftSide)

                    trySend(NavigationState(
                        isActive = isActive,
                        nextTurnIconRes = iconRes,
                        distanceToNextTurn = distance,
                        instructionText = mapTurnToString(turnType)
                    ))
                }
            }
        }
        
        // Fetch current initial data when the Flow starts (so UI syncs immediately if navigation is active)
        dataClient.dataItems.addOnSuccessListener { dataItems ->
            for (dataItem in dataItems) {
                if (dataItem.uri.path == "/osmand/navigation") {
                    val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                    val isActive = dataMap.getBoolean("isActive", false)
                    val distance = dataMap.getString("distance", "")
                    val turnType = dataMap.getInt("turnType", 0)
                    val isLeftSide = dataMap.getBoolean("isLeftSide", false)
                    
                    val iconRes = mapTurnToIcon(turnType, isLeftSide)

                    trySend(NavigationState(
                        isActive = isActive,
                        nextTurnIconRes = iconRes,
                        distanceToNextTurn = distance,
                        instructionText = mapTurnToString(turnType)
                    ))
                }
            }
            dataItems.release()
        }
        
        dataClient.addListener(listener)
        awaitClose { dataClient.removeListener(listener) }
    }

    private fun mapTurnToString(turnType: Int): String {
        return when (turnType) {
            1 -> "Continue straight"
            2 -> "Turn left"
            3 -> "Turn slightly left"
            4 -> "Turn sharply left"
            5 -> "Turn right"
            6 -> "Turn slightly right"
            7 -> "Turn sharply right"
            8 -> "Keep left"
            9 -> "Keep right"
            10, 11 -> "Make U-turn"
            12 -> "Off route"
            13, 14 -> "Take roundabout"
            else -> "Turn ahead"
        }
    }

    private fun mapTurnToIcon(turnType: Int, isLeftSide: Boolean): Int {
        return when (turnType) {
            1 -> android.R.drawable.arrow_up_float
            2, 3, 4, 8 -> android.R.drawable.ic_media_previous // Left arrow approximation
            5, 6, 7, 9 -> android.R.drawable.ic_media_next // Right arrow approximation
            10, 11 -> android.R.drawable.ic_menu_revert // U-turn approximation
            else -> android.R.drawable.ic_dialog_map
        }
    }
}
