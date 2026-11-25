package com.watercard

import android.util.Log
import com.watercard.data.AppDatabase
import com.watercard.data.Charge
import com.watercard.data.Subscriber

class ChargeService(private val db: AppDatabase) {

    suspend fun charge(reader: CardReader, subscriber: Subscriber, qty: Int): Charge {
        val current = try { reader.readCard() } catch (e: Exception) {
            throw RuntimeException("فشل قراءة البطاقة: ${e.message}")
        }
        val info = CardEncoder.decode(current)

        if (info.userNo != 0 && info.userNo != subscriber.userNo) {
            throw RuntimeException("البطاقة تخص المشترك ${info.userNo}، وليس ${subscriber.userNo}")
        }

        val count = subscriber.nextCount
        val newCard = CardEncoder.encode(subscriber.userNo, count, qty)

        reader.writeCard(newCard)

        val verify = try { reader.verifyWritten(newCard) } catch (e: Exception) {
            Log.w("ChargeService", "فشل التحقق بعد الكتابة: ${e.message}")
            false
        }
        if (!verify) {
            Log.w("ChargeService", "التحقق الكامل فشل — قد يكون العدّاد عدّل بايتات الحالة")
        }

        val charge = Charge(
            userNo = subscriber.userNo,
            countNo = count,
            qty = qty,
            amount = qty * subscriber.price,
            cardHex = newCard.toHex()
        )
        db.chargeDao().insert(charge)
        db.subscriberDao().update(subscriber.copy(nextCount = count + 1))

        return charge
    }

    suspend fun registerAndFirstCharge(
        reader: CardReader, name: String, phone: String,
        address: String, price: Double, firstQty: Int
    ): Pair<Subscriber, Charge> {
        val max = db.subscriberDao().maxUserNo() ?: 150
        val userNo = (max + 1).coerceAtMost(255)
        if (userNo > 255) throw RuntimeException("تم الوصول للحد الأقصى (255 مشترك)")

        val sub = Subscriber(
            userNo = userNo, name = name, phone = phone,
            address = address, price = price, nextCount = 1
        )
        val id = db.subscriberDao().insert(sub)
        val saved = sub.copy(id = id)

        val charge = charge(reader, saved, firstQty)
        val updated = saved.copy(nextCount = 2)
        return updated to charge
    }
}
