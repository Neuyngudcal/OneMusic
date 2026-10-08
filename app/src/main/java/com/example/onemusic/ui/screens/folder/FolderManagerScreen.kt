package com.example.onemusic.ui.screens.folder

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import com.example.onemusic.data.repository.MusicRepository
import com.example.onemusic.theme.LocalApexHazeState
import com.example.onemusic.theme.ObsidianBlack
import com.example.onemusic.ui.components.ApexConfirmDialog
import dev.chrisbanes.haze.HazeState

@Composable
fun FolderManagerScreen(
    musicRepository: MusicRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val folders by musicRepository.folders.collectAsState()
    val isScanning by musicRepository.isScanning.collectAsState()
    val allTracks by musicRepository.tracks.collectAsState(initial = emptyList())
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val hazeState = LocalApexHazeState.current ?: remember { HazeState() }
    // (uriString, displayName) của thư mục đang chờ xác nhận xóa
    var folderPendingRemove by remember { mutableStateOf<Pair<String, String>?>(null) }

    BackHandler {
        onBack()
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}

            val docFile = DocumentFile.fromTreeUri(context, uri)
            val displayName = docFile?.name ?: uri.lastPathSegment ?: "Thư mục nhạc"
            // Chỉ báo quét (isScanning) của màn này đã báo tiến trình, không cần Toast
            musicRepository.addAndScanFolder(context, uri, displayName)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .background(ObsidianBlack)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp + navBottom)
        ) {
            // 1. Top Bar with Back Button & Title
            item(key = "folder_top_bar") {
                FolderManagerTopBar(onBack = onBack)
            }

            // 2. Scanning Progress Banner
            item(key = "scanning_banner") {
                FolderScanningBanner(isScanning = isScanning)
            }

            // 3. Grouped Card: Danh sách thư mục đã chọn
            item(key = "folders_card") {
                FolderListCard(
                    folders = folders,
                    // Chỉ mở hộp xác nhận, chưa xóa ngay
                    onRemoveClick = { uriString, displayName -> folderPendingRemove = uriString to displayName }
                )
            }

            // 4. Grouped Card: Thông tin thư viện & Định dạng
            item(key = "memory_card") {
                FolderInfoCard(totalTrackCount = allTracks.size)
            }

            // 5. Action Buttons (Pills)
            item(key = "action_pills") {
                FolderActionButtons(
                    hasFolders = folders.isNotEmpty(),
                    onAddFolderClick = { folderPickerLauncher.launch(null) },
                    onRescanClick = {
                        if (!isScanning) {
                            musicRepository.rescanAllMusic(context)
                        }
                    }
                )
            }
        }

        // Xác nhận trước khi bỏ thư mục quét (bài hát sẽ biến mất khỏi thư viện)
        folderPendingRemove?.let { (uriString, displayName) ->
            ApexConfirmDialog(
                title = "Xóa thư mục \"$displayName\"?",
                message = "Các bài hát trong thư mục này sẽ biến mất khỏi thư viện OneMusic (file gốc không bị xóa).",
                confirmButtonText = "Xóa",
                isDestructive = true,
                onConfirm = {
                    musicRepository.removeFolder(context, uriString)
                    folderPendingRemove = null
                },
                onDismiss = { folderPendingRemove = null }
            )
        }
    }
}
