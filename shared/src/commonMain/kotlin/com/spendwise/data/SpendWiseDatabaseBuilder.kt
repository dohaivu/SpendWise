package com.spendwise.data

import androidx.room3.RoomDatabase

expect fun provideSpendWiseDatabaseBuilder(): RoomDatabase.Builder<SpendWiseDatabase>

