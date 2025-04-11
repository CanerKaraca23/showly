package com.michaldrabik.ui_discover_movies.filters.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.FrameLayout
import androidx.core.view.children
import com.michaldrabik.ui_base.utilities.extensions.onClick
import com.michaldrabik.ui_discover_movies.R
import com.michaldrabik.ui_discover_movies.databinding.ViewDiscoverMoviesFiltersBinding
import com.michaldrabik.ui_model.DiscoverFeed
import com.michaldrabik.ui_model.DiscoverFeed.ANTICIPATED
import com.michaldrabik.ui_model.DiscoverFeed.POPULAR
import com.michaldrabik.ui_model.DiscoverFeed.RECENT
import com.michaldrabik.ui_model.DiscoverFeed.TRENDING
import com.michaldrabik.ui_model.DiscoverFilters
import com.michaldrabik.ui_model.Genre

class DiscoverMoviesFiltersView : FrameLayout {

  constructor(context: Context) : super(context)
  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

  private val binding = ViewDiscoverMoviesFiltersBinding.inflate(LayoutInflater.from(context), this)

  var onFeedChipClick: (() -> Unit)? = null
  var onGenresChipClick: (() -> Unit)? = null
  var onHideCollectionChipClick: (() -> Unit)? = null

  private lateinit var filters: DiscoverFilters

  init {
    layoutParams = LayoutParams(MATCH_PARENT, WRAP_CONTENT)
    with(binding) {
      discoverMoviesGenresChip.text = discoverMoviesGenresChip.text.toString().filter { it.isLetter() }
      discoverMoviesGenresChip.onClick { onGenresChipClick?.invoke() }
      discoverMoviesFeedChip.onClick { onFeedChipClick?.invoke() }
      discoverMoviesCollectionChip.onClick { onHideCollectionChipClick?.invoke() }
    }
  }

  fun bind(filters: DiscoverFilters) {
    this.filters = filters
    bindFeed(filters.feedOrder)
    bindGenres(filters.genres)
    with(binding) {
      discoverMoviesCollectionChip.isChecked = filters.hideCollection
    }
  }

  private fun bindFeed(feed: DiscoverFeed) {
    with(binding) {
      discoverMoviesFeedChip.text = when (feed) {
        TRENDING -> context.getString(R.string.textFeedTrending)
        POPULAR -> context.getString(R.string.textFeedPopular)
        ANTICIPATED -> context.getString(R.string.textFeedAnticipated)
        RECENT -> context.getString(R.string.textSortNewest)
      }
    }
  }

  private fun bindGenres(genres: List<Genre>) {
    with(binding) {
      discoverMoviesGenresChip.isSelected = genres.isNotEmpty()
      discoverMoviesGenresChip.text = when {
        genres.isEmpty() -> context.getString(R.string.textGenres).filter { it.isLetter() }
        genres.size == 1 -> context.getString(genres.first().displayName)
        genres.size == 2 -> "${context.getString(genres[0].displayName)}, ${context.getString(genres[1].displayName)}"
        else ->
          "${
            context.getString(
              genres[0].displayName,
            )
          }, ${context.getString(genres[1].displayName)} + ${genres.size - 2}"
      }
    }
  }

  override fun setEnabled(enabled: Boolean) {
    binding.discoverMoviesChips.children.forEach { it.isEnabled = enabled }
  }
}
