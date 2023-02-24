package com.chxzyfps.shoppinglist.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.chxzyfps.shoppinglist.entities.LibraryItem
import com.chxzyfps.shoppinglist.entities.NoteItem
import com.chxzyfps.shoppinglist.entities.ShopListItem
import com.chxzyfps.shoppinglist.entities.ShopListNameItem

@Database (entities = [LibraryItem:: class, NoteItem::class, ShopListItem::class, ShopListNameItem:: class], version = 1)
abstract class MainDataBase : RoomDatabase() {

    companion object {
        @Volatile
        private var INSTANCE: MainDataBase? = null
        fun getDatabase(context: Context): MainDataBase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MainDataBase::class.java,
                "shopping_list.db"
                )
                    .build()
                instance
            }
        }
    }

    abstract fun getDao() : Dao

}