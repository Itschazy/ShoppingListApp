package com.chxzyfps.shoppinglist.activities

import android.content.Intent
import android.content.SharedPreferences
import android.opengl.Visibility
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.MenuItem.OnActionExpandListener
import android.view.View
import android.widget.EditText
import androidx.activity.viewModels
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.chxzyfps.shoppinglist.R
import com.chxzyfps.shoppinglist.databinding.ActivityShopListBinding
import com.chxzyfps.shoppinglist.db.MainViewModel
import com.chxzyfps.shoppinglist.db.ShopListItemAdapter
import com.chxzyfps.shoppinglist.dialogs.EditListItemDialog
import com.chxzyfps.shoppinglist.entities.LibraryItem
import com.chxzyfps.shoppinglist.entities.ShopListItem
import com.chxzyfps.shoppinglist.entities.ShopListNameItem
import com.chxzyfps.shoppinglist.utils.ShareHelper
import org.w3c.dom.Text

class ShopListActivity : AppCompatActivity(), ShopListItemAdapter.Listener {

    private lateinit var binding: ActivityShopListBinding
    private var shopListNameItem: ShopListNameItem? = null
    private lateinit var saveItem: MenuItem
    private var edItem: EditText? = null
    private var adapter: ShopListItemAdapter? = null
    private lateinit var textWatcher: TextWatcher
    private lateinit var defPref: SharedPreferences


    private val mainViewModel: MainViewModel by viewModels {
        MainViewModel.MainViewModelFactory((applicationContext as MainApp).database)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        defPref = PreferenceManager.getDefaultSharedPreferences(this)
        setTheme(getSelectedTheme())
        super.onCreate(savedInstanceState)
        binding = ActivityShopListBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
        initRcView()
        listItemObserver()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.shop_list_menu, menu)
        saveItem = menu?.findItem(R.id.save_item)!!
        val newItem = menu.findItem(R.id.new_item)
        edItem = newItem.actionView?.findViewById(R.id.edNewShopItem) as EditText
        newItem.setOnActionExpandListener(expandActionView())
        saveItem.isVisible = false
        textWatcher = textWatcher()
        return true
    }

    private fun textWatcher(): TextWatcher{
        return object : TextWatcher {
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

            }

            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                Log.d("My Log", "On text changed: $p0")
                mainViewModel.getAllLibraryItems("%$p0%")
            }

            override fun afterTextChanged(p0: Editable?) {

            }

        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.save_item -> {
                addNewShopItem(edItem?.text.toString())
            }
            R.id.delete_list -> {
                mainViewModel.deleteShopList(shopListNameItem?.id!!, true)
                finish()
            }
            R.id.clear_list -> {
                mainViewModel.deleteShopList(shopListNameItem?.id!!, false)
            }
            R.id.share_list -> {
                startActivity(Intent.createChooser(ShareHelper.shareShopList(adapter?.currentList!!, shopListNameItem?.name!!), "Share by"))
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun addNewShopItem(name: String){
        if(name.toString().isEmpty()) return
        val item = ShopListItem(
            null,
            name.toString(),
            "",
            false,
            shopListNameItem?.id!!,
            0
        )
        edItem?.setText("")
        mainViewModel.insertShopItem(item)
    }

    private fun listItemObserver(){
        mainViewModel.getAllItemsFromList(shopListNameItem?.id!!).observe(this) {
            adapter?.submitList(it)
            binding.tvEmpty.visibility = if(it.isEmpty()){
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    private fun libraryItemObserver(){
        mainViewModel.libraryItems.observe(this) {
            val tempShopList = ArrayList<ShopListItem>()
            it.forEach { item ->
                val shopItem = ShopListItem(
                    item.id,
                    item.name,
                    "",
                    false,
                    0,
                    1
                )
                tempShopList.add(shopItem)
            }
            adapter?.submitList(tempShopList)
            binding.tvEmpty.visibility = if(it.isEmpty()){
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    private fun initRcView() = with(binding){
        adapter = ShopListItemAdapter(this@ShopListActivity)
        rcView.layoutManager = LinearLayoutManager(this@ShopListActivity)
        rcView.adapter = adapter

    }

    private fun expandActionView(): OnActionExpandListener{
        return object : OnActionExpandListener{
            override fun onMenuItemActionExpand(p0: MenuItem): Boolean {
                saveItem.isVisible = true
                edItem?.addTextChangedListener(textWatcher)
                libraryItemObserver()
                mainViewModel.getAllItemsFromList(shopListNameItem?.id!!).removeObservers(this@ShopListActivity)
                mainViewModel.getAllLibraryItems("%%")
                return true
            }

            override fun onMenuItemActionCollapse(p0: MenuItem): Boolean {
                saveItem.isVisible = false
                edItem?.removeTextChangedListener(textWatcher)
                invalidateOptionsMenu()
                mainViewModel.libraryItems.removeObservers(this@ShopListActivity)
                edItem?.setText("")
                listItemObserver()
                return true
            }

        }
    }

    private fun init(){
        shopListNameItem = intent.getSerializableExtra(SHOP_LIST_NAME) as ShopListNameItem
    }

    companion object{
        const val SHOP_LIST_NAME = "shop_list_name"
    }

    override fun onClickItem(shopListItem: ShopListItem, state: Int) {
        when(state){
            ShopListItemAdapter.CHECK_BOX -> mainViewModel.updateListItem(shopListItem)
            ShopListItemAdapter.EDIT -> editListItem(shopListItem)
            ShopListItemAdapter.EDIT_LIBRARY_ITEM -> editLibraryItem(shopListItem)
            ShopListItemAdapter.DELETE_LIBRARY_ITEM -> {
                mainViewModel.deleteLibraryItem(shopListItem.id!!)
                mainViewModel.getAllLibraryItems("%%${edItem?.text.toString()}")
            }
            ShopListItemAdapter.ADD -> addNewShopItem(shopListItem.name)
        }
    }

    private fun editListItem(item: ShopListItem){
        EditListItemDialog.showDialog(this, item, object: EditListItemDialog.Listener{
            override fun onClick(item: ShopListItem) {
                mainViewModel.updateListItem(item)
            }

        })
    }

    private fun editLibraryItem(item: ShopListItem){
        EditListItemDialog.showDialog(this, item, object: EditListItemDialog.Listener{
            override fun onClick(item: ShopListItem) {
                mainViewModel.updateLibraryItem(LibraryItem(item.id, item.name))
                mainViewModel.getAllLibraryItems("%%${edItem?.text.toString()}")
            }

        })
    }

    private fun saveItemCount(){
        var checkedItemCounter: Int = 0
        adapter?.currentList?.forEach {
            if(it.itemChecked) checkedItemCounter++
        }
        val tempShopListNameItem = shopListNameItem?.copy(
            allItemCounter = adapter?.itemCount!!,
            checkedItemsCounter = checkedItemCounter,
        )
        mainViewModel.updateListName(tempShopListNameItem!!)
    }

    override fun onBackPressed() {
        saveItemCount()
        super.onBackPressed()
    }

    private fun getSelectedTheme(): Int {
        return if(defPref.getString("theme_key", "blue") == "blue"){
            R.style.Theme_NewNoteBlue
        } else {
            R.style.Theme_NewNoteRed
        }
    }
}