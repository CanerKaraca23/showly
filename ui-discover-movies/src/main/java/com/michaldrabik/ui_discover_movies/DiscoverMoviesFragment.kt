package com.michaldrabik.ui_discover_movies

import android.os.Bundle
import android.view.View
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateMargins
import androidx.core.view.updatePadding
import androidx.fragment.app.clearFragmentResultListener
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.SimpleItemAnimator
import com.michaldrabik.ui_base.BaseFragment
import com.michaldrabik.ui_base.common.OnTabReselectedListener
import com.michaldrabik.ui_base.common.sheets.context_menu.ContextMenuBottomSheet
import com.michaldrabik.ui_base.utilities.extensions.add
import com.michaldrabik.ui_base.utilities.extensions.colorFromAttr
import com.michaldrabik.ui_base.utilities.extensions.dimenToPx
import com.michaldrabik.ui_base.utilities.extensions.disableUi
import com.michaldrabik.ui_base.utilities.extensions.doOnApplyWindowInsets
import com.michaldrabik.ui_base.utilities.extensions.enableUi
import com.michaldrabik.ui_base.utilities.extensions.fadeIn
import com.michaldrabik.ui_base.utilities.extensions.fadeOut
import com.michaldrabik.ui_base.utilities.extensions.launchAndRepeatStarted
import com.michaldrabik.ui_base.utilities.extensions.navigateToSafe
import com.michaldrabik.ui_base.utilities.extensions.onClick
import com.michaldrabik.ui_base.utilities.extensions.visible
import com.michaldrabik.ui_base.utilities.extensions.withSpanSizeLookup
import com.michaldrabik.ui_base.utilities.viewBinding
import com.michaldrabik.ui_discover_movies.databinding.FragmentDiscoverMoviesBinding
import com.michaldrabik.ui_discover_movies.helpers.DiscoverMoviesLayoutManagerProvider
import com.michaldrabik.ui_discover_movies.recycler.DiscoverMovieListItem
import com.michaldrabik.ui_discover_movies.recycler.DiscoverMoviesAdapter
import com.michaldrabik.ui_model.Movie
import com.michaldrabik.ui_navigation.java.NavigationArgs
import dagger.hilt.android.AndroidEntryPoint
import kotlin.random.Random

