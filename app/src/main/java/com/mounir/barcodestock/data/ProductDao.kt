package com.mounir.barcodestock.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ProductDao {

    @Query("SELECT * FROM products ORDER BY expiryDate ASC")
    fun observeAll(): Flow<List<Product>>

    @Query("SELECT * FROM products WHERE name LIKE '%' || :q || '%' OR barcode LIKE '%' || :q || '%' ORDER BY expiryDate ASC")
    fun search(q: String): Flow<List<Product>>

    @Query("SELECT * FROM products ORDER BY expiryDate ASC")
    suspend fun allOnce(): List<Product>

    @Query("SELECT * FROM products WHERE expiryDate <= :threshold ORDER BY expiryDate ASC")
    suspend fun expiringBefore(threshold: Long): List<Product>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    suspend fun findByBarcode(barcode: String): Product?

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    suspend fun findById(id: Long): Product?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(product: Product): Long

    @Delete
    suspend fun delete(product: Product)
}
