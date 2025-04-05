package com.simple.memo.ui.main

import android.animation.ValueAnimator
import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.simple.memo.databinding.ActivityMainBinding
import com.simple.memo.ui.home.HomeFragment
import com.simple.memo.ui.settings.SettingsFragment
import com.simple.memo.ui.trash.TrashFragment
import com.simple.memo.ui.write.WriteMemoFragment
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.simple.memo.R
import com.simple.memo.viewModel.MemoViewModel


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var toggle: ActionBarDrawerToggle

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        val prefs = getSharedPreferences("settings_prefs", MODE_PRIVATE)
        val deleteCycle = prefs.getString("key_auto_delete", "never") ?: "never"

        val memoViewModel = ViewModelProvider(this)[MemoViewModel::class.java]
        memoViewModel.autoDeleteOldTrash(deleteCycle)

        binding.searchBar.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                hideKeyboard()
                binding.searchBar.clearFocus()
                true
            } else {
                false
            }
        }

        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            binding.toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)

        /*
        * 기본 Fragment 지정
        * */
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment, HomeFragment())
                .commit()
        }

        binding.navView.setNavigationItemSelectedListener { menuItem ->
            hideKeyboard()

            val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)

            val handled = when (menuItem.itemId) {
                R.id.nav_all_memos -> {
                    if (currentFragment !is HomeFragment) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.nav_host_fragment, HomeFragment())
                            .commit()
                        invalidateOptionsMenu()
                    }
                    true
                }

                R.id.nav_settings -> {
                    if (currentFragment !is SettingsFragment) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.nav_host_fragment, SettingsFragment())
                            .commit()
                        invalidateOptionsMenu()
                    }
                    true
                }

                R.id.nav_trash -> {
                    if (currentFragment !is TrashFragment) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.nav_host_fragment, TrashFragment())
                            .commit()
                        invalidateOptionsMenu()
                    }
                    true
                }

                else -> false
            }

            if (handled) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
            }

            handled
        }

        toggle.syncState()

        supportFragmentManager.addOnBackStackChangedListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            val isWriteScreen = currentFragment is WriteMemoFragment

            if (isWriteScreen) {
                toggle.isDrawerIndicatorEnabled = false
                toggle.setToolbarNavigationClickListener {
                    onBackPressedDispatcher.onBackPressed()
                }
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)

                animateToggle(0f, 1f)

            } else {
                toggle.toolbarNavigationClickListener = null
                toggle.isDrawerIndicatorEnabled = true
                binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                supportActionBar?.setDisplayHomeAsUpEnabled(false)

                animateToggle(1f, 0f)
            }

            toggle.syncState()
            invalidateOptionsMenu()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        val searchItem = menu.findItem(R.id.action_search)

        searchItem.setOnMenuItemClickListener {
            val isVisible = binding.searchBar.isVisible
            animateSearchBar(!isVisible)

            if (!isVisible) {
                binding.searchBar.requestFocus()
            } else {
                binding.searchBar.setText("")
            }
            true
        }

        val deleteItem = menu.findItem(R.id.action_delete)
        deleteItem.setOnMenuItemClickListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            if (currentFragment is WriteMemoFragment) {
                currentFragment.markAsDeleted()
                onBackPressedDispatcher.onBackPressed()
            }
            true
        }
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)

        val isHomeScreen = currentFragment is HomeFragment
        val isWriteScreen = currentFragment is WriteMemoFragment
        val isExistingMemo = if (isWriteScreen) {
            (currentFragment as WriteMemoFragment).getCurrentMemo() != null
        } else {
            false
        }

        menu.findItem(R.id.action_search)?.isVisible = isHomeScreen
        menu.findItem(R.id.action_delete)?.isVisible = isWriteScreen && isExistingMemo

        if (!isHomeScreen && binding.searchBar.isVisible) {
            binding.searchBar.visibility = View.GONE
            binding.searchBar.setText("")
        }

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)

        if (item.itemId == android.R.id.home && currentFragment is WriteMemoFragment) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    private fun animateToggle(from: Float, to: Float) {
        val drawable = toggle.drawerArrowDrawable
        val animator = ValueAnimator.ofFloat(from, to)
        animator.duration = 300
        animator.addUpdateListener {
            val value = it.animatedValue as Float
            drawable.progress = value
        }
        animator.start()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        currentFocus?.let { view ->
            imm.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    /*
    *  뒤로 가기시 알림
    * */
    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val isWriteScreen = currentFragment is WriteMemoFragment
        val isBackStackEmpty = supportFragmentManager.backStackEntryCount == 0

        if (!isWriteScreen && isBackStackEmpty) {
            showExitDialog(this)
        } else {
            super.onBackPressed()
        }
    }

    private fun showExitDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle(getString(R.string.alter))
            .setMessage(getString(R.string.exit_app))
            .setPositiveButton(getString(R.string.ok)) { _, _ ->
                finish()
            }
            .setNegativeButton(getString(R.string.cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun animateSearchBar(show: Boolean) {
        val duration = 250L
        if (show) {
            binding.searchBar.visibility = View.VISIBLE
            binding.searchBar.alpha = 0f
            binding.searchBar.translationY = -binding.searchBar.height.toFloat()

            binding.searchBar.animate()
                .alpha(1f)
                .translationY(0f)
                .setDuration(duration)
                .start()
        } else {
            hideKeyboard()

            binding.searchBar.animate()
                .alpha(0f)
                .translationY(-binding.searchBar.height.toFloat())
                .setDuration(duration)
                .withEndAction {
                    binding.searchBar.visibility = View.GONE
                }
                .start()


        }
    }
}