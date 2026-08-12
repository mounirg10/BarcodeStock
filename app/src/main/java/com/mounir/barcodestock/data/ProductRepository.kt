package com.mounir.barcodestock.data

import kotlinx.coroutines.flow.Flow

class ProductRepository(private val dao: ProductDao) {
    fun products(query: String): Flow<List<Product>> =
        if (query.isBlank()) dao.observeAll() else dao.search(query.trim())

    suspend fun findByBarcode(barcode: String) = dao.findByBarcode(barcode)
    suspend fun findById(id: Long) = dao.findById(id)
    suspend fun save(product: Product) = dao.upsert(product)
    suspend fun delete(product: Product) = dao.delete(product)
}
