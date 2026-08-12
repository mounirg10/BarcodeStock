package com.mounir.barcodestock.sale

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mounir.barcodestock.data.AppDatabase
import com.mounir.barcodestock.data.Product
import com.mounir.barcodestock.data.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** سطر في سلة البيع. */
data class CartLine(
    val productId: Long,
    val barcode: String,
    val name: String,
    val unitPrice: Double,
    val quantity: Int
) {
    val lineTotal: Double get() = unitPrice * quantity
}

/**
 * وضع البيع: مسح الباركود ← جلب السعر من قاعدة البيانات ← حساب المجموع الكلي.
 */
class SaleViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ProductRepository(AppDatabase.get(app).productDao())

    private val _lines = MutableStateFlow<List<CartLine>>(emptyList())
    val lines: StateFlow<List<CartLine>> = _lines

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    /** منتج ممسوح غير موجود في المخزون (لعرض نافذة تسجيله). */
    private val _unknownBarcode = MutableStateFlow<String?>(null)
    val unknownBarcode: StateFlow<String?> = _unknownBarcode

    val itemCount: Int get() = _lines.value.sumOf { it.quantity }

    val total: Double get() = _lines.value.sumOf { it.lineTotal }

    fun consumeMessage() { _message.value = null }
    fun consumeUnknown() { _unknownBarcode.value = null }

    /** يُستدعى عند كل قراءة باركود في شاشة البيع. */
    fun onBarcodeScanned(barcode: String) = viewModelScope.launch {
        val product = repo.findByBarcode(barcode.trim())
        if (product == null) {
            _unknownBarcode.value = barcode.trim()
            _message.value = "باركود غير مسجّل: $barcode"
            return@launch
        }
        addProduct(product)
        _message.value = "${product.name} — ${format(product.price)}"
    }

    private fun addProduct(product: Product) {
        val current = _lines.value.toMutableList()
        val index = current.indexOfFirst { it.productId == product.id }
        if (index >= 0) {
            val line = current[index]
            current[index] = line.copy(quantity = line.quantity + 1)
        } else {
            current.add(
                CartLine(
                    productId = product.id,
                    barcode = product.barcode,
                    name = product.name,
                    unitPrice = product.price,
                    quantity = 1
                )
            )
        }
        _lines.value = current
    }

    fun increase(line: CartLine) = update(line) { it.copy(quantity = it.quantity + 1) }

    fun decrease(line: CartLine) {
        if (line.quantity <= 1) remove(line) else update(line) { it.copy(quantity = it.quantity - 1) }
    }

    fun remove(line: CartLine) {
        _lines.value = _lines.value.filterNot { it.productId == line.productId }
    }

    fun clear() { _lines.value = emptyList() }

    private fun update(line: CartLine, transform: (CartLine) -> CartLine) {
        _lines.value = _lines.value.map { if (it.productId == line.productId) transform(it) else it }
    }

    /**
     * إنهاء البيع: يخصم الكميات المباعة من المخزون ثم يفرغ السلة.
     * @param onReceipt يستقبل نص الوصل والمبلغ النهائيّ.
     */
    fun checkout(deductStock: Boolean = true, onReceipt: (String, Double) -> Unit = { _, _ -> }) =
        viewModelScope.launch {
            val snapshot = _lines.value
            if (snapshot.isEmpty()) { _message.value = "السلة فارغة"; return@launch }
            val finalTotal = snapshot.sumOf { it.lineTotal }

            if (deductStock) {
                snapshot.forEach { line ->
                    repo.findById(line.productId)?.let { p ->
                        val left = (p.quantity - line.quantity).coerceAtLeast(0)
                        repo.save(p.copy(quantity = left))
                    }
                }
            }

            onReceipt(buildReceipt(snapshot, finalTotal), finalTotal)
            _lines.value = emptyList()
            _message.value = "تمت عملية البيع — المجموع: ${format(finalTotal)}"
        }

    private fun buildReceipt(lines: List<CartLine>, total: Double): String = buildString {
        appendLine("وصل بيع")
        appendLine("------------------------------")
        lines.forEach { l ->
            appendLine("${l.name} ×${l.quantity}  =  ${format(l.lineTotal)}")
        }
        appendLine("------------------------------")
        appendLine("عدد القطع: ${lines.sumOf { it.quantity }}")
        appendLine("المجموع الكلي: ${format(total)}")
    }

    companion object {
        fun format(value: Double): String = String.format(java.util.Locale.US, "%.2f", value)
    }
}
