package com.simple.memo.ui.main

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.PopupWindow
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.simple.memo.databinding.ActivityMainBinding
import com.simple.memo.ui.home.HomeFragment
import com.simple.memo.ui.settings.SettingsFragment
import com.simple.memo.ui.trash.TrashFragment
import com.simple.memo.ui.write.WriteMemoFragment
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.simple.memo.R
import com.simple.memo.data.local.MemoDatabase
import com.simple.memo.viewModel.MemoViewModel
import kotlinx.coroutines.launch
import androidx.core.content.edit


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var selectedMenu: View

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

        if (savedInstanceState == null) {
            when {
                intent.getBooleanExtra("from_widget_write", false) -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, WriteMemoFragment())
                        .commit()
                }

                else -> {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.nav_host_fragment, HomeFragment())
                        .commit()
                }
            }
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

        if (intent?.getBooleanExtra("from_widget", false) == true) {
            val memoId = intent.getIntExtra("memo_id", -1)
            if (memoId != -1) {
                lifecycleScope.launch {
                    val db = MemoDatabase.getDatabase(applicationContext)
                    val memo = db.memoDao().getMemoById(memoId)
                    if (memo != null) {
                        supportFragmentManager.beginTransaction()
                            .replace(R.id.nav_host_fragment, WriteMemoFragment.newInstance(memo))
                            .addToBackStack(null)
                            .commit()

                        toggle.isDrawerIndicatorEnabled = false
                        toggle.setToolbarNavigationClickListener {
                            onBackPressedDispatcher.onBackPressed()
                        }
                        binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                        supportActionBar?.setDisplayHomeAsUpEnabled(true)
                        toggle.syncState()
                    }
                }
            }
        }
        setupCustomDrawerMenu()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)

        val searchItem = menu.findItem(R.id.action_search)

        /*
        * 검색 아이콘 클릭
        * */
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

        val moreItem = menu.findItem(R.id.action_more)
        moreItem.setOnMenuItemClickListener {
            showPopupMenu(findViewById(R.id.action_more))
            true
        }

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)

        val isHomeScreen = currentFragment is HomeFragment

        val isHomeAndTrashScreen =
            currentFragment is HomeFragment || currentFragment is TrashFragment
        val isWriteScreen = currentFragment is WriteMemoFragment
        val isExistingMemo = if (isWriteScreen) {
            (currentFragment as WriteMemoFragment).getCurrentMemo() != null
        } else {
            false
        }

        menu.findItem(R.id.action_more)?.isVisible = isHomeAndTrashScreen
        menu.findItem(R.id.action_search)?.isVisible = isHomeScreen
        menu.findItem(R.id.action_delete)?.isVisible = isWriteScreen && isExistingMemo

        if (!isHomeScreen && binding.searchBar.isVisible) {
            binding.searchBar.visibility = View.GONE
            binding.searchBar.isFocusableInTouchMode = false
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

    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val isWriteScreen = currentFragment is WriteMemoFragment
        val isBackStackEmpty = supportFragmentManager.backStackEntryCount == 0

        if (currentFragment is HomeFragment && currentFragment.isMultiSelectMode()) {
            currentFragment.exitMultiSelectMode()
            return
        } else if (currentFragment is TrashFragment && currentFragment.isMultiSelectMode()) {
            currentFragment.exitMultiSelectMode()
            return
        }

        if (!isWriteScreen && isBackStackEmpty) {
            val isVisible = binding.searchBar.isVisible

            if (isVisible) {
                animateSearchBar(false)
                binding.searchBar.setText("")
            } else {
                showExitDialog()
            }
        } else {
            super.onBackPressed()
        }
    }

    private fun showExitDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_exit, null)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btn_cancel)
        val btnConfirm = dialogView.findViewById<TextView>(R.id.btn_confirm)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                dialog.dismiss()
                true
            } else {
                false
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
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
                .withEndAction {
                    binding.toolbar.requestFocus()
                    binding.searchBar.clearFocus()
                    binding.searchBar.isFocusableInTouchMode = true
                }
                .start()
        } else {
            hideKeyboard()
            binding.searchBar.animate()
                .alpha(0f)
                .translationY(-binding.searchBar.height.toFloat())
                .setDuration(duration)
                .withEndAction {
                    binding.searchBar.visibility = View.GONE
                    binding.searchBar.isFocusableInTouchMode = false
                }
                .start()
        }
    }

    private fun setupCustomDrawerMenu() {
        val menuHome = findViewById<View>(R.id.menu_home)
        val menuSettings = findViewById<View>(R.id.menu_settings)
        val menuTrash = findViewById<View>(R.id.menu_trash)
        val menuAddFolder = findViewById<View>(R.id.menu_add_folder)

        loadFoldersFromPrefs().forEach { folderName ->
            addFolderMenu(folderName)
        }

        menuHome.setOnClickListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)

            val isSameFolder = if (currentFragment is HomeFragment) {
                val currentFolder = currentFragment.arguments?.getString("folderName") ?: "기본"
                currentFolder == "기본"
            } else {
                false
            }

            if (!isSameFolder) {

                animateSearchBar(false)
                binding.searchBar.setText("")

                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, HomeFragment())
                    .commit()
                invalidateOptionsMenu()
            }

            updateSelectedMenu(menuHome)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        menuSettings.setOnClickListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            Log.e("TAG", "setupCustomDrawerMenu: ${currentFragment}")

            if (currentFragment !is SettingsFragment) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, SettingsFragment())
                    .commit()
                invalidateOptionsMenu()
                hideKeyboard()
            }
            updateSelectedMenu(menuSettings)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        menuTrash.setOnClickListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            Log.e("TAG", "setupCustomDrawerMenu: ${currentFragment}")

            if (currentFragment !is TrashFragment) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, TrashFragment())
                    .commit()
                invalidateOptionsMenu()
                hideKeyboard()
            }
            updateSelectedMenu(menuTrash)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        menuAddFolder.setOnClickListener {
            hideKeyboard()
            showFolderInputDialog()
        }

        // 앱 시작 시 기본 선택 메뉴 설정
        selectedMenu = menuHome
        selectedMenu.isSelected = true
    }

    private fun updateSelectedMenu(newSelected: View) {
        selectedMenu.isSelected = false
        newSelected.isSelected = true
        selectedMenu = newSelected
    }

    private fun showFolderInputDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_folder, null)
        val etFolderName = dialogView.findViewById<EditText>(R.id.et_folder_name)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btn_cancel)
        val btnConfirm = dialogView.findViewById<TextView>(R.id.btn_confirm)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) {
                dialog.dismiss()
                true
            } else {
                false
            }
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            val folderName = etFolderName.text.toString().trim()
            val prefs = getSharedPreferences("folder_prefs", MODE_PRIVATE)
            val folderSet = prefs.getStringSet("folder_list", emptySet()) ?: emptySet()

            when {
                folderName.isEmpty() -> {
                    etFolderName.error = getString(R.string.input_folder_name)
                }

                folderSet.contains(folderName) -> {
                    etFolderName.error = getString(R.string.error_folder_name_exists)
                }

                else -> {
                    addFolderMenu(folderName)
                    saveFolderToPrefs(folderName)
                    dialog.dismiss()
                }
            }
        }
        dialog.show()

        dialogView.isFocusableInTouchMode = true
        dialogView.isFocusable = true
        dialogView.isClickable = true

        dialog.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun addFolderMenu(name: String) {
        val folderContainer = findViewById<LinearLayout>(R.id.folder_container)

        val folderView = layoutInflater.inflate(R.layout.item_drawer_folder, folderContainer, false)

        val icon = folderView.findViewById<ImageView>(R.id.img_icon)
        val text = folderView.findViewById<TextView>(R.id.tv_folder_name)

        icon.setImageResource(R.drawable.ic_folder)
        text.text = name

        folderView.setOnClickListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)

            val isSameFolder = if (currentFragment is HomeFragment) {
                val currentFolder = currentFragment.arguments?.getString("folderName") ?: "기본"
                currentFolder == name
            } else {
                false
            }

            if (!isSameFolder) {

                animateSearchBar(false)
                binding.searchBar.setText("")

                val fragment = HomeFragment().apply {
                    arguments = bundleOf("folderName" to name)
                }

                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment)
                    .commit()

                invalidateOptionsMenu()
            }
            updateSelectedMenu(folderView)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        folderContainer.addView(folderView)
    }

    fun saveFolderToPrefs(folderName: String) {
        val prefs = getSharedPreferences("folder_prefs", Context.MODE_PRIVATE)
        val folderSet =
            prefs.getStringSet("folder_list", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        folderSet.add(folderName)
        prefs.edit { putStringSet("folder_list", folderSet) }
    }

    private fun loadFoldersFromPrefs(): Set<String> {
        val prefs = getSharedPreferences("folder_prefs", Context.MODE_PRIVATE)
        return prefs.getStringSet("folder_list", emptySet()) ?: emptySet()
    }

    /*
    * 서랍 메뉴 새로 고침
    * */
    fun refreshFolderMenus() {
        val folderContainer = findViewById<LinearLayout>(R.id.folder_container)
        folderContainer.removeAllViews()

        loadFoldersFromPrefs().forEach { folderName ->
            addFolderMenu(folderName)
        }
    }


    private fun showPopupMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        val customView = layoutInflater.inflate(R.layout.popup_menu_item, null)
        val popupWindow = PopupWindow(
            customView,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        )
        popupWindow.elevation = 8f
        popupWindow.showAsDropDown(anchor)

        val memoSelectView = customView.findViewById<TextView>(R.id.select_memo)
        memoSelectView.setOnClickListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            if (currentFragment is HomeFragment) {
                currentFragment.startMultiSelectMode()
            } else if (currentFragment is TrashFragment) {
                currentFragment.startMultiSelectMode()
            }
            popupWindow.dismiss()
        }
        popup.show()
    }


    /*
    * EditText focus 해제 , 키보드 내리기
    * */
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.action == MotionEvent.ACTION_DOWN) {
            val v = currentFocus
            if (v is EditText) {
                val outRect = Rect()
                v.getGlobalVisibleRect(outRect)
                if (!outRect.contains(ev.rawX.toInt(), ev.rawY.toInt())) {
                    v.clearFocus()
                    hideKeyboard(v)
                }
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun hideKeyboard(view: View) {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

}
