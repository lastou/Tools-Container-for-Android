package com.diana.tools

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.Menu
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
    private val toolDirList by lazy { mainViewModel.toolDirList }
    private val idList by lazy { mainViewModel.idList }

//    private var rootDirUri: Uri? = null
//    private val toolDirList = mutableListOf<String>()
//    private val idList = mutableListOf<Int>()

//    private val pickRootDirectoryLauncher = registerForActivityResult<Uri?, Uri?>(
//        ActivityResultContracts.OpenDocumentTree()
//    ) { directoryUri: Uri? ->
//        if (directoryUri != null) {
//            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
//            contentResolver.takePersistableUriPermission(directoryUri, takeFlags)
//            setRootDirectory(directoryUri)
//        }
//    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)
//        val mainViewModel = ViewModelProvider(this).get(MainViewModel::class.java)
//        mainViewModel.rootDirUri.observe(this) {
//            setRootDirectory(it)
//        }

//        val selectRootDirButton: Button = findViewById(R.id.select_root_dir_button)
//        selectRootDirButton.setOnClickListener {
//            pickRootDirectoryLauncher.launch(null)
//        }

        getRootDirectoryUri()?.let { mainViewModel.updateRootDirUri(it) }
        rootDirUri.observe(this) { newData ->
            newData?.let {
                setRootDirectory(it)
                updateTools()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
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

//   TODO:     release old permission
        oldRootDir?.toUri().let { }
//        grant new permission
        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        contentResolver.takePersistableUriPermission(rootDirUri, takeFlags)
    }

//    private fun releasePersistablePermission(uri: Uri?) {
//        if (uri == null) return
//        val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
//        val hasTargetPermission = contentResolver.getPersistedUriPermissions().any { permission ->
//            permission.uri == uri && (permission.modeFlags and takeFlags) != 0
//        }
//        // 存在权限则释放
//        if (hasTargetPermission) {
//            contentResolver.releasePersistableUriPermission(uri, takeFlags)
//        }
//    }

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
        val navGraph = navController.graph

        val _idList = mutableListOf<Int>()
        repeat(_toolDirList.size) {
            _idList.add(View.generateViewId())
        }

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
            navGraph.addDestination(toolDestination)
        }

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home) + _idList.toSet(),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

//        update viewModel data
        mainViewModel.updateToolList(_toolDirList, _idList)
    }
}