@AndroidEntryPoint
internal class DiscoverMoviesFragment :
  BaseFragment<DiscoverMoviesViewModel>(R.layout.fragment_discover_movies),
  OnTabReselectedListener {

  companion object {
    const val REQUEST_DISCOVER_FILTERS = "REQUEST_DISCOVER_FILTERS"
  }

  private val binding by viewBinding(FragmentDiscoverMoviesBinding::bind)

  override val viewModel by viewModels<DiscoverMoviesViewModel>()
  override val navigationId = R.id.discoverMoviesFragment

  private val swipeRefreshStartOffset by lazy { requireContext().dimenToPx(R.dimen.swipeRefreshStartOffset) }
  private val swipeRefreshEndOffset by lazy { requireContext().dimenToPx(R.dimen.swipeRefreshEndOffset) }

  private var adapter: DiscoverMoviesAdapter? = null
  private var layoutManager: GridLayoutManager? = null

  private var searchViewPosition = 0F
  private var tabsViewPosition = 0F
  private var filtersViewPosition = 0F

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    savedInstanceState?.let {
      searchViewPosition = it.getFloat("ARG_SEARCH_POS", 0F)
      tabsViewPosition = it.getFloat("ARG_TABS_POS", 0F)
      filtersViewPosition = it.getFloat("ARG_FILTERS_POS", 0F)
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putFloat("ARG_SEARCH_POS", searchViewPosition)
    outState.putFloat("ARG_TABS_POS", tabsViewPosition)
    outState.putFloat("ARG_FILTERS_POS", filtersViewPosition)
  }

  override fun onResume() {
    super.onResume()
    showNavigation()
  }

  override fun onPause() {
    enableUi()
    with(binding) {
      searchViewPosition = discoverMoviesSearchView.translationY
      tabsViewPosition = discoverMoviesTabsView.translationY
      filtersViewPosition = discoverMoviesFiltersView.translationY
    }
    super.onPause()
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?,
  ) {
    super.onViewCreated(view, savedInstanceState)
    setupView()
    setupInsets()
    setupRecycler()
    setupSwipeRefresh()

    launchAndRepeatStarted(
      { viewModel.uiState.collect { render(it) } },
      { viewModel.messageFlow.collect { showSnack(it) } },
      doAfterLaunch = { viewModel.loadMovies() },
    )

    setFragmentResultListener(REQUEST_DISCOVER_FILTERS) { _, _ ->
      viewModel.loadMovies(resetScroll = true, skipCache = true, instantProgress = true)
    }
  }

  private fun setupView() {
    with(binding) {
      discoverMoviesSearchView.run {
        translationY = searchViewPosition
        settingsIconVisible = true
        isEnabled = false
        onClick { openSearch() }
        onSettingsClickListener = {
          hideNavigation()
          navigateToSafe(R.id.actionDiscoverMoviesFragmentToSettingsFragment)
        }
      }
      discoverMoviesTabsView.run {
        translationY = tabsViewPosition
        onModeSelected = { mode = it }
        selectMovies()
      }
      discoverMoviesFiltersView.run {
        translationY = filtersViewPosition
        onGenresChipClick = { navigateToSafe(R.id.actionDiscoverMoviesFragmentToFiltersGenres) }
        onFeedChipClick = { navigateToSafe(R.id.actionDiscoverMoviesFragmentToFiltersFeed) }
        onHideCollectionChipClick = { viewModel.toggleCollection() }
      }
    }
  }

  private fun setupInsets() {
    with(binding) {
      discoverMoviesRoot.doOnApplyWindowInsets { _, insets, _, _ ->
        val tabletOffset = if (isTablet) dimenToPx(R.dimen.spaceMedium) else 0
        val statusBarSize = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top + tabletOffset
        discoverMoviesRecycler
          .updatePadding(top = statusBarSize + dimenToPx(R.dimen.discoverRecyclerPadding))
        (discoverMoviesSearchView.layoutParams as ViewGroup.MarginLayoutParams)
          .updateMargins(top = statusBarSize + dimenToPx(R.dimen.spaceMedium))
        (discoverMoviesTabsView.layoutParams as ViewGroup.MarginLayoutParams)
          .updateMargins(top = statusBarSize + dimenToPx(R.dimen.collectionTabsMargin))
        (discoverMoviesFiltersView.layoutParams as ViewGroup.MarginLayoutParams)
          .updateMargins(top = statusBarSize + dimenToPx(R.dimen.collectionFiltersMargin))
        discoverMoviesSwipeRefresh.setProgressViewOffset(
          true,
          swipeRefreshStartOffset + statusBarSize,
          swipeRefreshEndOffset,
        )
      }
    }
  }

  private fun setupRecycler() {
    layoutManager = DiscoverMoviesLayoutManagerProvider.provideLayoutManager(requireContext())
    adapter = DiscoverMoviesAdapter(
      itemClickListener = { openDetails(it) },
      itemLongClickListener = { openMovieMenu(it.movie) },
      missingImageListener = { ids, force -> viewModel.loadMissingImage(ids, force) },
      listChangeListener = { binding.discoverMoviesRecycler.scrollToPosition(0) },
    )
    binding.discoverMoviesRecycler.apply {
      adapter = this@DiscoverMoviesFragment.adapter
      layoutManager = this@DiscoverMoviesFragment.layoutManager
      (itemAnimator as SimpleItemAnimator).supportsChangeAnimations = false
      setHasFixedSize(true)
    }
  }

  private fun setupSwipeRefresh() {
    binding.discoverMoviesSwipeRefresh.apply {
      val color = requireContext().colorFromAttr(R.attr.colorAccent)
      setProgressBackgroundColorSchemeColor(requireContext().colorFromAttr(R.attr.colorSearchViewBackground))
      setColorSchemeColors(color, color, color)
      setOnRefreshListener {
        searchViewPosition = 0F
        tabsViewPosition = 0F
        viewModel.loadMovies(pullToRefresh = true)
      }
    }
  }

  override fun setupBackPressed() {
    val dispatcher = requireActivity().onBackPressedDispatcher
    dispatcher.addCallback(viewLifecycleOwner) {
      isEnabled = false
      activity?.onBackPressed()
    }
  }

  private fun openSearch() {
    disableUi()
    hideNavigation()
    with(binding) {
      discoverMoviesTabsView.fadeOut(duration = 200).add(animations)
      discoverMoviesFiltersView.fadeOut(duration = 200).add(animations)
      discoverMoviesRecycler
        .fadeOut(duration = 200) {
          navigateToSafe(R.id.actionDiscoverMoviesFragmentToSearchFragment)
        }.add(animations)
    }
  }

  private fun openDetails(item: DiscoverMovieListItem) {
    if (!binding.discoverMoviesRecycler.isEnabled) return
    disableUi()
    hideNavigation()
    animateItemsExit(item)
  }

  private fun openMovieMenu(movie: Movie) {
    if (!binding.discoverMoviesRecycler.isEnabled) return
    setFragmentResultListener(NavigationArgs.REQUEST_ITEM_MENU) { requestKey, _ ->
      if (requestKey == NavigationArgs.REQUEST_ITEM_MENU) {
        viewModel.loadMovies()
      }
      clearFragmentResultListener(NavigationArgs.REQUEST_ITEM_MENU)
    }
    val bundle = ContextMenuBottomSheet.createBundle(movie.ids.trakt)
    navigateToSafe(R.id.actionDiscoverMoviesFragmentToItemMenu, bundle)
  }

  private fun animateItemsExit(item: DiscoverMovieListItem) {
    with(binding) {
      discoverMoviesSearchView.fadeOut().add(animations)
      discoverMoviesTabsView.fadeOut().add(animations)
      discoverMoviesFiltersView.fadeOut().add(animations)

      val clickedIndex = adapter?.indexOf(item) ?: 0
      val itemCount = adapter?.itemCount ?: 0
      (0..itemCount).forEach {
        if (it != clickedIndex) {
          val view = discoverMoviesRecycler.findViewHolderForAdapterPosition(it)
          view?.let { v ->
            val randomDelay = Random.nextLong(50, 200)
            v.itemView.fadeOut(duration = 150, startDelay = randomDelay).add(animations)
          }
        }
      }

      val clickedView = discoverMoviesRecycler.findViewHolderForAdapterPosition(clickedIndex)
      clickedView
        ?.itemView
        ?.fadeOut(
          duration = 150,
          startDelay = 350,
          endAction = {
            if (!isResumed) return@fadeOut
            val bundle = Bundle().apply { putLong(NavigationArgs.ARG_MOVIE_ID, item.movie.traktId) }
            navigateToSafe(R.id.actionDiscoverMoviesFragmentToMovieDetailsFragment, bundle)
          },
        ).add(animations)
    }
  }

  private fun render(uiState: DiscoverMoviesUiState) {
    uiState.run {
      with(binding) {
        items?.let {
          val resetScroll = resetScroll?.consume() == true
          adapter?.setItems(it, resetScroll)
          layoutManager?.withSpanSizeLookup { pos ->
            adapter
              ?.getItems()
              ?.get(pos)
              ?.image
              ?.type
              ?.getSpan(isTablet)!!
          }
          discoverMoviesRecycler.fadeIn(200, withHardware = true)
        }
        isSyncing?.let {
          discoverMoviesSearchView.setTraktProgress(it)
          discoverMoviesSearchView.isEnabled = !it
        }
        isLoading?.let {
          discoverMoviesSwipeRefresh.isRefreshing = it
          discoverMoviesSearchView.isEnabled = !it
          discoverMoviesTabsView.isEnabled = !it
          discoverMoviesFiltersView.isEnabled = !it
          discoverMoviesRecycler.isEnabled = !it
        }
        filters?.let {
          if (discoverMoviesFiltersView.visibility != VISIBLE) {
            discoverMoviesFiltersView.visible()
          }
          discoverMoviesFiltersView.bind(it)
        }
      }
    }
  }

  override fun onTabReselected() = openSearch()

  override fun onDestroyView() {
    adapter = null
    layoutManager = null
    super.onDestroyView()
  }
}
