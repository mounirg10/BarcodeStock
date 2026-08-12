package com.mounir.barcodestock.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mounir.barcodestock.data.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class ProductViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ProductRepository(AppDatabase.get(app).productDao())

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val products: StateFlow<List<Product>> = _query
        .flatMapLatest { repo.products(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(q: String) { _query.value = q }

    fun save(product: Product, onDone: () -> Unit = {}) = viewModelScope.launch {
        repo.save(product); onDone()
    }

    fun delete(product: Product) = viewModelScope.launch { repo.delete(product) }

    suspend fun findByBarcode(code: String) = repo.findByBarcode(code)
    suspend fun findById(id: Long) = repo.findById(id)
}
