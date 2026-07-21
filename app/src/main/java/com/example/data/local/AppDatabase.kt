package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CategoryEntity::class,
        ProductEntity::class,
        OrderEntity::class,
        OrderItemEntity::class,
        PaymentTransactionEntity::class,
        CashierShiftEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun categoryDao(): CategoryDao
    abstract fun productDao(): ProductDao
    abstract fun orderDao(): OrderDao
    abstract fun orderItemDao(): OrderItemDao
    abstract fun paymentTransactionDao(): PaymentTransactionDao
    abstract fun cashierShiftDao(): CashierShiftDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pdv_gourmet_database"
                )
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        suspend fun populateInitialData(db: AppDatabase) {
            val categoryDao = db.categoryDao()
            val productDao = db.productDao()
            val cashierDao = db.cashierShiftDao()

            // Pre-populate Categories
            val catBurgerId = categoryDao.insertCategory(CategoryEntity(name = "Hambúrgueres", iconName = "fastfood", colorHex = "#FF5722"))
            val catPratosId = categoryDao.insertCategory(CategoryEntity(name = "Pratos & Carnes", iconName = "restaurant", colorHex = "#E91E63"))
            val catPorcoesId = categoryDao.insertCategory(CategoryEntity(name = "Porções", iconName = "tapas", colorHex = "#FF9800"))
            val catBebidasId = categoryDao.insertCategory(CategoryEntity(name = "Bebidas", iconName = "local_bar", colorHex = "#2196F3"))
            val catSobremesasId = categoryDao.insertCategory(CategoryEntity(name = "Sobremesas", iconName = "icecream", colorHex = "#9C27B0"))

            // Pre-populate Products
            val products = listOf(
                // Hambúrgueres
                ProductEntity(categoryId = catBurgerId, name = "Burger Gourmet Artesanal", description = "Pão brioche, blend 180g, queijo cheddar, bacon crocante e maionese da casa", price = 34.90, code = "BUR01", iconName = "lunch_dining"),
                ProductEntity(categoryId = catBurgerId, name = "Smash Double Cheese", description = "2 discos de 90g, duplo queijo prato, picles e molho especial", price = 28.50, code = "BUR02", iconName = "lunch_dining"),
                ProductEntity(categoryId = catBurgerId, name = "Burger Vegetariano Cogumelos", description = "Burger de cogumelos e grão de bico, queijo de búfala e rúcula", price = 32.00, code = "BUR03", iconName = "lunch_dining"),

                // Pratos
                ProductEntity(categoryId = catPratosId, name = "Picanha na Chapa (G)", description = "Picanha fatiada 500g com alho assado, farofa rica, vinagrete e mandioca", price = 119.90, code = "PRA01", iconName = "dinner_dining"),
                ProductEntity(categoryId = catPratosId, name = "Filé Mignon ao Molho Madeira", description = "Acompanha arroz à grega e batatas rústicas douradas", price = 64.00, code = "PRA02", iconName = "dinner_dining"),
                ProductEntity(categoryId = catPratosId, name = "Feijoada Completa Individual", description = "Carnes nobres, arroz, couve refogada, farofa e torresmo crocante", price = 48.90, code = "PRA03", iconName = "dinner_dining"),

                // Porções
                ProductEntity(categoryId = catPorcoesId, name = "Batata Frita Especial com Cheddar & Bacon", description = "500g de batata rústica crocante coberta com cheddar cremoso", price = 32.90, code = "POR01", iconName = "tapas"),
                ProductEntity(categoryId = catPorcoesId, name = "Iscas de Peixe com Molho Tártaro", description = "Tilápia empanada na farinha panko bem crocante", price = 45.00, code = "POR02", iconName = "set_meal"),
                ProductEntity(categoryId = catPorcoesId, name = "Mandioca Frita com Carne de Sol", description = "Mandioca macia por dentro e crocante por fora com queijo coalho", price = 39.90, code = "POR03", iconName = "tapas"),

                // Bebidas
                ProductEntity(categoryId = catBebidasId, name = "Chopp Artesanal 500ml", description = "Pilsen trincando de gelado direto da torneira", price = 12.00, code = "BEB01", iconName = "sports_bar"),
                ProductEntity(categoryId = catBebidasId, name = "Caipirinha de Limão Cachaça Premium", description = "Limão taiti, açúcar de baunilha e cachaça envelhecida", price = 18.00, code = "BEB02", iconName = "local_bar"),
                ProductEntity(categoryId = catBebidasId, name = "Suco Natural de Laranja 400ml", description = "100% fruta natural espremido na hora", price = 9.50, code = "BEB03", iconName = "local_drink"),
                ProductEntity(categoryId = catBebidasId, name = "Guaraná Antarctica Lata 350ml", description = "Refrigerante lata bem gelado", price = 6.50, code = "BEB04", iconName = "local_drink"),

                // Sobremesas
                ProductEntity(categoryId = catSobremesasId, name = "Pudim de Leite da Vovó", description = "Pudim cremoso sem furinhos com calda caramelizada", price = 14.90, code = "SOB01", iconName = "icecream"),
                ProductEntity(categoryId = catSobremesasId, name = "Petit Gâteau com Sorvete de Baunilha", description = "Bolo quente de chocolate belga com recheio cremoso escorrendo", price = 24.90, code = "SOB02", iconName = "cake")
            )

            productDao.insertProducts(products)

            // Open initial cashier shift automatically if none exists
            if (cashierDao.getCurrentShiftSync() == null) {
                cashierDao.insertShift(
                    CashierShiftEntity(
                        initialFloat = 200.0,
                        openedAt = System.currentTimeMillis() - 3600000, // 1 hr ago
                        status = "OPEN"
                    )
                )
            }
        }
    }
}
