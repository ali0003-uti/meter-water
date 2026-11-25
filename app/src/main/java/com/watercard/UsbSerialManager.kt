package com.watercard

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Build
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class UsbSerialManager(private val context: Context) {

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private var port: UsbSerialPort? = null

    fun listDevices(): List<UsbDevice> {
        val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        return drivers.map { it.device }
    }

    suspend fun open(device: UsbDevice): UsbSerialPort {
        val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(usbManager)
        val driver = drivers.firstOrNull { it.device == device }
            ?: throw RuntimeException("لا يوجد تعريف لهذا الجهاز")

        if (!usbManager.hasPermission(device)) {
            val granted = requestPermission(device)
            if (!granted) throw RuntimeException("لم يُمنح إذن USB")
        }

        val connection = usbManager.openDevice(device)
            ?: throw RuntimeException("فشل فتح الجهاز")

        val p = driver.ports[0]
        p.open(connection)
        p.setParameters(19200, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
        port = p
        return p
    }

    fun close() {
        try { port?.close() } catch (_: Exception) {}
        port = null
    }

    private suspend fun requestPermission(device: UsbDevice): Boolean =
        suspendCancellableCoroutine { cont ->
            val action = "${context.packageName}.USB_PERMISSION"
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    if (intent.action == action) {
                        try { ctx.unregisterReceiver(this) } catch (_: Exception) {}
                        val ok = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                        if (cont.isActive) cont.resume(ok)
                    }
                }
            }
            val flags = if (Build.VERSION.SDK_INT >= 33) PendingIntent.FLAG_MUTABLE else 0
            val pi = PendingIntent.getBroadcast(
                context, 0, Intent(action).setPackage(context.packageName), flags
            )
            if (Build.VERSION.SDK_INT >= 33) {
                context.registerReceiver(receiver, IntentFilter(action), Context.RECEIVER_NOT_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                context.registerReceiver(receiver, IntentFilter(action))
            }
            usbManager.requestPermission(device, pi)
        }
}
