package com.unichristus.leitor_fiscal.data.db

import android.provider.BaseColumns

object CupomDbContract {
    object CupomEntry : BaseColumns {
        const val TABLE_NAME = "cupons_salvos"
        const val COLUMN_STORE_NAME = "store_name"
        const val COLUMN_CNPJ = "cnpj"
        const val COLUMN_ADDRESS = "address"
        const val COLUMN_DATE_TIME = "date_time"
        const val COLUMN_CCF = "ccf"
        const val COLUMN_COO = "coo"
        const val COLUMN_TOTAL_AMOUNT = "total_amount"
        const val COLUMN_SCANNED_AT_TIMESTAMP = "scanned_at_timestamp"
    }

    object ProductEntry : BaseColumns {
        const val TABLE_NAME = "produtos_salvos"
        const val COLUMN_PRODUCT_UUID = "product_uuid"
        const val COLUMN_CUPOM_ID_FK = "cupom_id_fk"
        const val COLUMN_CODE = "code"
        const val COLUMN_NAME = "name"
        const val COLUMN_QUANTITY = "quantity"
        const val COLUMN_UNIT_PRICE = "unit_price"
        const val COLUMN_TOTAL_PRICE = "total_price"
        const val COLUMN_DISCOUNT = "discount"
    }
}