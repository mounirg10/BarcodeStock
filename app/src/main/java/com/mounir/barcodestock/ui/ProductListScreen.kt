package com.mounir.barcodestock.ui

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.mounir.barcodestock.data.ExpiryStatus
import com.mounir.barcodestock.data.Product
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val dateFmt: SimpleDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale("ar"))

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ProductListScreen(
    vm: ProductViewModel,
    onScan: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Long) -> Unit,
    onSale: () -> Unit = {}
) {
    val context = LocalContext.current
    val products by vm.products.collectAsState()
    val query by vm.query.collectAsState()
    val message by vm.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }

    // إذن الإشعارات مطلوب من أندرويد 13 فما فوق
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val notifPermission = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
        LaunchedEffect(Unit) {
            if (!notifPermission.status.isGranted) notifPermission.launchPermissionRequest()
        }
    }

    LaunchedEffect(message) {
        message?.let { snackbar.showSnackbar(it); vm.consumeMessage() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("إدارة المنتجات", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onSale) {
                        Icon(Icons.Default.PointOfSale, contentDescription = "وضع البيع")
                    }
                    IconButton(onClick = { vm.exportToExcel(context) }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "نسخ احتياطي Excel")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                SmallFloatingActionButton(onClick = onAdd) { Icon(Icons.Default.Add, null) }
                Spacer(Modifier.height(12.dp))
                ExtendedFloatingActionButton(
                    onClick = onScan,
                    icon = { Icon(Icons.Default.QrCodeScanner, null) },
                    text = { Text("مسح باركود") }
                )
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize().padding(horizontal = 16.dp)) {

            Button(
                onClick = onSale,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).height(50.dp)
            ) { Text("🛒 بدء عملية بيع (مسح وحساب المجموع)") }

            StatsRow(products)

            OutlinedTextField(
                value = query,
                onValueChange = vm::onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                leadingIcon = { Icon(Icons.Default.Search, null) },
                label = { Text("بحث بالاسم أو الباركود") },
                singleLine = true
            )

            if (products.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("لا توجد منتجات بعد. اضغط على «مسح باركود» للبدء.")
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(products, key = { it.id }) { p ->
                        ProductCard(p, onClick = { onEdit(p.id) }, onDelete = { vm.delete(p) })
                    }
                    item { Spacer(Modifier.height(90.dp)) }
                }
            }
        }
    }
}

@Composable
private fun StatsRow(products: List<Product>) {
    val expired = products.count { it.status == ExpiryStatus.EXPIRED }
    val soon = products.count { it.status == ExpiryStatus.SOON }
    val total = products.sumOf { it.price * it.quantity }

    Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatCard("المنتجات", products.size.toString(), Modifier.weight(1f))
        StatCard("قرب الانتهاء", soon.toString(), Modifier.weight(1f))
        StatCard("منتهية", expired.toString(), Modifier.weight(1f))
        StatCard("القيمة", String.format(Locale.US, "%.2f", total), Modifier.weight(1.2f))
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(title, fontSize = 11.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductCard(p: Product, onClick: () -> Unit, onDelete: () -> Unit) {
    val color = when (p.status) {
        ExpiryStatus.VALID -> Color(0xFF2E7D32)
        ExpiryStatus.SOON -> Color(0xFFEF6C00)
        ExpiryStatus.EXPIRED -> Color(0xFFC62828)
    }

    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(p.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("الباركود: ${p.barcode}", fontSize = 12.sp)
                Text("السعر: ${p.price} × ${p.quantity}", fontSize = 12.sp)
                Text("الدخول: ${dateFmt.format(Date(p.entryDate))}", fontSize = 12.sp)
                Text("الصلاحية: ${dateFmt.format(Date(p.expiryDate))}", fontSize = 12.sp)
                Spacer(Modifier.height(6.dp))
                AssistChip(
                    onClick = {},
                    label = { Text(p.status.label, color = color, fontSize = 12.sp) }
                )
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "حذف", tint = Color(0xFFC62828)) }
        }
    }
}
