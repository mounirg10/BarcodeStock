package com.mounir.barcodestock.ui

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mounir.barcodestock.data.Product
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    vm: ProductViewModel,
    productId: Long?,
    scannedBarcode: String?,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    var id by remember { mutableStateOf(0L) }
    var barcode by remember { mutableStateOf(scannedBarcode ?: "") }
    var name by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var entryDate by remember { mutableStateOf(System.currentTimeMillis()) }
    var expiryDate by remember { mutableStateOf(System.currentTimeMillis() + 30L * 24 * 3600 * 1000) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(productId, scannedBarcode) {
        val existing = when {
            productId != null && productId > 0 -> vm.findById(productId)
            !scannedBarcode.isNullOrBlank() -> vm.findByBarcode(scannedBarcode)
            else -> null
        }
        existing?.let {
            id = it.id; barcode = it.barcode; name = it.name
            price = it.price.toString(); quantity = it.quantity.toString()
            entryDate = it.entryDate; expiryDate = it.expiryDate
        }
    }

    fun pickDate(current: Long, onPicked: (Long) -> Unit) {
        val cal = Calendar.getInstance().apply { timeInMillis = current }
        DatePickerDialog(
            context,
            { _, y, m, d ->
                val c = Calendar.getInstance()
                c.set(y, m, d, 0, 0, 0); c.set(Calendar.MILLISECOND, 0)
                onPicked(c.timeInMillis)
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text(if (id == 0L) "إضافة منتج" else "تعديل منتج") },
            navigationIcon = { TextButton(onClick = onBack) { Text("رجوع") } }
        )
    }) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = barcode, onValueChange = { barcode = it },
                label = { Text("الباركود") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("اسم المنتج") }, modifier = Modifier.fillMaxWidth(), singleLine = true
            )
            OutlinedTextField(
                value = price, onValueChange = { v -> price = v.filter { it.isDigit() || it == '.' } },
                label = { Text("سعر المنتج") }, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                singleLine = true
            )
            OutlinedTextField(
                value = quantity, onValueChange = { v -> quantity = v.filter { it.isDigit() } },
                label = { Text("الكمية") }, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            OutlinedButton(onClick = { pickDate(entryDate) { entryDate = it } }, modifier = Modifier.fillMaxWidth()) {
                Text("تاريخ دخول المنتج: ${dateFmt.format(Date(entryDate))}")
            }
            OutlinedButton(onClick = { pickDate(expiryDate) { expiryDate = it } }, modifier = Modifier.fillMaxWidth()) {
                Text("تاريخ نهاية الصلاحية: ${dateFmt.format(Date(expiryDate))}")
            }

            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }

            Button(
                modifier = Modifier.fillMaxWidth().height(52.dp),
                onClick = {
                    when {
                        barcode.isBlank() -> error = "الرجاء إدخال أو مسح الباركود"
                        name.isBlank() -> error = "الرجاء إدخال اسم المنتج"
                        price.toDoubleOrNull() == null -> error = "السعر غير صحيح"
                        expiryDate < entryDate -> error = "تاريخ الصلاحية يجب أن يكون بعد تاريخ الدخول"
                        else -> vm.save(
                            Product(
                                id = id,
                                barcode = barcode.trim(),
                                name = name.trim(),
                                price = price.toDouble(),
                                quantity = quantity.toIntOrNull() ?: 1,
                                entryDate = entryDate,
                                expiryDate = expiryDate
                            )
                        ) { onDone() }
                    }
                }
            ) { Text("حفظ المنتج") }
        }
    }
}
