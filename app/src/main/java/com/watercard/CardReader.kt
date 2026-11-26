package com.watercard

import android.util.Log
import com.hoho.android.usbserial.driver.UsbSerialPort
import kotlinx.coroutines.delay

class CardReader(private val port: UsbSerialPort) {

    companion object {
        private const val TAG = "CardReader"
        private const val TIMEOUT_MS = 800
    }

    private fun buildFrame(data: ByteArray): ByteArray {
        var x = 0x33
        for (b in data) {
            x = x xor (b.toInt() and 0xFF)
        }
        val len = data.size
        return byteArrayOf(
            (len and 0xFF).toByte(),
            ((len shr 8) and 0xFF).toByte()
        ) + data + byteArrayOf((x and 0xFF).toByte())
    }

    private suspend fun send(data: ByteArray): ByteArray {
        port.purgeHwBuffers(true, true)
        port.write(buildFrame(data), TIMEOUT_MS)

        val header = ByteArray(4)
        var read = 0
        while (read < 4) {
            val n = port.read(header, read, 4 - read, TIMEOUT_MS)
            if (n <= 0) throw RuntimeException("انتهت المهلة في رأس الرد")
            read += n
        }
        
        val h0 = header[0].toInt()
        val h1 = header[1].toInt()
        if (h0 != 0x00 || h1 != 0x02) {
            throw RuntimeException("رأس رد غير متوقع")
        }

        val len = (header[2].toInt() and 0xFF) or ((header[3].toInt() and 0xFF) shl 8)
        if (len <= 0 || len > 64) throw RuntimeException("طول رد غير منطقي: $len")

        val body = ByteArray(len + 1)
        read = 0
        while (read < body.size) {
            val n = port.read(body, read, body.size - read, TIMEOUT_MS)
            if (n <= 0) throw RuntimeException("انتهت المهلة في جسم الرد")
            read += n
        }

        val payload = body.copyOfRange(0, len)
        var x = 0x33
        for (b in payload) {
            x = x xor (b.toInt() and 0xFF)
        }
        
        // الحل النهائي: مقارنة نصوص بدلاً من أرقام
        val calculatedHex = "%02X".format(x and 0xFF)
        val receivedHex = "%02X".format(body[len].toInt() and 0xFF)
        if (calculatedHex != receivedHex) {
            throw RuntimeException("CHK غير مطابق")
        }

        return payload
    }

    private suspend fun resetCard() {
        try { send(byteArrayOf(0x07)); delay(200) } catch (e: Exception) { Log.d(TAG, "07: ${e.message}") }
        try { send(byteArrayOf(0x06)); delay(200) } catch (e: Exception) { Log.d(TAG, "06: ${e.message}") }
    }

    suspend fun readRawCard(): ByteArray {
        resetCard()
        val result = ByteArray(32)
        for (p in 0..7) {
            val payload = send(byteArrayOf(0x08, 0x01, 0x00))
            if (payload.size < 4) throw RuntimeException("قراءة صفحة $p قصيرة (${payload.size})")
            System.arraycopy(payload, 0, result, p * 4, 4)
        }
        return result
    }

    suspend fun readCard(): ByteArray {
        val raw32 = readRawCard()
        return raw32.copyOfRange(4, 32)
    }

    private suspend fun writePage(page: Int, prev: ByteArray, next: ByteArray) {
        require(prev.size == 4 && next.size == 4)
        val data = byteArrayOf(0x09, 0x00, page.toByte(), 0x00, 0x00) + prev + next
        send(data)
        delay(60)
    }

    suspend fun writeCard(card: ByteArray) {
        require(card.size == 28) { "البطاقة يجب أن تكون 28 بايتاً" }
        
        val currentRaw = readRawCard()
        val configBytes = byteArrayOf(0x60, 0x08, 0x80.toByte(), 0xE8.toByte())
        
        if (!currentRaw.copyOfRange(0, 4).contentEquals(configBytes)) {
            writePage(0, currentRaw.copyOfRange(0, 4), configBytes)
        }
        
        for (p in 1..7) {
            val prev = currentRaw.copyOfRange(p * 4, p * 4 + 4)
            val next = card.copyOfRange((p - 1) * 4, p * 4)
            
            if (!prev.contentEquals(next)) {
                writePage(p, prev, next)
            }
        }
        delay(100)
    }

    suspend fun writeAndVerify(card: ByteArray): Boolean {
        writeCard(card)
        var actual = readCard()
        if (actual.contentEquals(card)) {
            return true
        }

        Log.w(TAG, "فشل التحقق من الكتابة، إعادة المحاولة...")
        delay(200)
        writeCard(card)
        actual = readCard()
        return actual.contentEquals(card)
    }
}
