package com.diana.tools

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavArgument
import androidx.navigation.NavType
import androidx.navigation.findNavController
import androidx.navigation.fragment.FragmentNavigator
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.diana.tools.databinding.ActivityMainBinding
import com.diana.tools.ui.WebpageViewerFragment
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var appBarConfiguration: AppBarConfiguration

    private val mainViewModel: MainViewModel by viewModels()

    private val rootDirUri by lazy { mainViewModel.rootDirUri }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)

        getRootDirectoryUri()?.let { mainViewModel.updateRootDirUri(it) }
        rootDirUri.observe(this) { newData ->
            newData?.let {
                setRootDirectory(it)
                updateTools()
            }
        }

//        initialize menu and navigation
        val navView: NavigationView = binding.navView
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                mainViewModel.refreshWebView()
                true // consume the event
            }

            else -> {
                super.onOptionsItemSelected(item)
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun getRootDirectoryUri(): Uri? {
        val rootDir = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("root_dir", null)
        return rootDir?.toUri()
    }

    private fun setRootDirectory(rootDirUri: Uri) {
        val appPrefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val oldRootDir = appPrefs.getString("root_dir", null)

        if (rootDirUri.toString() == oldRootDir) {
            return
        }

        appPrefs.edit {
            putString("root_dir", rootDirUri.toString())
        }

//        release old permission
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        if (oldRootDir != null) {
            val oldRootDirUri = oldRootDir.toUri()
            val hasTargetPermission = contentResolver.persistedUriPermissions
                .any { permission ->
                    permission.uri == oldRootDirUri && permission.isReadPermission
                }
            if (hasTargetPermission) {
                contentResolver.releasePersistableUriPermission(oldRootDirUri, takeFlags)
            }
        }
//        grant new permission
        contentResolver.takePersistableUriPermission(rootDirUri, takeFlags)
    }

    private fun updateTools() {
//        scan tools
        val _toolDirList = mutableListOf<String>()
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            rootDirUri.value,
            DocumentsContract.getTreeDocumentId(rootDirUri.value)
        )
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE,
        )
        contentResolver.query(
            childrenUri,
            projection,
            null,
            null,
            null
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val name = cursor.getString(
                    cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                )
                val mimeType = cursor.getString(
                    cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                )

                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    _toolDirList.add(name)
                }
            }
        }

//        update menu and navigation
        val navView: NavigationView = binding.navView
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navController = findNavController(R.id.nav_host_fragment_content_main)

        val _idList = mutableListOf<Int>()
        repeat(_toolDirList.size) {
            _idList.add(View.generateViewId())
        }
//        reset menu and navGraph
        navView.menu.clear()
        navView.inflateMenu(R.menu.activity_main_drawer)
        val baseNavGraph = navController.navInflater.inflate(R.navigation.mobile_navigation)
//        add new items
        _toolDirList.forEachIndexed { index, dirName ->
            navView.menu.add(
                R.id.menu_tool,
                _idList[index],
                Menu.NONE,
                dirName
            ).apply {
                isCheckable = true
            }

            val toolDestination = FragmentNavigator.Destination(
                navController.navigatorProvider
            ).apply {
                setClassName(WebpageViewerFragment::class.java.name)
                id = _idList[index]
                label = dirName

                addArgument(
                    "root_uri", NavArgument.Builder()
                        .setDefaultValue(rootDirUri.value.toString())
                        .setType(NavType.StringType)
                        .build()
                )
                addArgument(
                    "dir_name", NavArgument.Builder()
                        .setDefaultValue(dirName)
                        .setType(NavType.StringType)
                        .build()
                )
            }
            baseNavGraph.addDestination(toolDestination)
        }
        navController.graph = baseNavGraph

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home) + _idList.toSet(),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

//        update viewModel data
        mainViewModel.updateToolList(_toolDirList, _idList)
    }

    fun navigateToId(destId: Int) {
//        simulate click
        binding.navView.menu.performIdentifierAction(destId, 0)
    }
}
