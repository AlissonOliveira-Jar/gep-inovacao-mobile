package com.unichristus.leitor_fiscal.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns

class CupomDbHelper(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_VERSION = 1
        const val DATABASE_NAME = "LeitorCupom.db"

        private const val SQL_CREATE_CUPONS_TABLE =
            "CREATE TABLE ${CupomDbContract.CupomEntry.TABLE_NAME} (" +
                    "${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "${CupomDbContract.CupomEntry.COLUMN_STORE_NAME} TEXT," +
                    "${CupomDbContract.CupomEntry.COLUMN_CNPJ} TEXT," +
                    "${CupomDbContract.CupomEntry.COLUMN_ADDRESS} TEXT," +
                    "${CupomDbContract.CupomEntry.COLUMN_DATE_TIME} TEXT," +
                    "${CupomDbContract.CupomEntry.COLUMN_CCF} TEXT," +
                    "${CupomDbContract.CupomEntry.COLUMN_COO} TEXT," +
                    "${CupomDbContract.CupomEntry.COLUMN_TOTAL_AMOUNT} TEXT," +
                    "${CupomDbContract.CupomEntry.COLUMN_SCANNED_AT_TIMESTAMP} INTEGER)"

        private const val SQL_CREATE_PRODUCTS_TABLE =
            "CREATE TABLE ${CupomDbContract.ProductEntry.TABLE_NAME} (" +
                    "${CupomDbContract.ProductEntry.COLUMN_PRODUCT_UUID} TEXT PRIMARY KEY," +
                    "${CupomDbContract.ProductEntry.COLUMN_CUPOM_ID_FK} INTEGER," +
                    "${CupomDbContract.ProductEntry.COLUMN_CODE} TEXT," +
                    "${CupomDbContract.ProductEntry.COLUMN_NAME} TEXT," +
                    "${CupomDbContract.ProductEntry.COLUMN_QUANTITY} TEXT," +
                    "${CupomDbContract.ProductEntry.COLUMN_UNIT_PRICE} TEXT," +
                    "${CupomDbContract.ProductEntry.COLUMN_TOTAL_PRICE} TEXT," +
                    "${CupomDbContract.ProductEntry.COLUMN_DISCOUNT} TEXT," +
                    "FOREIGN KEY (${CupomDbContract.ProductEntry.COLUMN_CUPOM_ID_FK}) REFERENCES " +
                    "${CupomDbContract.CupomEntry.TABLE_NAME}(${BaseColumns._ID}) ON DELETE CASCADE)"


        private const val SQL_DELETE_CUPONS_TABLE = "DROP TABLE IF EXISTS ${CupomDbContract.CupomEntry.TABLE_NAME}"
        private const val SQL_DELETE_PRODUCTS_TABLE = "DROP TABLE IF EXISTS ${CupomDbContract.ProductEntry.TABLE_NAME}"
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(SQL_CREATE_CUPONS_TABLE)
        db.execSQL(SQL_CREATE_PRODUCTS_TABLE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL(SQL_DELETE_PRODUCTS_TABLE)
        db.execSQL(SQL_DELETE_CUPONS_TABLE)
        onCreate(db)
    }

    override fun onDowngrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        onUpgrade(db, oldVersion, newVersion)
    }
}