@file:Suppress("ktlint:standard:max-line-length")

package com.michaldrabik.data_local.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.michaldrabik.data_local.database.model.ArchiveShow
import com.michaldrabik.data_local.database.model.Show
import com.michaldrabik.data_local.sources.ArchiveShowsLocalDataSource

@Dao
interface ArchiveShowsDao : ArchiveShowsLocalDataSource {

  @Query(
    "SELECT " +
      "shows.id_trakt, " +
      "shows.id_tvdb, " +
      "shows.id_tmdb, " +
      "shows.id_imdb, " +
      "shows.id_slug, " +
      "shows.id_tvrage, " +
      "shows.title, " +
      "shows.year, " +
      "shows.overview, " +
      "shows.first_aired, " +
      "shows.runtime, " +
      "shows.airtime_day, " +
      "shows.airtime_time, " +
      "shows.airtime_timezone, " +
      "shows.certification, " +
      "shows.network, " +
      "shows.country, " +
      "shows.trailer, " +
      "shows.homepage, " +
      "shows.status, " +
      "shows.rating, " +
      "shows.votes, " +
      "shows.comment_count, " +
      "shows.genres, " +
      "shows.aired_episodes, " +
      "shows_archive.updated_at, " +
      "shows_archive.created_at " +
      "FROM shows " +
      "INNER JOIN shows_archive USING(id_trakt)",
  )
  override suspend fun getAll(): List<Show>

  @Query(
    "SELECT " +
      "shows.id_trakt, " +
      "shows.id_tvdb, " +
      "shows.id_tmdb, " +
      "shows.id_imdb, " +
      "shows.id_slug, " +
      "shows.id_tvrage, " +
      "shows.title, " +
      "shows.year, " +
      "shows.overview, " +
      "shows.first_aired, " +
      "shows.runtime, " +
      "shows.airtime_day, " +
      "shows.airtime_time, " +
      "shows.airtime_timezone, " +
      "shows.certification, " +
      "shows.network, " +
      "shows.country, " +
      "shows.trailer, " +
      "shows.homepage, " +
      "shows.status, " +
      "shows.rating, " +
      "shows.votes, " +
      "shows.comment_count, " +
      "shows.genres, " +
      "shows.aired_episodes, " +
      "shows_archive.updated_at, " +
      "shows_archive.created_at " +
      "FROM shows " +
      "INNER JOIN shows_archive USING(id_trakt) WHERE id_trakt IN (:ids)",
  )
  override suspend fun getAll(ids: List<Long>): List<Show>

  @Query("SELECT shows.id_trakt FROM shows INNER JOIN shows_archive USING(id_trakt)")
  override suspend fun getAllTraktIds(): List<Long>

  @Query("SELECT shows.* FROM shows INNER JOIN shows_archive USING(id_trakt) WHERE id_trakt == :traktId")
  override suspend fun getById(traktId: Long): Show?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  override suspend fun insert(show: ArchiveShow)

  @Query("DELETE FROM shows_archive WHERE id_trakt == :traktId")
  override suspend fun deleteById(traktId: Long)
}
