package com.simple.memo.ui.main

import android.animation.ValueAnimator
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Rect
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
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
import androidx.recyclerview.widget.LinearLayoutManager
import com.simple.memo.ui.settings.ManageFoldersFragment
import com.simple.memo.util.CustomToastMessage
import kotlin.math.min


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var toggle: ActionBarDrawerToggle
    private var selectedMenu: View? = null
    private var isFolderExpanded = true
    private lateinit var folderAdapter: FolderAdapter

    private val prefs by lazy {
        this.getSharedPreferences("drawer_prefs", Context.MODE_PRIVATE)
    }

    private val preferenceKeyFolderExpanded = "folder_expanded"

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

        binding.drawerLayout.setScrimColor(Color.TRANSPARENT)
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

        //==================== 폴더 리스트 어댑터 초기화 ====================================================//
        folderAdapter = FolderAdapter { folderItem, itemView ->
            val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
            val isSameFolder = if (currentFragment is HomeFragment) {
                val currentFolder = currentFragment.arguments?.getString("folderName") ?: "기본"
                currentFolder == folderItem.name
            } else false

            if (!isSameFolder) {
                animateSearchBar(false)
                binding.searchBar.setText("")
                val fragment = HomeFragment().apply {
                    arguments = bundleOf("folderName" to folderItem.name)
                }
                supportFragmentManager.beginTransaction()
                    .replace(R.id.nav_host_fragment, fragment)
                    .commit()
                invalidateOptionsMenu()
                updateSelectedMenu(itemView)
            }

            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        binding.customMenuList.folderRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.customMenuList.folderRecyclerView.adapter = folderAdapter
        updateFolderList()
        //=======================================================================================//
        // checkForAppUpdate()
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

            val isDrawerOpen = binding.drawerLayout.isDrawerOpen(GravityCompat.START)
            Log.e("TAG", "isDrawerOpen : $isDrawerOpen")

            // 서랍 메뉴 닫기
            if (isDrawerOpen) {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                return
            }

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
            .setCancelable(true)
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

        /*
        * 태블릿 최대 크기 지정 600dp
        * */
        val dialogWidth = resources.displayMetrics.widthPixels
        val maxDialogWidth = resources.getDimensionPixelSize(R.dimen.dialog_max_width)

        dialog.window?.setLayout(
            min(dialogWidth, maxDialogWidth),
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
        val myFolders = findViewById<LinearLayout>(R.id.menu_my_folders)
        val toggleIcon = findViewById<ImageView>(R.id.folder_toggle_icon)

        myFolders.setOnClickListener {
            // 축 기준을 위로 설정
            binding.customMenuList.folderRecyclerView.pivotY = 0f

            if (isFolderExpanded) {
                // 접기
                binding.customMenuList.folderRecyclerView.animate()
                    .scaleY(0f)
                    .alpha(0f)
                    .setDuration(200)
                    .withEndAction {
                        binding.customMenuList.folderRecyclerView.visibility = View.GONE
                    }
                    .start()
                rotateArrow(toggleIcon, true)
            } else {
                // 펼치기
                binding.customMenuList.folderRecyclerView.visibility = View.VISIBLE
                binding.customMenuList.folderRecyclerView.scaleY = 0f
                binding.customMenuList.folderRecyclerView.alpha = 0f
                binding.customMenuList.folderRecyclerView.pivotY = 0f // 중요
                binding.customMenuList.folderRecyclerView.animate()
                    .scaleY(1f)
                    .alpha(1f)
                    .setDuration(200)
                    .start()
                rotateArrow(toggleIcon, false)
            }

            isFolderExpanded = !isFolderExpanded
            saveFolderExpandedState(isFolderExpanded)
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
                binding.tvToolbarTitle.alpha = 0f
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
                binding.tvToolbarTitle.alpha = 0f
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
                binding.tvToolbarTitle.alpha = 0f
            }
            updateSelectedMenu(menuTrash)
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }

        menuAddFolder.setOnClickListener {
            hideKeyboard()
            showFolderInputDialog()
        }

        restoreFolderExpandedState()

        // 앱 시작 시 기본 선택 메뉴 설정
        selectedMenu = menuHome
        selectedMenu?.isSelected = true
    }

    private fun updateSelectedMenu(newSelected: View) {
        selectedMenu?.isSelected = false
        // 어댑터 아이템 뷰 아닌지 확인
        if (newSelected.tag is FolderItem) {
            folderAdapter.selectFolderByName((newSelected.tag as FolderItem).name)
            selectedMenu = null
        } else {
            // 어댑터 외 메뉴면 선택 표시 + 어댑터 선택 해제
            newSelected.isSelected = true
            selectedMenu = newSelected
            folderAdapter.clearSelection()
        }
    }


    private fun showFolderInputDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_folder, null)
        val etFolderName = dialogView.findViewById<EditText>(R.id.et_folder_name)
        val btnCancel = dialogView.findViewById<TextView>(R.id.btn_cancel)
        val btnConfirm = dialogView.findViewById<TextView>(R.id.btn_confirm)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
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

            val existingNames = folderSet.mapNotNull {
                it.split("|||").getOrNull(0)
            }.toSet()

            when {
                folderName.isEmpty() -> {
                    hideKeyboard(etFolderName)
                    CustomToastMessage.createToast(this, getString(R.string.input_folder_name))
                }

                existingNames.contains(folderName) -> {
                    hideKeyboard(etFolderName)
                    CustomToastMessage.createToast(
                        this,
                        getString(R.string.error_folder_name_exists)
                    )
                }

                else -> {
                    saveFolderToPrefs(folderName)
                    updateFolderList()
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

        /*
        * 태블릿 최대 크기 지정 600dp
        * */
        val dialogWidth = resources.displayMetrics.widthPixels
        val maxDialogWidth = resources.getDimensionPixelSize(R.dimen.dialog_max_width)

        dialog.window?.setLayout(
            min(dialogWidth, maxDialogWidth),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    /**
     * 신규 폴더명 저장 (폴더명|||TimeStamp)
     * @param folderName - 폴더명
     * */
    private fun saveFolderToPrefs(folderName: String) {
        val prefs = getSharedPreferences("folder_prefs", Context.MODE_PRIVATE)
        val folderSet =
            prefs.getStringSet("folder_list", mutableSetOf())?.toMutableSet() ?: mutableSetOf()

        // 이미 같은 이름 있는지 확인 (기존 항목 제거)
        folderSet.removeIf { it.startsWith("$folderName|||") }

        // 이름 + 타임스탬프
        val item = "$folderName|||${System.currentTimeMillis()}"
        folderSet.add(item)

        prefs.edit { putStringSet("folder_list", folderSet) }

        val currentFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        if (currentFragment is ManageFoldersFragment) {
            currentFragment.refreshFolderList()
        }
    }

    /**
     * 저장된 폴더 목록 조회
     * @return - 폴더 목록
     * */
    private fun loadFoldersFromPrefs(): List<String> {
        val prefs = getSharedPreferences("folder_prefs", Context.MODE_PRIVATE)
        val folderSet = prefs.getStringSet("folder_list", emptySet()) ?: emptySet()

        return folderSet
            .mapNotNull {
                val parts = it.split("|||")
                if (parts.size == 2) parts[0] to parts[1].toLongOrNull() else null
            }
            .sortedByDescending { it.second } // 최신순 정렬
            .map { it.first } // 이름만 추출
    }

    /**
     * 폴더 목록 표시하는 리사이클러뷰 데이터 갱신
     * */
    fun updateFolderList() {
        val folderSet = loadFoldersFromPrefs()
        val folderList = folderSet.map { FolderItem(it) }
        folderAdapter.submitList(folderList) {
            // submitList 완료된 후 콜백
            folderAdapter.selectedFolderName?.let {
                folderAdapter.selectFolderByName(it)
            }
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

        val writeReview = customView.findViewById<TextView>(R.id.write_review)
        writeReview.setOnClickListener {
            openPlayStoreReview(this)
            popupWindow.dismiss()
        }

        popup.show()
    }

    /**
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

    private fun rotateArrow(view: View, expanded: Boolean) {
        val to = if (expanded) -90f else 0f
        view.animate()
            .rotation(to)
            .setDuration(260)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun restoreFolderExpandedState() {
        isFolderExpanded = prefs.getBoolean(preferenceKeyFolderExpanded, true)
        Log.e("TAG", "isFolderExpanded: $isFolderExpanded")

        val toggleIcon = findViewById<ImageView>(R.id.folder_toggle_icon)

        if (isFolderExpanded) {
            binding.customMenuList.folderRecyclerView.visibility = View.VISIBLE
            toggleIcon.rotation = 0f
        } else {
            binding.customMenuList.folderRecyclerView.visibility = View.GONE
            toggleIcon.rotation = -90f
        }
    }

    private fun saveFolderExpandedState(expanded: Boolean) {
        prefs.edit { putBoolean(preferenceKeyFolderExpanded, expanded) }
    }

    /**
     * 플레이스토어 리뷰 페이지로 이동
     * */
    private fun openPlayStoreReview(context: Context) {
        val packageName = context.packageName
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$packageName"))
            intent.setPackage("com.android.vending")
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            )
            context.startActivity(intent)
        }
    }


    /*
    * 인앱 테스트 구현
    * 내부앱 공유로 테스트
    * */
//    private val updateLauncher = registerForActivityResult(
//        ActivityResultContracts.StartIntentSenderForResult()
//    ) { result ->
//        if (result.resultCode != Activity.RESULT_OK) {
//            CustomToastMessage.createToast(this, "앱 업데이트 실패")
//            Log.e("InAppUpdate", "Update flow failed! Result code: ${result.resultCode}")
//        }
//    }
//
//    /*
//    * 앱 실행시 버전 확인
//    * */
//    private fun checkForAppUpdate() {
//        val appUpdateManager = AppUpdateManagerFactory.create(this)
//        appUpdateManager.appUpdateInfo.addOnSuccessListener { appUpdateInfo ->
//            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
//                appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
//            ) {
//                val appUpdateOptions = AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
//                // 인앱 업데이트 진행
//                appUpdateManager.startUpdateFlowForResult(
//                    appUpdateInfo,
//                    updateLauncher,
//                    appUpdateOptions
//                )
//            }
//        }.addOnFailureListener { error ->
//            Log.e("TAG", "$error")
//        }
//    }
}

