package com.unknownafaik.firewallbypass

import android.app.Application
import android.app.DownloadManager
import android.content.Context
import android.content.IntentFilter
import android.net.Uri
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.net.InetAddress
import java.net.InterfaceAddress
import java.net.NetworkInterface
import java.net.SocketException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RequestResponseViewModel @Inject constructor(
    private val app: Application
) : AndroidViewModel(app) {

    sealed class Status {
        data class ReadyToTest(val id: String = "") : Status()
        data class InProgress(val ids: List<Long>, val completed: List<Long>) : Status()
        data class Completed(val response: String = "") : Status()
    }

    private val request = MutableLiveData<Status>(Status.ReadyToTest())
    private val downloadManager = app.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    val viewModel: LiveData<Status> = request

    fun onCompleted(id: Long) {

        val currentStatus = request.value

        if (currentStatus is Status.InProgress) {

            val ids = mutableListOf<Long>()
            ids.addAll(currentStatus.ids)

            val completed = mutableListOf<Long>()
            completed.addAll(currentStatus.completed)
            completed.add(id)

            request.postValue(
                Status.InProgress(
                    ids,
                    completed
                )
            )

            if (ids.size == completed.size) {
                request.postValue(
                    Status.Completed()
                )
            }
        }
    }

    private fun downloadRequest(uri: Uri, fileName: String): DownloadManager.Request {
        return DownloadManager.Request(uri)
            .setAllowedNetworkTypes(DownloadManager.Request.NETWORK_MOBILE or DownloadManager.Request.NETWORK_WIFI)
            .setAllowedOverRoaming(true)
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
    }

    @Throws(SecurityException::class)
    suspend fun makeNewRequest(): Boolean {

        if (request.value is Status.InProgress) {
            return false
        }

        try {
            val requests = mutableListOf<Long>()
            val headers = try {
                getHeaders()
            } catch (e: Exception) {
                emptyMap()
            }

            app.registerReceiver(
                DownloadCompleted(),
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )

            requests.add(
                downloadManager.enqueue(
                    downloadRequest(
                        Uri.parse("https://www.myip.com/"),
                        "firewall${System.currentTimeMillis()}.html"
                    )
                )
            )

            headers.forEach { (_, value) ->

                val ipAPi = "https://ipapi.com/ip_api.php?ip=$value"
                val ipInfo = "https://ipinfo.io/$value"

                val uri = Uri.parse(ipInfo)
                val fileName = "firewall${System.currentTimeMillis()}.html"

                val downloadRequest = downloadRequest(uri, fileName)
                requests.add(downloadManager.enqueue(downloadRequest))
            }

            request.postValue(
                Status.InProgress(requests, emptyList())
            )
        }catch (e : Exception){
            println(e.localizedMessage)
            if(e is SecurityException) throw e
        }

        return true
    }

    private fun getHeaders(): Map<String, String> {

        val result = mutableMapOf<String, String>()

        try {
            val networkInfos = NetworkInterface.getNetworkInterfaces()
            val subInterfaceAddress = mutableListOf<InterfaceAddress>()

            networkInfos?.let {
                while (networkInfos.hasMoreElements()) {

                    val networkInfo = networkInfos.nextElement()
                    val enumIpAddr = networkInfo.inetAddresses

                    subInterfaceAddress.addAll(networkInfo.interfaceAddresses)

                    while (enumIpAddr.hasMoreElements()) {
                        val inetAddress = enumIpAddr.nextElement()

                        if (!inetAddress.isLoopbackAddress) {

                            val ip = InetAddress.getByAddress(inetAddress.address)

                            ip.hostAddress?.let { address ->

                                if (address.isReal()) {
                                    result[address] = inetAddress.toString() + ip.toString()
                                    result[address] = address
                                }
                            }
                        }
                    }
                }
            }

        } catch (ex: SocketException) {
            println(ex.localizedMessage)
        }

        return result
    }

    private fun String.isReal(): Boolean {

        val ipAddress: String = this

        val notDummy = !ipAddress.contains("_", false)
        val notDATA = !ipAddress.contains("data0", true)
        val notDummy2 = !ipAddress.contains("dummy", true)
        val notWLAN = !ipAddress.contains("wlan", true)

        val notLOCALHOST = !ipAddress.startsWith("192.168.", true)
        val notLocalhostIPV6 = !ipAddress.startsWith("fc00:", true)

        val notLocalhost2 = !ipAddress.startsWith("fe80::", true)

        val notReal7 = !ipAddress.startsWith("100.", true)
        val notReal8 = !ipAddress.startsWith("10.1", true)

        return notDummy && notDATA && notDummy2 && notWLAN
                && notLOCALHOST && notLocalhostIPV6 && notLocalhost2 && notReal7 && notReal8
    }

}