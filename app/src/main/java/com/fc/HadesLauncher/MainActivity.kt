package com.fc.HadesLauncher

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment.getExternalStorageDirectory
import android.provider.DocumentsContract
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import com.fc.HadesLauncher.fsaf.FileManager
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import java.io.File
import java.io.FileNotFoundException
import android.app.ActivityManager
import android.content.Context
import android.text.TextUtils


class MainActivity : AppCompatActivity() {

    private var llAccounts: LinearLayout? = null
    private val accountsPath = getExternalStorageDirectory().absolutePath + "/HadesLauncher"
    private var uriTree: Uri? = null
    private var packageUri: Uri? = null

    private var READ_REQUEST_CODE = 10001
    private var WRITE_REQUEST_CODE = 10002
    private var REQUEST_CODE_FOR_DIR = 10003

    private val GAME_PACKAGE = "com.ParallelSpace.Cerberus"
    private val GAME_ACCOUNT_FILE_NAME = "login.info"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        llAccounts = findViewById(R.id.ll_accounts)
        readUriTree()
        requestPermission()
    }


    private fun requestPermission() {
        XXPermissions.with(this)
            .permission(Permission.READ_EXTERNAL_STORAGE)
            .permission(Permission.WRITE_EXTERNAL_STORAGE)
            .request { _, _ ->
                val f = File(accountsPath)
                if (!f.exists()) {
                    f.mkdir()
                }
                readAccountFiles(f)

                val f1 = File("$accountsPath/accounts_file")
                if (f1.exists()) {
                    readAccountFiles(f1)
                }
            }
        if (uriTree == null) {
            startSafForDirPermission()
        }
    }

    private fun startSafForDirPermission() {
        val uri =
            Uri.parse("content://com.android.externalstorage.documents/document/primary%3AAndroid%2Fdata")
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(
            Intent.FLAG_GRANT_READ_URI_PERMISSION
                    or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
                    or Intent.FLAG_GRANT_PREFIX_URI_PERMISSION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, uri)
        }
        startActivityForResult(intent, REQUEST_CODE_FOR_DIR)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        var uri: Uri?
        if (data == null) {
            return
        }
        uriTree = data.data
        saveUriTree()
        if (requestCode == REQUEST_CODE_FOR_DIR && data.data.also { uri = it } != null) {
            contentResolver.takePersistableUriPermission(
                uriTree!!, Intent.FLAG_GRANT_READ_URI_PERMISSION
                        or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
        }
    }

    private fun deleteAccountFile() {
        try {
            DocumentsContract.deleteDocument(
                contentResolver,
                Uri.parse(packageUri.toString() + "%2Ffiles%2F" + GAME_ACCOUNT_FILE_NAME)
            )
        } catch (e: FileNotFoundException) {
            Toast.makeText(this, "删除文件失败！", Toast.LENGTH_SHORT).show()
        }
    }

    private fun readAccountFiles(dir: File) {
        dir.listFiles()?.forEach { file ->
            if (!file.isDirectory) {
                val item =
                    LayoutInflater.from(this).inflate(R.layout.item_account, llAccounts, false)
                val tvAccount = item.findViewById<TextView>(R.id.tv_account)
                tvAccount.text = file.name
                item.setOnClickListener {
                    uriTree?.apply {
                        DocumentFile.fromTreeUri(this@MainActivity, this)?.let { root ->
                            root.listFiles().forEach { documentFile ->
                                if(documentFile.uri.path?.endsWith(GAME_PACKAGE) == true){
                                    packageUri = documentFile.uri
                                }
                            }

                            packageUri?.apply {
                                val fm = FileManager(this@MainActivity)
                                val sourceFile = fm.fromRawFile(file)

                                val toFile =
                                    fm.fromUri(Uri.parse("$this%2Ffiles%2F$GAME_ACCOUNT_FILE_NAME"))
                                if (toFile != null) {
                                    fm.copyFileContents(sourceFile, toFile)
                                    killGame()
                                    startGame()
                                }
                            }
                        }
                    }
                }
                llAccounts?.addView(item)
            }
        }
    }

    private fun killGame() {
        val am = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        am.killBackgroundProcesses(GAME_PACKAGE)
    }

    private fun startGame() {
        val intent = packageManager.getLaunchIntentForPackage(GAME_PACKAGE)
        startActivity(intent)
    }

    private val SP_NAME = "hadeslauncher"
    private val SP_URI_TREE = "uritree"
    private fun saveUriTree() {
        uriTree?.let {
            val sp = getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
            sp.edit().putString(SP_URI_TREE, it.toString()).apply()
        }
    }

    private fun readUriTree() {
        val sp = getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
        val s = sp.getString(SP_URI_TREE, "")
        if (!TextUtils.isEmpty(s)) {
            uriTree = Uri.parse(s)
        }
    }
}