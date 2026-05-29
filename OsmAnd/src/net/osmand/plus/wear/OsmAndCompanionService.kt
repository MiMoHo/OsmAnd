package net.osmand.plus.wear

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import net.osmand.aidl.IOsmAndAidlInterface
import net.osmand.aidl.IOsmAndAidlCallback
import net.osmand.aidl.navigation.ADirectionInfo
import net.osmand.aidl.navigation.ANavigationUpdateParams

class OsmAndCompanionService : Service() {

    private var osmandAidlInterface: IOsmAndAidlInterface? = null
    private var navigationCallbackId: Long = -1L

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d("OsmAndCompanion", "Connected to OsmAnd AIDL Service")
            osmandAidlInterface = IOsmAndAidlInterface.Stub.asInterface(service)
            registerForNavigationUpdates()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d("OsmAndCompanion", "Disconnected from OsmAnd AIDL Service")
            osmandAidlInterface = null
        }
    }

    private val osmandCallback = object : IOsmAndAidlCallback.Stub() {
        override fun updateNavigationInfo(directionInfo: ADirectionInfo?) {
            directionInfo?.let { info ->
                sendDataToWearOS(info)
            }
        }
        
        override fun onSearchComplete(resultSet: List<net.osmand.aidl.search.SearchResult>?) {}
        override fun onUpdate() {}
        override fun onAppInitialized() {}
        override fun onGpxBitmapCreated(bitmap: net.osmand.aidl.gpx.AGpxBitmap?) {}
        override fun onContextMenuButtonClicked(buttonId: Int, pointId: String?, layerId: String?) {}
        override fun onVoiceRouterNotify(params: net.osmand.aidl.navigation.OnVoiceNavigationParams?) {}
    }

    override fun onCreate() {
        super.onCreate()
        bindToOsmAnd()
    }

    private fun bindToOsmAnd() {
        val intent = Intent("net.osmand.aidl.OsmandAidlService")
        intent.setPackage(packageName)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun registerForNavigationUpdates() {
        try {
            val params = ANavigationUpdateParams().apply {
                setSubscribeToUpdates(true)
            }
            navigationCallbackId = osmandAidlInterface?.registerForNavigationUpdates(params, osmandCallback) ?: -1L
        } catch (e: Exception) {
            Log.e("OsmAndCompanion", "Failed to register for OsmAnd updates", e)
        }
    }

    private fun sendDataToWearOS(info: ADirectionInfo) {
        val dataClient = Wearable.getDataClient(applicationContext)
        val putDataReq = PutDataMapRequest.create("/osmand/navigation").apply {
            dataMap.putBoolean("isActive", true)
            dataMap.putString("distance", formatDistance(info.distanceTo))
            dataMap.putInt("turnType", info.turnType)
            dataMap.putBoolean("isLeftSide", info.isLeftSide)
            dataMap.putLong("timestamp", System.currentTimeMillis())
        }.asPutDataRequest()
        
        dataClient.putDataItem(putDataReq)
    }

    private fun formatDistance(distanceMeters: Int): String {
        return if (distanceMeters > 1000) {
            String.format("%.1f km", distanceMeters / 1000f)
        } else {
            "$distanceMeters m"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (navigationCallbackId != -1L) {
            try {
                osmandAidlInterface?.unregisterFromUpdates(navigationCallbackId)
            } catch (e: Exception) {
                Log.e("OsmAndCompanion", "Error unregistering updates", e)
            }
        }
        unbindService(serviceConnection)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
