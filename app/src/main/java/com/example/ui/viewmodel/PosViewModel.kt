package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.*
import com.example.data.repository.PosRepository
import com.example.data.terminal.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class PosTab(val title: String, val iconName: String) {
    PDV("Vendas / PDV", "point_of_sale"),
    TABLES("Mesas e Comandas", "table_restaurant"),
    PRODUCTS("Cardápio", "restaurant_menu"),
    TERMINALS("Maquininhas", "credit_card"),
    CASHIER("Caixa / Relatórios", "account_balance_wallet"),
    AI_ASSISTANT("IA Gourmet", "auto_awesome")
}

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    val terminalManager = PaymentTerminalManager()
    val repository = PosRepository(db, terminalManager)

    // UI Tab State
    private val _activeTab = MutableStateFlow(PosTab.PDV)
    val activeTab: StateFlow<PosTab> = _activeTab.asStateFlow()

    fun setActiveTab(tab: PosTab) {
        _activeTab.value = tab
    }

    // Selected Table / Command Name
    private val _selectedTableName = MutableStateFlow("Mesa 01")
    val selectedTableName: StateFlow<String> = _selectedTableName.asStateFlow()

    fun selectTableName(tableName: String) {
        _selectedTableName.value = tableName
        viewModelScope.launch {
            loadOrCreateOrderForTable(tableName)
        }
    }

    // Current Order for selected Table
    private val _currentOrder = MutableStateFlow<OrderEntity?>(null)
    val currentOrder: StateFlow<OrderEntity?> = _currentOrder.asStateFlow()

    // Current Order Items
    val currentOrderItems: StateFlow<List<OrderItemEntity>> = _currentOrder
        .flatMapLatest { order ->
            if (order != null) repository.getOrderItems(order.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Products & Categories
    val categories: StateFlow<List<CategoryEntity>> = repository.categories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    fun setSelectedCategory(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    val filteredProducts: StateFlow<List<ProductEntity>> = combine(
        repository.products,
        _selectedCategoryId,
        _searchQuery
    ) { products, catId, query ->
        products.filter { product ->
            (catId == null || product.categoryId == catId) &&
                    (query.isBlank() || product.name.contains(query, ignoreCase = true) || product.code.contains(query, ignoreCase = true))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Orders List (All Tables)
    val activeOrders: StateFlow<List<OrderEntity>> = repository.activeOrders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All Transactions
    val allTransactions: StateFlow<List<PaymentTransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Cashier Shift
    val currentCashierShift: StateFlow<CashierShiftEntity?> = repository.currentCashierShift
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Payment Dialog / Modal State
    private val _showPaymentModal = MutableStateFlow(false)
    val showPaymentModal: StateFlow<Boolean> = _showPaymentModal.asStateFlow()

    private val _selectedPaymentMethod = MutableStateFlow(PaymentMethod.CREDIT)
    val selectedPaymentMethod: StateFlow<PaymentMethod> = _selectedPaymentMethod.asStateFlow()

    private val _installmentCount = MutableStateFlow(1)
    val installmentCount: StateFlow<Int> = _installmentCount.asStateFlow()

    val terminalStep = terminalManager.currentStep
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TerminalStep.Idle)

    val selectedTerminalBrand = terminalManager.selectedTerminal

    // AI Assistant State
    private val _aiResponse = MutableStateFlow<String?>(null)
    val aiResponse: StateFlow<String?> = _aiResponse.asStateFlow()

    private val _isAiLoading = MutableStateFlow(false)
    val isAiLoading: StateFlow<Boolean> = _isAiLoading.asStateFlow()

    init {
        // Load initial table
        viewModelScope.launch {
            loadOrCreateOrderForTable(_selectedTableName.value)
        }
    }

    private suspend fun loadOrCreateOrderForTable(tableName: String) {
        val order = repository.createOrGetTableOrder(tableName)
        _currentOrder.value = order
    }

    fun addProductToCurrentOrder(product: ProductEntity, quantity: Int = 1, notes: String = "") {
        viewModelScope.launch {
            val order = _currentOrder.value ?: repository.createOrGetTableOrder(_selectedTableName.value)
            _currentOrder.value = order
            repository.addItemToOrder(order.id, product, quantity, notes)
            // Reload order to reflect updated total
            _currentOrder.value = repository.createOrGetTableOrder(_selectedTableName.value)
        }
    }

    fun updateOrderItemQuantity(item: OrderItemEntity, newQuantity: Int) {
        viewModelScope.launch {
            val order = _currentOrder.value ?: return@launch
            repository.updateItemQuantity(order.id, item.id, newQuantity)
            _currentOrder.value = repository.createOrGetTableOrder(_selectedTableName.value)
        }
    }

    fun toggleServiceTip(includeTip: Boolean) {
        viewModelScope.launch {
            val order = _currentOrder.value ?: return@launch
            repository.recalculateOrderTotals(order.id, includeTip)
            _currentOrder.value = repository.createOrGetTableOrder(_selectedTableName.value)
        }
    }

    fun openPaymentModal() {
        _showPaymentModal.value = true
        terminalManager.resetTerminal()
    }

    fun closePaymentModal() {
        _showPaymentModal.value = false
        terminalManager.resetTerminal()
    }

    fun setPaymentMethod(method: PaymentMethod) {
        _selectedPaymentMethod.value = method
    }

    fun setInstallments(count: Int) {
        _installmentCount.value = count
    }

    fun setTerminalBrand(brand: TerminalBrand) {
        terminalManager.selectTerminalBrand(brand)
    }

    fun processTerminalPayment() {
        val order = _currentOrder.value ?: return
        if (order.total <= 0) return

        viewModelScope.launch {
            val result = terminalManager.startPayment(
                amount = order.total,
                method = _selectedPaymentMethod.value,
                installments = _installmentCount.value,
                tableOrCommandName = order.tableOrCommandName
            )

            if (result is PaymentResult.Success) {
                repository.recordPaymentAndCompleteOrder(
                    orderId = order.id,
                    paymentMethod = _selectedPaymentMethod.value,
                    terminalBrand = selectedTerminalBrand.value,
                    amount = order.total,
                    nsu = result.nsu,
                    authCode = result.authCode,
                    cardBrand = result.cardBrand,
                    installments = _installmentCount.value
                )
            }
        }
    }

    fun confirmPixPaymentInTerminal() {
        val order = _currentOrder.value ?: return
        viewModelScope.launch {
            val result = terminalManager.simulatePixConfirm(order.total, order.tableOrCommandName)
            repository.recordPaymentAndCompleteOrder(
                orderId = order.id,
                paymentMethod = PaymentMethod.PIX,
                terminalBrand = selectedTerminalBrand.value,
                amount = order.total,
                nsu = result.nsu,
                authCode = result.authCode,
                cardBrand = "PIX",
                installments = 1
            )
        }
    }

    fun openCashierShift(initialFloat: Double) {
        viewModelScope.launch {
            repository.openCashierShift(initialFloat)
        }
    }

    fun closeCashierShift() {
        viewModelScope.launch {
            repository.closeCashierShift()
        }
    }

    fun addNewProduct(name: String, categoryId: Long, price: Double, description: String, code: String) {
        viewModelScope.launch {
            repository.addProduct(
                ProductEntity(
                    name = name,
                    categoryId = categoryId,
                    price = price,
                    description = description,
                    code = code
                )
            )
        }
    }

    fun askAiAssistant(prompt: String) {
        viewModelScope.launch {
            _isAiLoading.value = true
            _aiResponse.value = repository.getAiRestaurantInsight(prompt)
            _isAiLoading.value = false
        }
    }
}
