package com.chxzyfps.shoppinglist.activities

import android.app.Application
import com.chxzyfps.shoppinglist.db.MainDataBase

class MainApp : Application() {
    val database by lazy { MainDataBase.getDatabase(this)}
}