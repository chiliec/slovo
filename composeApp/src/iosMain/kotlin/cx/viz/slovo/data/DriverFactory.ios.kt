package cx.viz.slovo.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import cx.viz.slovo.db.SlovoDatabase

actual class DriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(SlovoDatabase.Schema, "slovo.db")
}
