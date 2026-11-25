package com.watercard

object CardEncoder {

    private val S = intArrayOf(
        0x20, 0xC6, 0x62, 0x3A, 0xBF, 0x79, 0xE7, 0x4C,
        0x98, 0x0E, 0xA5, 0x8D, 0xDB, 0x13, 0x51, 0xF4
    )

    private fun packChecks(b1: Int, b2: Int, b3: Int, b4: Int): IntArray {
        val c0 = ((b1 shr 6) and 3) or ((b2 and 3) shl 2) or
                 (((b3 shr 2) and 3) shl 4) or (((b4 shr 4) and 3) shl 6)
        val c1 = ((b2 shr 6) and 3) or ((b3 and 3) shl 2) or
                 (((b4 shr 2) and 3) shl 4) or (((b1 shr 4) and 3) shl 6)
        val c2 = ((b3 shr 6) and 3) or ((b4 and 3) shl 2) or
                 (((b1 shr 2) and 3) shl 4) or (((b2 shr 4) and 3) shl 6)
        val c3 = ((b4 shr 6) and 3) or ((b1 and 3) shl 2) or
                 (((b2 shr 2) and 3) shl 4) or (((b3 shr 4) and 3) shl 6)
        return intArrayOf(c0, c1, c2, c3)
    }

    fun encode(userNo: Int, count: Int, qty: Int): ByteArray {
        require(userNo in 0..255) { "رقم المشترك يجب أن يكون 0..255" }
        require(count in 1..255) { "رقم الدفعة يجب أن يكون 1..255" }
        require(qty in 1..100) { "الكمية يجب أن تكون 1..100" }

        val raw = IntArray(28)
        raw[0] = 0x17; raw[1] = 0x13; raw[2] = 0x00
        raw[3] = userNo and 0xFF
        raw[4] = count and 0xFF
        raw[6] = (qty * 10) and 0xFF
        raw[13] = if (count == 1) 0x00 else 0x01
        raw[14] = 0x01
        raw[16] = 0x03; raw[17] = 0xE8

        val b1 = 0xDF
        val b2 = (((userNo and 0x0F) shl 4) xor ((qty * 10) and 0xFF) xor 0xD7) and 0xFF
        val b3 = 0x61
        val b4 = ((0x11 xor S[count and 0x0F]) + 0x90) and 0xFF

        val chk = packChecks(b1, b2, b3, b4)
        raw[24] = chk[0]; raw[25] = chk[1]; raw[26] = chk[2]; raw[27] = chk[3]

        var sum = 0
        for (i in 0 until 15) sum += raw[i]
        sum += chk[0] + chk[1] + chk[2] + chk[3]
        raw[15] = sum and 0xFF

        return ByteArray(28) { i -> (raw[i] xor (0xA5 xor i)).toByte() }
    }

    fun decode(card: ByteArray): DecodedCard {
        require(card.size >= 28)
        val raw = IntArray(28) { i -> (card[i].toInt() and 0xFF) xor (0xA5 xor i) }
        return DecodedCard(
            userNo = raw[3],
            count = raw[4],
            qty = raw[6] / 10,
            isFirst = raw[13] == 0,
            raw = raw
        )
    }

    fun verifySignature(card: ByteArray): Boolean {
        if (card.size < 28) return false
        val d = decode(card)
        if (d.qty !in 1..100 || d.count !in 1..255) return false
        val expected = encode(d.userNo, d.count, d.qty)
        return (24..27).all { expected[it] == card[it] }
    }
}

data class DecodedCard(
    val userNo: Int,
    val count: Int,
    val qty: Int,
    val isFirst: Boolean,
    val raw: IntArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DecodedCard) return false
        return userNo == other.userNo && count == other.count &&
               qty == other.qty && isFirst == other.isFirst && raw.contentEquals(other.raw)
    }
    override fun hashCode(): Int {
        var r = userNo
        r = 31 * r + count
        r = 31 * r + qty
        r = 31 * r + raw.contentHashCode()
        return r
    }
}

fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it) }
