package com.watercard.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscribers")
data class Subscriber(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userNo: Int,
    val name: String,
    val phone: String = "",
    val address: String = "",
    val price: Double = 0.1,
    val limitQty: Int = 100,
    val nextCount: Int = 1,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun copyWithNextCount(newCount: Int): Subscriber {
        return this.copy(nextCount = newCount)
    }
}

@Entity(tableName = "charges")
data class Charge(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userNo: Int,
    val countNo: Int,
    val qty: Int,
    val amount: Double,
    val cardHex: String,
    val createdAt: Long = System.currentTimeMillis()
)
