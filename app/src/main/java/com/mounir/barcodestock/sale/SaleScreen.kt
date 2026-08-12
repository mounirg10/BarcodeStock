package com.mounir.barcodestock.sale

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mounir.barcodestock.scanner.CameraScanner

/**
 * شاشة البيع (نقطة بيع مبسّطة):
 * مسح متواصل للباركود ← جلب اسم وسعر المنتج ← إضافته للسلة ← المجموع الكلي فورًا.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleScreen(
    vm: SaleViewModel,
    onBack: () -> Unit,
    onAddUnknown: (String) -> Unit
) {
    val lines by vm.lines.collectAsState()
    val message by vm.message.collectAsState()
    val unknown by vm.unknownBarcode.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var receipt by remember { mutableStateOf<String?>(null) }

    val total = lines.sumOf { it.lineTotal }
    val count = lines.sumOf { it.quantity }

    LaunchedEffect(message) { message?.let { snackbar.showSnackbar(it); vm.consumeMessage() } }

    unknown?.let { code ->
        AlertDialog(
            onDismissRequest = { vm.consumeUnknown() },
            title = { Text("منتج غير مسجّل") },
            text = { Text("الباركود $code غير موجود في المخزون. هل تريد تسجيله الآن؟") },
            confirmButton = {
                TextButton(onClick = { vm.consumeUnknown(); onAddUnknown(code) }) { Text("تسجيل المنتج") }
            },
            dismissButton = { TextButton(onClick = { vm.consumeUnknown() }) { Text("تجاهل") } }
        )
    }

    receipt?.let { text ->
        AlertDialog(
            onDismissRequest = { receipt = null },
            title = { Text("وصل البيع") },
            text = { Text(text, fontSize = 14.sp) },
            confirmButton = { TextButton(onClick = { receipt = null }) { Text("تمام") } }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("وضع البيع") },
                navigationIcon = { TextButton(onClick = onBack) { Text("رجوع") } },
                actions = { TextButton(onClick = { vm.clear() }) { Text("إفراغ السلة") } }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("عدد القطع: $count", fontSize = 14.sp)
                        Text(
                            "المجموع الكلي: ${SaleViewModel.format(total)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { vm.checkout { text, _ -> receipt = text } },
                        enabled = lines.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) { Text("إنهاء البيع وخصم الكميات") }
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {

            // الكاميرا تبقى مفتوحة لمسح منتج تلو الآخر
            CameraScanner(
                modifier = Modifier.fillMaxWidth().height(220.dp),
                continuous = true,
                onBarcode = { code -> vm.onBarcodeScanned(code) }
            )

            if (lines.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("امسح باركود المنتجات لإضافتها إلى سلة الزبون")
                }
            } else {
                LazyColumn(
                    Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    items(lines, key = { it.productId }) { line ->
                        CartRow(
                            line = line,
                            onPlus = { vm.increase(line) },
                            onMinus = { vm.decrease(line) },
                            onRemove = { vm.remove(line) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CartRow(
    line: CartLine,
    onPlus: () -> Unit,
    onMinus: () -> Unit,
    onRemove: () -> Unit
) {
    ElevatedCard(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(line.name, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("السعر: ${SaleViewModel.format(line.unitPrice)}", fontSize = 12.sp)
                Text(
                    "المجموع: ${SaleViewModel.format(line.lineTotal)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            IconButton(onClick = onMinus) { Icon(Icons.Default.Remove, "إنقاص") }
            Text(line.quantity.toString(), fontWeight = FontWeight.Bold)
            IconButton(onClick = onPlus) { Icon(Icons.Default.Add, "زيادة") }
            IconButton(onClick = onRemove) { Icon(Icons.Default.Delete, "حذف") }
        }
    }
}
