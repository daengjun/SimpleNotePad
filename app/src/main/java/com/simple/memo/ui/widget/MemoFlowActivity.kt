package com.simple.memo.ui.widget

import android.os.Bundle
import androidx.appcompat.widget.Toolbar
import androidx.appcompat.app.AppCompatActivity
import com.simple.memo.R
import com.simple.memo.data.model.MemoEntity

class MemoFlowActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_memo_flow)

        setSupportActionBar(findViewById(R.id.toolbar))
        supportActionBar?.setDisplayShowTitleEnabled(true)
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, MemoPickerFragment())
                .commit()
        }

        supportFragmentManager.addOnBackStackChangedListener {
            val isRoot = supportFragmentManager.backStackEntryCount == 0
            supportActionBar?.setDisplayHomeAsUpEnabled(!isRoot)
        }

        findViewById<Toolbar>(R.id.toolbar).setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    fun openWriteMemoFragment(memo: MemoEntity? = null) {
        val fragment = if (memo != null) {
            WidgetWriteMemoFragment.newInstance(memo)
        } else {
            WidgetWriteMemoFragment()
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }
}