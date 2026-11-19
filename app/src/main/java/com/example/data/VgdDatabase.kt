package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        WalletStateEntity::class,
        ExchangeRecordEntity::class,
        WalletActivityEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class VgdDatabase : RoomDatabase() {
    abstract fun walletDao(): WalletDao

    companion object {
        @Volatile
        private var INSTANCE: VgdDatabase? = null

        fun getInstance(context: Context): VgdDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    VgdDatabase::class.java,
                    "vgd_housie_db"
                )
                    .fallbackToDestructiveMigration(dropAllTables = false)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
