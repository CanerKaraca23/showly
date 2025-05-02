@file:Suppress("ktlint")

package com.michaldrabik.data_local.database.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.michaldrabik.data_local.database.model.Movie
import com.michaldrabik.data_local.database.model.PersonCredits
import com.michaldrabik.data_local.database.model.Show
import com.michaldrabik.data_local.sources.PeopleCreditsLocalDataSource

@Dao
interface PeopleCreditsDao : BaseDao<PersonCredits>, PeopleCreditsLocalDataSource {

  @Query(
    """
    SELECT
    shows.id_trakt,
    shows.id_tvdb,
    shows.id_tmdb,
    shows.id_imdb,
    shows.id_slug,
    shows.id_tvrage,
    shows.title,
    shows.year,
    shows.overview,
    shows.first_aired,
    shows.runtime,
    shows.airtime_day,
    shows.airtime_time,
    shows.airtime_timezone,
    shows.certification,
    shows.network,
    shows.country,
    shows.trailer,
    shows.homepage,
    shows.status,
    shows.rating,
    shows.votes,
    shows.comment_count,
    shows.genres,
    shows.aired_episodes,
    people_credits.created_at AS created_at,
    people_credits.updated_at AS updated_at
    FROM shows
    INNER JOIN people_credits ON people_credits.id_trakt_show = shows.id_trakt
    WHERE people_credits.id_trakt_person = :personTraktId
    """
  )
  override suspend fun getAllShowsForPerson(personTraktId: Long): List<Show>

  @Query(
    """
    SELECT
    movies.id_trakt,
    movies.id_tmdb,
    movies.id_imdb,
    movies.id_slug,
    movies.title,
    movies.year,
    movies.overview,
    movies.released,
    movies.runtime,
    movies.country,
    movies.trailer,
    movies.language,
    movies.homepage,
    movies.status,
    movies.rating,
    movies.votes,
    movies.comment_count,
    movies.genres,
    people_credits.updated_at AS updated_at,
    people_credits.created_at AS created_at
    FROM movies
    INNER JOIN people_credits ON people_credits.id_trakt_movie = movies.id_trakt
    WHERE people_credits.id_trakt_person = :personTraktId
    """
  )
  override suspend fun getAllMoviesForPerson(personTraktId: Long): List<Movie>

  @Query("SELECT updated_at FROM people_credits WHERE id_trakt_person = :personTraktId LIMIT 1")
  override suspend fun getTimestampForPerson(personTraktId: Long): Long?

  @Query("DELETE FROM people_credits WHERE id_trakt_person == :personTraktId")
  override suspend fun deleteAllForPerson(personTraktId: Long)

  @Transaction
  override suspend fun insertSingle(
    personTraktId: Long,
    credits: List<PersonCredits>
  ) {
    deleteAllForPerson(personTraktId)
    insert(credits)
  }
}
