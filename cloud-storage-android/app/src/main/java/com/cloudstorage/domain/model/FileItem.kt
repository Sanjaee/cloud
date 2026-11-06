package com.cloudstorage.domain.model

data class FileItem(
    val id: String,
    val ownerId: String? = null,
    val folderId: String? = null,
    val storageObjectId: String? = null,
    val name: String,
    val originalName: String? = null,
    val mimeType: String = "application/octet-stream",
    val size: Long = 0L,
    val isStarred: Boolean = false,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val deletedAt: String? = null
) {
    val isImage: Boolean
        get() = mimeType.startsWith("image/")

    val isVideo: Boolean
        get() = mimeType.startsWith("video/")

    val isAudio: Boolean
        get() = mimeType.startsWith("audio/")

    val isPdf: Boolean
        get() = mimeType == "application/pdf"

    val formattedSize: String
        get() {
            if (size <= 0) return "0 B"
            val units = arrayOf("B", "KB", "MB", "GB", "TB")
            val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
            val df = java.text.DecimalFormat("#,##0.#")
            return "${df.format(size / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
        }
}

