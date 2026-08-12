package com.mounir.barcodestock.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.concurrent.TimeUnit

@Entity(tableName = "products")
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val barcode: String,
    val name: String,
    val price: Double,
    val quantity: Int = 1,
    val entryDate: Long,
    val expiryDate: Long
) {
    val daysLeft: Long
        get() = TimeUnit.MILLISECONDS.toDays(expiryDate - System.currentTimeMillis())

    val status: ExpiryStatus
        get() = when {
            daysLeft < 0 -> ExpiryStatus.EXPIRED
            daysLeft <= 30 -> ExpiryStatus.SOON
            else -> ExpiryStatus.VALID
        }
}

enum class ExpiryStatus(val label: String) {
    VALID("صالح"), SOON("قارب على الانتهاء"), EXPIRED("منتهي الصلاحية")
}
