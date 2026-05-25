package com.spendwise.data

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.spendwise.infrastructure.AndroidContextProvider

fun getSpendWiseDatabaseBuilder(context: Context): RoomDatabase.Builder<SpendWiseDatabase> {
    val appContext = context.applicationContext
    return Room.databaseBuilder<SpendWiseDatabase>(
        context = appContext,
        name = appContext.getDatabasePath("spendwise.db").absolutePath
    )
}

actual fun provideSpendWiseDatabaseBuilder(): RoomDatabase.Builder<SpendWiseDatabase> {
    return getSpendWiseDatabaseBuilder(AndroidContextProvider.context)
}

