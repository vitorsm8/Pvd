package com.example.data.repository

import com.example.BuildConfig
import com.example.data.local.*
import com.example.data.terminal.PaymentMethod
import com.example.data.terminal.PaymentTerminalManager
import com.example.data.terminal.TerminalBrand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

class PosRepository(
    private val db: AppDatabase,
    val terminalManager: PaymentTerminalManager
) {
    private val categoryDao = db.categoryDao()
    private val productDao = db.productDao()
    private val orderDao = db.orderDao()
    private val orderItemDao = db.orderItemDao()
    private val paymentTransactionDao = db.paymentTransactionDao()
    private val cashierShiftDao = db.cashierShiftDao()

    val categories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    val products: Flow<List<ProductEntity>> = productDao.getAllProducts()
    val activeOrders: Flow<List<OrderEntity>> = orderDao.getActiveOrders()
    val allTransactions: Flow<List<PaymentTransactionEntity>> = paymentTransactionDao.getAllTransactions()
    val currentCashierShift: Flow<CashierShiftEntity?> = cashierShiftDao.getCurrentShift()

    fun getProductsByCategory(categoryId: Long): Flow<List<ProductEntity>> = productDao.getProductsByCategory(categoryId)
    fun getOrderItems(orderId: Long): Flow<List<OrderItemEntity>> = orderItemDao.getItemsForOrder(orderId)

    suspend fun createOrGetTableOrder(tableOrCommandName: String): OrderEntity {
        val existing = orderDao.getActiveOrderByTable(tableOrCommandName)
        if (existing != null) {
            return existing
        }

        val count = (orderDao.getAllOrders().firstOrNull()?.size ?: 0) + 1
        val newOrder = OrderEntity(
            orderNumber = count,
            tableOrCommandName = tableOrCommandName,
            status = "OPEN",
            subtotal = 0.0,
            serviceTip = 0.0,
            total = 0.0
        )
        val id = orderDao.insertOrder(newOrder)
        return newOrder.copy(id = id)
    }

    suspend fun addItemToOrder(
        orderId: Long,
        product: ProductEntity,
        quantity: Int = 1,
        notes: String = ""
    ) {
        val currentItems = orderItemDao.getItemsForOrderSync(orderId)
        val existingItem = currentItems.find { it.productId == product.id && it.notes == notes }

        if (existingItem != null) {
            val updated = existingItem.copy(quantity = existingItem.quantity + quantity)
            orderItemDao.updateOrderItem(updated)
        } else {
            val newItem = OrderItemEntity(
                orderId = orderId,
                productId = product.id,
                productName = product.name,
                unitPrice = product.price,
                quantity = quantity,
                notes = notes
            )
            orderItemDao.insertOrderItem(newItem)
        }

        recalculateOrderTotals(orderId, includeTip = true)
    }

    suspend fun updateItemQuantity(orderId: Long, itemId: Long, newQuantity: Int) {
        if (newQuantity <= 0) {
            orderItemDao.deleteOrderItem(itemId)
        } else {
            val items = orderItemDao.getItemsForOrderSync(orderId)
            val item = items.find { it.id == itemId }
            if (item != null) {
                orderItemDao.updateOrderItem(item.copy(quantity = newQuantity))
            }
        }
        recalculateOrderTotals(orderId, includeTip = true)
    }

    suspend fun recalculateOrderTotals(orderId: Long, includeTip: Boolean = true) {
        val order = orderDao.getOrderById(orderId) ?: return
        val items = orderItemDao.getItemsForOrderSync(orderId)
        val subtotal = items.sumOf { it.unitPrice * it.quantity }
        val tip = if (includeTip && subtotal > 0) subtotal * 0.10 else 0.0
        val total = subtotal + tip

        orderDao.updateOrder(
            order.copy(
                subtotal = subtotal,
                serviceTip = tip,
                total = total
            )
        )
    }

    suspend fun recordPaymentAndCompleteOrder(
        orderId: Long,
        paymentMethod: PaymentMethod,
        terminalBrand: TerminalBrand,
        amount: Double,
        nsu: String,
        authCode: String,
        cardBrand: String,
        installments: Int = 1
    ) {
        val transaction = PaymentTransactionEntity(
            orderId = orderId,
            paymentMethod = paymentMethod.name,
            terminalType = terminalBrand.name,
            amount = amount,
            status = "SUCCESS",
            nsu = nsu,
            authCode = authCode,
            cardBrand = cardBrand,
            installments = installments,
            timestamp = System.currentTimeMillis()
        )
        paymentTransactionDao.insertTransaction(transaction)

        // Mark order as COMPLETED
        orderDao.updateOrderStatus(orderId, "COMPLETED")

        // Update current Cashier Shift totals
        val shift = cashierShiftDao.getCurrentShiftSync()
        if (shift != null && shift.status == "OPEN") {
            val updatedShift = when (paymentMethod) {
                PaymentMethod.CREDIT -> shift.copy(totalCredit = shift.totalCredit + amount)
                PaymentMethod.DEBIT -> shift.copy(totalDebit = shift.totalDebit + amount)
                PaymentMethod.PIX -> shift.copy(totalPix = shift.totalPix + amount)
                PaymentMethod.CASH -> shift.copy(totalCash = shift.totalCash + amount)
                PaymentMethod.VOUCHER -> shift.copy(totalVoucher = shift.totalVoucher + amount)
            }
            cashierShiftDao.updateShift(updatedShift)
        }
    }

    suspend fun openCashierShift(initialFloat: Double) {
        cashierShiftDao.insertShift(
            CashierShiftEntity(
                openedAt = System.currentTimeMillis(),
                initialFloat = initialFloat,
                status = "OPEN"
            )
        )
    }

    suspend fun closeCashierShift() {
        val shift = cashierShiftDao.getCurrentShiftSync()
        if (shift != null && shift.status == "OPEN") {
            cashierShiftDao.updateShift(
                shift.copy(
                    closedAt = System.currentTimeMillis(),
                    status = "CLOSED"
                )
            )
        }
    }

    suspend fun addProduct(product: ProductEntity): Long = productDao.insertProduct(product)
    suspend fun updateProduct(product: ProductEntity) = productDao.updateProduct(product)
    suspend fun deleteProduct(id: Long) = productDao.deleteProduct(id)

    suspend fun addCategory(name: String, iconName: String, colorHex: String): Long {
        return categoryDao.insertCategory(CategoryEntity(name = name, iconName = iconName, colorHex = colorHex))
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    // Gemini AI Assistant for Restaurant Insights
    suspend fun getAiRestaurantInsight(userPrompt: String): String = withContext(Dispatchers.IO) {
        try {
            val shift = cashierShiftDao.getCurrentShiftSync()
            val totalShiftSales = (shift?.totalCredit ?: 0.0) +
                    (shift?.totalDebit ?: 0.0) +
                    (shift?.totalPix ?: 0.0) +
                    (shift?.totalCash ?: 0.0) +
                    (shift?.totalVoucher ?: 0.0)

            val systemContext = """
                Você é o assistente inteligente de inteligência artificial do sistema 'PDV Gourmet' para restaurantes em português.
                Dados em tempo real do restaurante:
                - Status do Caixa: ${shift?.status ?: "Não aberto"}
                - Fundo de Caixa Inicial: R$ ${String.format("%.2f", shift?.initialFloat ?: 0.0)}
                - Total Vendas no Turno Atual: R$ ${String.format("%.2f", totalShiftSales)}
                  - Crédito: R$ ${String.format("%.2f", shift?.totalCredit ?: 0.0)}
                  - Débito: R$ ${String.format("%.2f", shift?.totalDebit ?: 0.0)}
                  - PIX: R$ ${String.format("%.2f", shift?.totalPix ?: 0.0)}
                  - Dinheiro: R$ ${String.format("%.2f", shift?.totalCash ?: 0.0)}
                  - Vale Refeição: R$ ${String.format("%.2f", shift?.totalVoucher ?: 0.0)}
                
                Responda com cortesia, objetividade e foco comercial em gestão de restaurantes, sugestões de harmonização de pratos, promoções do dia e dicas de vendas.
            """.trimIndent()

            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext "💡 [Dica do Garçom IA]: Oferecer sobremesas e bebidas trincando aumenta o ticket médio em até 18%! No turno atual você acumula R$ ${String.format("%.2f", totalShiftSales)} em vendas."
            }

            val jsonBody = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", "$systemContext\n\nPergunta do Garçom/Gerente: $userPrompt")
                            })
                        })
                    })
                })
            }

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val requestBody = jsonBody.toString().toRequestBody(mediaType)
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey")
                .post(requestBody)
                .build()

            val response = httpClient.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val jsonResponse = JSONObject(responseString)
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.getJSONObject("content")
                    val parts = content.getJSONArray("parts")
                    if (parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).getString("text")
                    }
                }
            }

            "💡 [Dica Gourmet]: Para vender mais no almoço, ofereça aos clientes um combo de 'Prato Principal + Suco Natural' com desconto especial!"
        } catch (e: Exception) {
            "💡 [Dica Gourmet]: Ofertar sobremesas crocantes e bebidas bem geladas ao fechar a conta eleva a satisfação dos clientes e aumenta o faturamento!"
        }
    }
}
