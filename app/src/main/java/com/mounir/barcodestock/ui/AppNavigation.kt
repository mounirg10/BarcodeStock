package com.mounir.barcodestock.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mounir.barcodestock.sale.SaleScreen
import com.mounir.barcodestock.sale.SaleViewModel
import com.mounir.barcodestock.scanner.ScannerScreen

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val vm: ProductViewModel = viewModel()
    val saleVm: SaleViewModel = viewModel()

    NavHost(navController = nav, startDestination = "list") {

        composable("list") {
            ProductListScreen(
                vm = vm,
                onScan = { nav.navigate("scanner") },
                onAdd = { nav.navigate("form?id=0&barcode=") },
                onEdit = { id -> nav.navigate("form?id=$id&barcode=") },
                onSale = { nav.navigate("sale") }
            )
        }

        composable("scanner") {
            ScannerScreen(
                onBarcode = { code ->
                    nav.navigate("form?id=0&barcode=$code") {
                        popUpTo("scanner") { inclusive = true }
                    }
                },
                onBack = { nav.popBackStack() }
            )
        }

        composable("sale") {
            SaleScreen(
                vm = saleVm,
                onBack = { nav.popBackStack() },
                onAddUnknown = { code -> nav.navigate("form?id=0&barcode=$code") }
            )
        }

        composable(
            route = "form?id={id}&barcode={barcode}",
            arguments = listOf(
                navArgument("id") { type = NavType.LongType; defaultValue = 0L },
                navArgument("barcode") { type = NavType.StringType; defaultValue = "" }
            )
        ) { entry ->
            ProductFormScreen(
                vm = vm,
                productId = entry.arguments?.getLong("id"),
                scannedBarcode = entry.arguments?.getString("barcode"),
                onDone = { nav.popBackStack("list", inclusive = false) },
                onBack = { nav.popBackStack() }
            )
        }
    }
}
