package com.axveer.slovo.data

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.axveer.slovo.db.SlovoDatabase

actual class DriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver =
        AndroidSqliteDriver(SlovoDatabase.Schema, context, "slovo.db")
}
