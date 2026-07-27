package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val iconName: String,
    val colorHex: String
)

// Nota sobre dinheiro: todo campo que termina em "Cents" guarda o valor
// em CENTAVOS (Long), nunca em reais/Double. Veja com.example.util.Money
// para as funções de formatação (toBRL()) e conversão de texto digitado
// (toCentsOrNull()). Isso evita erros de arredondamento de ponto
// flutuante em cálculos financeiros (ex: 0.1 + 0.2 != 0.3 em Double).

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val name: String,
    val description: String,
    val priceCents: Long,
    val isAvailable: Boolean = true,
    val code: String = "",
    val iconName: String = "restaurant"
)

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderNumber: Int,
    val tableOrCommandName: String, // e.g. "Mesa 04", "Comanda 12", "Balcão"
    val status: String, // "OPEN", "PENDING_PAYMENT", "COMPLETED", "CANCELLED"
    val subtotalCents: Long,
    val serviceTipCents: Long,
    val totalCents: Long,
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val customerCount: Int = 1
)

@Entity(tableName = "order_items")
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val productId: Long,
    val productName: String,
    val unitPriceCents: Long,
    val quantity: Int,
    val notes: String = ""
)

@Entity(tableName = "payment_transactions")
data class PaymentTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val paymentMethod: String, // "CREDIT", "DEBIT", "PIX", "CASH", "VOUCHER"
    val terminalType: String, // "STONE", "PAGBANK", "CIELO", "REDE", "GENERIC_SMART"
    val amountCents: Long,
    val status: String, // "SUCCESS", "FAILED", "CANCELLED"
    val nsu: String,
    val authCode: String,
    val cardBrand: String, // "VISA", "MASTERCARD", "ELO", "AMEX", "PIX", "MONEY"
    val installments: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "cashier_shifts")
data class CashierShiftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val openedAt: Long = System.currentTimeMillis(),
    val closedAt: Long? = null,
    val initialFloatCents: Long,
    val totalCreditCents: Long = 0,
    val totalDebitCents: Long = 0,
    val totalPixCents: Long = 0,
    val totalCashCents: Long = 0,
    val totalVoucherCents: Long = 0,
    val status: String = "OPEN" // "OPEN", "CLOSED"
)
