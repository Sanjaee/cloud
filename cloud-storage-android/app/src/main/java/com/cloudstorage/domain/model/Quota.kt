package com.cloudstorage.domain.model

data class Quota(
    val totalQuota: Long = 10737418240L,
    val usedStorage: Long = 0L,
    val availableStorage: Long = 10737418240L,
    val percentageUsed: Double = 0.0,
    val fileCount: Int = 0
) {
    val formattedUsed: String
        get() = formatBytes(usedStorage)

    val formattedTotal: String
        get() = formatBytes(totalQuota)

    val formattedAvailable: String
        get() = formatBytes(availableStorage)

    private fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val df = java.text.DecimalFormat("#,##0.#")
        return "${df.format(bytes / Math.pow(1024.0, digitGroups.toDouble()))} ${units[digitGroups]}"
    }
}

