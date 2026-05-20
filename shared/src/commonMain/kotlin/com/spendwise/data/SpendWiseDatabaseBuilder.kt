package com.spendwise.data

import androidx.room.RoomDatabase

expect fun provideSpendWiseDatabaseBuilder(): RoomDatabase.Builder<SpendWiseDatabase>

