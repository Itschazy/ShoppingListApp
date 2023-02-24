package com.chxzyfps.shoppinglist.activities

import android.content.Intent
import android.content.SharedPreferences
import android.media.tv.AdRequest
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.preference.PreferenceManager
import com.chxzyfps.shoppinglist.R
import com.chxzyfps.shoppinglist.databinding.ActivityMainBinding
import com.chxzyfps.shoppinglist.dialogs.NewListDialog
import com.chxzyfps.shoppinglist.fragments.FragmentManager
import com.chxzyfps.shoppinglist.fragments.NoteFragment
import com.chxzyfps.shoppinglist.fragments.ShopListNamesFragment
import com.chxzyfps.shoppinglist.settings.SettingsActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class MainActivity : AppCompatActivity(), NewListDialog.Listener {
    lateinit var binding: ActivityMainBinding
    private lateinit var defPref: SharedPreferences
    private var currentMenuItemId = R.id.shop_list
    private var currentTheme = ""
    private var iAd: InterstitialAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        defPref = PreferenceManager.getDefaultSharedPreferences(this)
        setTheme(getSelectedTheme())
        currentTheme = defPref.getString("theme_key", "blue").toString()
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        FragmentManager.setFragment(ShopListNamesFragment.newInstance(), this)
        setBottomNavListener()
        loadInterAd()
    }

    private fun loadInterAd(){
        val request = com.google.android.gms.ads.AdRequest.Builder().build()
        InterstitialAd.load(this, getString(R.string.inter_ad_id), request,
            object : InterstitialAdLoadCallback(){
                override fun onAdLoaded(ad: InterstitialAd) {
                    iAd = ad
                }

                override fun onAdFailedToLoad(ad: LoadAdError) {
                    iAd = null
                }
        })
    }

    private fun showInterAd(adListener: AdListener){
        if(iAd != null){
            iAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    iAd = null
                    loadInterAd()
                    adListener.onFinish()
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    iAd = null
                    loadInterAd()
                }

                override fun onAdShowedFullScreenContent() {
                    iAd = null
                    loadInterAd()
                }
            }
            iAd?.show(this)
        } else {
            adListener.onFinish()
        }
    }

    private fun setBottomNavListener() {
        binding.bNav.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.settings -> {
                    showInterAd(object : AdListener{
                        override fun onFinish() {
                            startActivity(Intent(this@MainActivity, SettingsActivity::class.java))
                        }
                    })
                }
                R.id.notes -> {
                    currentMenuItemId = R.id.notes
                    FragmentManager.setFragment(NoteFragment.newInstance(), this)
                }
                R.id.shop_list -> {
                    currentMenuItemId = R.id.shop_list
                    FragmentManager.setFragment(ShopListNamesFragment.newInstance(), this)
                }
                R.id.new_item -> {
                    FragmentManager.currentFrag?.onClickNew()
                }
            }
            true
        }
    }

    override fun onResume() {
        super.onResume()
        binding.bNav.selectedItemId = currentMenuItemId
        if (currentTheme != defPref.getString("theme_key", "blue")) recreate()
    }

    private fun getSelectedTheme(): Int {
        return if(defPref.getString("theme_key", "blue") == "blue"){
            R.style.Theme_ShoppingListBlue
        } else {
            R.style.Theme_ShoppingListRed
        }
    }

    override fun onClick(name: String) {
        Log.d("MyLog", "Mame = $name")
    }

    interface AdListener{
        fun onFinish()
    }
}