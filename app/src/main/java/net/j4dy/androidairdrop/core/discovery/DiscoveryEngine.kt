package net.j4dy.androidairdrop.core.discovery

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import net.j4dy.androidairdrop.core.model.AirDropPeer
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.concurrent.ConcurrentHashMap
import javax.jmdns.JmDNS
import javax.jmdns.ServiceEvent
import javax.jmdns.ServiceInfo
import javax.jmdns.ServiceListener

class DiscoveryEngine(private val context: Context) {

    private val tag = "DiscoveryEngine"
    private val airdropServiceType = "_airdrop._tcp.local."

    private val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    private val multicastLock: WifiManager.MulticastLock = wifiManager.createMulticastLock("AndroidAirDropMulticast").apply {
        setReferenceCounted(false)
    }

    private val peersMap = ConcurrentHashMap<String, AirDropPeer>()
    private val _peersFlow = MutableStateFlow<List<AirDropPeer>>(emptyList())
    val peersFlow: StateFlow<List<AirDropPeer>> = _peersFlow.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private var jmDns: JmDNS? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    private val serviceListener = object : ServiceListener {
        override fun serviceAdded(event: ServiceEvent?) {
            Log.d(tag, "Service added: ${event?.name}")
            event?.name?.let { jmDns?.requestServiceInfo(airdropServiceType, it, 3000) }
        }

        override fun serviceRemoved(event: ServiceEvent?) {
            val name = event?.name ?: return
            Log.d(tag, "Service removed: $name")
            peersMap.remove(name)
            updatePeers()
        }

        override fun serviceResolved(event: ServiceEvent?) {
            val info = event?.info ?: return
            handleServiceResolved(info)
        }
    }

    fun startDiscovery() {
        if (_isScanning.value) return
        _isScanning.value = true

        scope.launch {
            try {
                multicastLock.acquire()
                val localAddress = getLocalWifiIpAddress()
                if (localAddress != null) {
                    Log.i(tag, "Starting JmDNS on $localAddress")
                    jmDns = JmDNS.create(localAddress).apply {
                        addServiceListener(airdropServiceType, serviceListener)
                    }
                } else {
                    Log.w(tag, "Could not determine local Wi-Fi address")
                }
            } catch (e: Exception) {
                Log.e(tag, "Failed to start discovery", e)
            }
        }
    }

    fun stopDiscovery() {
        if (!_isScanning.value) return
        _isScanning.value = false

        scope.launch {
            try {
                jmDns?.removeServiceListener(airdropServiceType, serviceListener)
                jmDns?.close()
                jmDns = null
            } catch (e: IOException) {
                Log.w(tag, "Error closing JmDNS", e)
            } finally {
                if (multicastLock.isHeld) {
                    multicastLock.release()
                }
            }
        }
    }

    private fun handleServiceResolved(info: ServiceInfo) {
        val addresses = info.inet4Addresses
        if (addresses.isEmpty()) return

        val hostAddress = addresses[0].hostAddress ?: return
        val port = info.port
        val rawName = info.name
        val model = info.getPropertyString("model") ?: "Mac"

        // AirDrop service names often appear as computer names or IDs
        val displayName = cleanDeviceName(rawName)

        val peer = AirDropPeer(
            id = rawName,
            name = displayName,
            hostAddress = hostAddress,
            port = port,
            model = model
        )

        Log.i(tag, "Discovered AirDrop peer: $peer")
        peersMap[rawName] = peer
        updatePeers()
    }

    private fun updatePeers() {
        _peersFlow.value = peersMap.values.toList().sortedBy { it.name }
    }

    private fun cleanDeviceName(rawName: String): String {
        return rawName.replace("._airdrop._tcp.local.", "").trim()
    }

    private fun getLocalWifiIpAddress(): InetAddress? {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return null
        val caps = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return null

        if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isUp && !iface.isLoopback) {
                    val addrs = iface.inetAddresses
                    while (addrs.hasMoreElements()) {
                        val addr = addrs.nextElement()
                        if (addr is Inet4Address && !addr.isLoopbackAddress) {
                            return addr
                        }
                    }
                }
            }
        }
        return null
    }
}
