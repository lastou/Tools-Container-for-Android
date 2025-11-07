package com.diana.tools

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.Menu
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
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
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private var rootDirUri: Uri? = null
    private val toolDirList = mutableListOf<String>()

    private val pickRootDirectoryLauncher = registerForActivityResult<Uri?, Uri?>(
        ActivityResultContracts.OpenDocumentTree() // 系统提供的“选择目录”契约
    ) { directoryUri: Uri? ->
        // 3. 处理返回结果（用户选择目录后回调）
        if (directoryUri != null) {
            // 申请持久化权限（避免重启后失效）
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            contentResolver.takePersistableUriPermission(directoryUri, takeFlags)

            // 保存目录 Uri 或直接使用
            saveRootDirectory(directoryUri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        rootDirUri = getRootDirectoryUri()
        if (rootDirUri != null) {
            // 已选择，直接读取目录下的文件
//            readFilesInDirectory(savedUri)
        } else {
            // 未选择，提示用户选择目录（例如：显示按钮让用户触发 pickDirectory()）
            pickRootDirectoryLauncher.launch(null)
        }



        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

        binding.appBarMain.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null)
                .setAnchorView(R.id.fab).show()
        }

        scanDirs()
        updateTools()
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

    private fun saveRootDirectory(uri: Uri) {
        // 实现保存逻辑（如存入 SharedPreferences）
        getSharedPreferences("app_prefs", MODE_PRIVATE)
            .edit {
                putString("root_dir", uri.toString())
            }
    }

    private fun getRootDirectoryUri(): Uri? {
        val rootDir = getSharedPreferences("app_prefs", MODE_PRIVATE).getString("root_dir", null)
        return rootDir?.toUri()
    }

    // 读取目录下的所有文件（仅读取，不修改）
    private fun scanDirs() {
        toolDirList.clear()

        // 1. 构建查询参数：列出目录下的所有文件
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
            rootDirUri,
            DocumentsContract.getTreeDocumentId(rootDirUri)
        )

        // 3. 定义需要查询的字段（至少包含类型和名称）
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,  // 目录/文件名
            DocumentsContract.Document.COLUMN_MIME_TYPE,     // 类型（目录为 MIME_TYPE_DIR）
        )

        // 4. 执行查询
        contentResolver.query(
            childrenUri,
            projection,
            null,  // 筛选条件（不筛选，返回所有子项）
            null,
            null   // 排序方式（默认按名称排序）
        )?.use { cursor ->
            // 5. 遍历结果，筛选出目录（MIME_TYPE 为目录类型）
            while (cursor.moveToNext()) {
                val name = cursor.getString(
                    cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                )
                val mimeType = cursor.getString(
                    cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_MIME_TYPE)
                )

                // 判断是否为目录（MIME_TYPE_DIR 表示目录）
                if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                    toolDirList.add(name)
                }
            }
        }
    }

    private fun updateTools() {
        val navView: NavigationView = binding.navView
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        val navGraph = navController.graph

        val menuIdList = mutableListOf<Int>()
        repeat(toolDirList.size) {
            menuIdList.add(View.generateViewId())
        }

        toolDirList.forEachIndexed { index, dirName ->
            navView.menu.add(
                R.id.menu_tool,
                menuIdList[index],
                Menu.NONE,
                dirName
            ).apply {
                isCheckable = true
            }

            val toolDestination = FragmentNavigator.Destination(
                navController.navigatorProvider
            ).apply {
                setClassName(WebpageViewerFragment::class.java.name)
                id = menuIdList[index]
                label = dirName

                val webUrl = DocumentsContract.buildDocumentUriUsingTree(
                    rootDirUri,
                    DocumentsContract.getTreeDocumentId(rootDirUri) + "/$dirName/index.html"
                ).toString()

//                addArgument(
//                    "web_url", NavArgument.Builder()
//                        .setDefaultValue(webUrl)
//                        .setType(NavType.StringType)
//                        .build()
//                )

                addArgument(
                    "root_uri", NavArgument.Builder()
                        .setDefaultValue(rootDirUri.toString())
                        .setType(NavType.StringType)
                        .build()
                )
                addArgument(
                    "dirname", NavArgument.Builder()
                        .setDefaultValue(dirName)
                        .setType(NavType.StringType)
                        .build()
                )
            }
            navGraph.addDestination(toolDestination)
        }


        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_gallery, R.id.nav_slideshow) + menuIdList.toSet(),
            drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
    }
}
