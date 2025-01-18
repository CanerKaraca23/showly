package com.michaldrabik.ui_backup.model

import com.squareup.moshi.Json

data class BackupMovies(
  @Json(name = "cH") val collectionHistory: List<BackupMovie> = emptyList(),
  @Json(name = "cW") val collectionWatchlist: List<BackupMovie> = emptyList(),
  @Json(name = "cHid") val collectionHidden: List<BackupMovie> = emptyList(),
  @Json(name = "pP") val progressPinned: List<Long> = emptyList(),
)

data class BackupMovie(
  @Json(name = "id") val traktId: Long,
  @Json(name = "tmId") val tmdbId: Long,
  @Json(name = "t") val title: String,
  @Json(name = "a") val addedAt: String,
)
