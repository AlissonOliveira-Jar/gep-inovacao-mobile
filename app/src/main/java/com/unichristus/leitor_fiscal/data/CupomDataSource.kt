// Local: com/unichristus/leitor_fiscal/data/CupomDataSource.kt
package com.unichristus.leitor_fiscal.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.provider.BaseColumns
import android.util.Log
import androidx.core.database.sqlite.transaction
import com.unichristus.leitor_fiscal.data.db.CupomDbContract
import com.unichristus.leitor_fiscal.data.db.CupomDbHelper
import java.io.IOException

class CupomDataSource(context: Context) {

    private val dbHelper = CupomDbHelper(context)

    fun insertCupomAndProducts(cupomInfo: CupomInfo, products: List<Product>): Long {
        val db: SQLiteDatabase = dbHelper.writableDatabase
        var generatedCupomId: Long = -1L

        try {
            db.transaction {
                val cupomValues = ContentValues().apply {
                    put(CupomDbContract.CupomEntry.COLUMN_STORE_NAME, cupomInfo.storeName)
                    put(CupomDbContract.CupomEntry.COLUMN_CNPJ, cupomInfo.cnpj)
                    put(CupomDbContract.CupomEntry.COLUMN_ADDRESS, cupomInfo.address)
                    put(CupomDbContract.CupomEntry.COLUMN_DATE_TIME, cupomInfo.dateTime)
                    put(CupomDbContract.CupomEntry.COLUMN_CCF, cupomInfo.ccf)
                    put(CupomDbContract.CupomEntry.COLUMN_COO, cupomInfo.coo)
                    put(CupomDbContract.CupomEntry.COLUMN_TOTAL_AMOUNT, cupomInfo.totalAmount)
                    put(CupomDbContract.CupomEntry.COLUMN_SCANNED_AT_TIMESTAMP, System.currentTimeMillis())
                }

                generatedCupomId = insert(CupomDbContract.CupomEntry.TABLE_NAME, null, cupomValues)

                if (generatedCupomId == -1L) {
                    throw IOException("Falha ao inserir cupom principal.")
                }

                if (products.isNotEmpty()) {
                    for (product in products) {
                        val productValues = ContentValues().apply {
                            put(CupomDbContract.ProductEntry.COLUMN_PRODUCT_UUID, product.id)
                            put(CupomDbContract.ProductEntry.COLUMN_CUPOM_ID_FK, generatedCupomId)
                            put(CupomDbContract.ProductEntry.COLUMN_CODE, product.code)
                            put(CupomDbContract.ProductEntry.COLUMN_NAME, product.name)
                            put(CupomDbContract.ProductEntry.COLUMN_QUANTITY, product.quantity)
                            put(CupomDbContract.ProductEntry.COLUMN_UNIT_PRICE, product.unitPrice)
                            put(CupomDbContract.ProductEntry.COLUMN_TOTAL_PRICE, product.totalPrice)
                            put(CupomDbContract.ProductEntry.COLUMN_DISCOUNT, product.discount)
                        }
                        val productInsertResult = insert(CupomDbContract.ProductEntry.TABLE_NAME, null, productValues)
                        if (productInsertResult == -1L) {
                            throw IOException("Falha ao inserir produto: ${product.name}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("CupomDataSource", "Erro na transação ao inserir cupom e produtos", e)
            generatedCupomId = -1L
        }
        return generatedCupomId
    }

    fun getAllSavedCupons(): List<CupomInfo> {
        val cuponsList = mutableListOf<CupomInfo>()
        val db: SQLiteDatabase = dbHelper.readableDatabase
        var cursor: Cursor? = null

        try {
            val projection = arrayOf(
                BaseColumns._ID,
                CupomDbContract.CupomEntry.COLUMN_STORE_NAME,
                CupomDbContract.CupomEntry.COLUMN_CNPJ,
                CupomDbContract.CupomEntry.COLUMN_ADDRESS,
                CupomDbContract.CupomEntry.COLUMN_DATE_TIME,
                CupomDbContract.CupomEntry.COLUMN_CCF,
                CupomDbContract.CupomEntry.COLUMN_COO,
                CupomDbContract.CupomEntry.COLUMN_TOTAL_AMOUNT,
                CupomDbContract.CupomEntry.COLUMN_SCANNED_AT_TIMESTAMP
            )
            val sortOrder = "${CupomDbContract.CupomEntry.COLUMN_SCANNED_AT_TIMESTAMP} DESC"
            cursor = db.query(
                CupomDbContract.CupomEntry.TABLE_NAME, projection, null, null, null, null, sortOrder
            )
            with(cursor) {
                while (moveToNext()) {
                    val id = getLong(getColumnIndexOrThrow(BaseColumns._ID))
                    val storeName = getString(getColumnIndexOrThrow(CupomDbContract.CupomEntry.COLUMN_STORE_NAME))
                    val cnpj = getString(getColumnIndexOrThrow(CupomDbContract.CupomEntry.COLUMN_CNPJ))
                    val address = getString(getColumnIndexOrThrow(CupomDbContract.CupomEntry.COLUMN_ADDRESS))
                    val dateTime = getString(getColumnIndexOrThrow(CupomDbContract.CupomEntry.COLUMN_DATE_TIME))
                    val ccf = getString(getColumnIndexOrThrow(CupomDbContract.CupomEntry.COLUMN_CCF))
                    val coo = getString(getColumnIndexOrThrow(CupomDbContract.CupomEntry.COLUMN_COO))
                    val totalAmount = getString(getColumnIndexOrThrow(CupomDbContract.CupomEntry.COLUMN_TOTAL_AMOUNT))
                    val scannedAt = getLong(getColumnIndexOrThrow(CupomDbContract.CupomEntry.COLUMN_SCANNED_AT_TIMESTAMP))

                    cuponsList.add(
                        CupomInfo(
                            id = id,
                            storeName = storeName,
                            cnpj = cnpj,
                            address = address,
                            dateTime = dateTime,
                            ccf = ccf,
                            coo = coo,
                            totalAmount = totalAmount,
                            scannedAtTimestamp = scannedAt
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("CupomDataSource", "Erro ao buscar todos os cupons", e)
        } finally {
            cursor?.close()
        }
        return cuponsList
    }
}