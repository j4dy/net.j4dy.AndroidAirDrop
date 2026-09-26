package net.j4dy.androidairdrop.core.network

import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.net.SocketAddress
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.SocketFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

object AirDropTrustManager {

    val trustAllCerts = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }

    fun createClientSSLSocketFactory(): SSLSocketFactory {
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf<TrustManager>(trustAllCerts), SecureRandom())
        return sslContext.socketFactory
    }

    class LinkLocalSocketFactory(private var networkInterface: NetworkInterface? = null) : SocketFactory() {

        fun setNetworkInterface(iface: NetworkInterface?) {
            networkInterface = iface
        }

        override fun createSocket(): Socket {
            return object : Socket() {
                override fun connect(endpoint: SocketAddress?, timeout: Int) {
                    var targetEndpoint = endpoint
                    if (networkInterface != null && endpoint is InetSocketAddress) {
                        val addr = endpoint.address
                        if (addr is Inet6Address) {
                            val scopedAddress = Inet6Address.getByAddress(null, addr.address, networkInterface)
                            targetEndpoint = InetSocketAddress(scopedAddress, endpoint.port)
                        }
                    }
                    super.connect(targetEndpoint, timeout)
                }
            }
        }

        override fun createSocket(host: String?, port: Int): Socket? = null
        override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket? = null
        override fun createSocket(host: InetAddress?, port: Int): Socket? = null
        override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket? = null
    }
}
