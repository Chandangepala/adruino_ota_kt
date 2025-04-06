package com.android.adruino_ota_kt.view_models

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoho.android.usbserial.driver.UsbSerialDriver
import com.hoho.android.usbserial.driver.UsbSerialPort.PARITY_NONE
import com.hoho.android.usbserial.driver.UsbSerialPort.STOPBITS_1
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

const val ACTION_USB_PERMISSION = UsbManager.ACTION_USB_DEVICE_ATTACHED
class ArduinoViewModel : ViewModel() {
    private val _devices = mutableStateListOf<UsbSerialDriver>()
    val devices: List<UsbSerialDriver> get() = _devices

    private var _usbConnectionState = mutableStateOf("")
    val usbConnectionState: State<String> get() = _usbConnectionState

    private val _uploadProgress = mutableStateOf(0f)
    val uploadProgress: Float get() = _uploadProgress.value

    private val _permissionRequest = MutableStateFlow<UsbDevice?>(null)
    val permissionRequest: StateFlow<UsbDevice?> = _permissionRequest.asStateFlow()

    fun requestUsbPermission(context: Context, device: UsbDevice) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        if (!usbManager.hasPermission(device)) {
            val permissionIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_IMMUTABLE
            )
            usbManager.requestPermission(device, permissionIntent)
            _permissionRequest.value = device
        }
    }

    fun checkAndRequestPermission(context: Context, device: UsbDevice) {
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        if (!usbManager.hasPermission(device)) {
            val permissionIntent = PendingIntent.getBroadcast(
                context,
                0,
                Intent(ACTION_USB_PERMISSION),
                PendingIntent.FLAG_IMMUTABLE
            )
            usbManager.requestPermission(device, permissionIntent)
        }
    }

    fun refreshDevices(context: Context) {
        _devices.clear()
        val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
        _devices.addAll(UsbSerialProber.getDefaultProber().findAllDrivers(usbManager))
    }

    fun uploadHexFile(
        context: Context,
        driver: UsbSerialDriver,
        uri: Uri,
        onComplete: (Result<String>) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            var connection: UsbDeviceConnection? = null
            try {
                val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

                // Add this permission check before attempting upload
                if (!usbManager.hasPermission(driver.device)) {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    })
                    throw SecurityException("Redirecting to permissions settings")
                }

                if (!usbManager.hasPermission(driver.device)) {
                    throw SecurityException("USB permission not granted")
                }

                connection = usbManager.openDevice(driver.device)
                    ?: throw IOException("Failed to open USB connection")

                val port = driver.ports[0].apply {
                    open(connection)
                    setParameters(115200, 8, STOPBITS_1, PARITY_NONE)
                }

                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val buffer = ByteArray(4096)
                    var bytesSent = 0L
                    var read: Int

                    while (stream.read(buffer).also { read = it } != -1) {
                        port.write(buffer, read)
                        bytesSent += read
                        _uploadProgress.value = bytesSent.toFloat() / stream.available()
                    }

                    withContext(Dispatchers.Main) {
                        onComplete(Result.success("Uploaded $bytesSent bytes"))
                    }
                } ?: throw IOException("Failed to open file stream")

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onComplete(Result.failure(e))
                }
            } finally {
                connection?.close()
            }
        }
    }
}