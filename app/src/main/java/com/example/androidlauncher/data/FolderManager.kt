package com.example.androidlauncher.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.folderDataStore by preferencesDataStore(name = "folders")

class FolderManager(private val context: Context) {
    companion object {
        private val FOLDERS_KEY = stringPreferencesKey("app_folders")
    }

    val folders: Flow<List<FolderInfo>> = context.folderDataStore.data
        .map { preferences ->
            val jsonString = preferences[FOLDERS_KEY] ?: "[]"
            parseFolders(jsonString)
        }

    suspend fun saveFolders(folders: List<FolderInfo>) {
        context.folderDataStore.edit { preferences ->
            preferences[FOLDERS_KEY] = serializeFolders(folders)
        }
    }

    private fun parseFolders(jsonString: String): List<FolderInfo> {
        val list = mutableListOf<FolderInfo>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val appsArray = obj.getJSONArray("apps")
                val apps = mutableListOf<String>()
                for (j in 0 until appsArray.length()) {
                    apps.add(appsArray.getString(j))
                }
                list.add(FolderInfo(
                    id = obj.getString("id"),
                    name = obj.getString("name"),
                    appPackageNames = apps
                ))
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    private fun serializeFolders(folders: List<FolderInfo>): String {
        val jsonArray = JSONArray()
        for (folder in folders) {
            val obj = JSONObject()
            obj.put("id", folder.id)
            obj.put("name", folder.name)
            val appsArray = JSONArray()
            folder.appPackageNames.forEach { appsArray.put(it) }
            obj.put("apps", appsArray)
            jsonArray.put(obj)
        }
        return jsonArray.toString()
    }
}
