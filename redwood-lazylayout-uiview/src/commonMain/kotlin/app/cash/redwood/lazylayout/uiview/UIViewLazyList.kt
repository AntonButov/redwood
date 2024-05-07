/*
 * Copyright (C) 2022 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress(
  "OVERRIDE_DEPRECATION",
  "PARAMETER_NAME_CHANGED_ON_OVERRIDE",
  "RETURN_TYPE_MISMATCH_ON_OVERRIDE",
)

package app.cash.redwood.lazylayout.uiview

import app.cash.redwood.Modifier
import app.cash.redwood.layout.api.Constraint
import app.cash.redwood.layout.api.CrossAxisAlignment
import app.cash.redwood.lazylayout.api.ScrollItemIndex
import app.cash.redwood.lazylayout.widget.LazyList
import app.cash.redwood.lazylayout.widget.LazyListScrollProcessor
import app.cash.redwood.lazylayout.widget.LazyListUpdateProcessor
import app.cash.redwood.lazylayout.widget.LazyListUpdateProcessor.Binding
import app.cash.redwood.lazylayout.widget.RefreshableLazyList
import app.cash.redwood.ui.Margin
import app.cash.redwood.widget.ChangeListener
import app.cash.redwood.widget.Widget
import kotlin.math.max
import kotlin.math.min
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ObjCClass
import kotlinx.cinterop.cValue
import kotlinx.cinterop.convert
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.cinterop.zeroValue
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.CoreGraphics.CGSizeZero
import platform.Foundation.NSIndexPath
import platform.Foundation.NSUUID.Companion.UUID
import platform.Foundation.classForCoder
import platform.UIKit.NSLayoutConstraint
import platform.UIKit.UIColor
import platform.UIKit.UIControlEventValueChanged
import platform.UIKit.UIEdgeInsetsMake
import platform.UIKit.UILabel
import platform.UIKit.UIRefreshControl
import platform.UIKit.UIScrollView
import platform.UIKit.UITableView
import platform.UIKit.UITableViewAutomaticDimension
import platform.UIKit.UITableViewCell
import platform.UIKit.UITableViewCellSeparatorStyle.UITableViewCellSeparatorStyleNone
import platform.UIKit.UITableViewCellStyle
import platform.UIKit.UITableViewDataSourcePrefetchingProtocol
import platform.UIKit.UITableViewDataSourceProtocol
import platform.UIKit.UITableViewDelegateProtocol
import platform.UIKit.UITableViewRowAnimationNone
import platform.UIKit.UITableViewScrollPosition
import platform.UIKit.UITableViewStyle
import platform.UIKit.UIView
import platform.UIKit.UIViewNoIntrinsicMetric
import platform.UIKit.indexPathForItem
import platform.UIKit.item
import platform.darwin.NSInteger
import platform.darwin.NSObject

internal open class UIViewLazyList() : LazyList<UIView>, ChangeListener {
  internal val tableView: UITableView = object : UITableView(
    CGRectZero.readValue(),
    UITableViewStyle.UITableViewStylePlain,
  ) {
    override fun setContentOffset(contentOffset: CValue<CGPoint>, animated: Boolean) {
      // If the caller is requesting a contentOffset with y == 0,
      // and the current contentOffset.y is not 0,
      // assume that it's a programmatic scroll-to-top call.
      if (contentOffset.useContents { y } == 0.0 && this.contentOffset.useContents { y } != 0.0) {
        ignoreScrollUpdates = true
        scrollProcessor.onScrollToTop()
      }
      super.setContentOffset(contentOffset, animated)
    }
  }

  override var modifier: Modifier = Modifier

  override val value: UIView
    get() = tableView

  private val updateProcessor = object : LazyListUpdateProcessor<LazyListContainerCell, UIView>() {
    override fun createPlaceholder(original: UIView): UIView? {
      return object : UIView(CGRectZero.readValue()) {
        override fun sizeThatFits(size: CValue<CGSize>) = original.sizeThatFits(size)
      }
    }

    override fun insertRows(index: Int, count: Int) {
      // TODO(jwilson): pass a range somehow when 'count' is large?
      tableView.beginUpdates()
      UIView.performWithoutAnimation {
        tableView.insertRowsAtIndexPaths(
          (index until index + count).map { NSIndexPath.indexPathForItem(it.convert(), 0) },
          UITableViewRowAnimationNone,
        )
      }
      tableView.endUpdates()
    }

    override fun deleteRows(index: Int, count: Int) {
      // TODO(jwilson): pass a range somehow when 'count' is large?
      tableView.beginUpdates()
      UIView.performWithoutAnimation {
        tableView.deleteRowsAtIndexPaths(
          (index until index + count).map { NSIndexPath.indexPathForItem(it.convert(), 0) },
          UITableViewRowAnimationNone,
        )
      }
      tableView.endUpdates()
    }

    override fun setContent(view: LazyListContainerCell, content: Widget<UIView>?) {
      println("REDWOOD_DEBUG: UIViewLazyList.setContent on item: ${view.identifier}")
      view.content = content
    }
  }

  private var ignoreScrollUpdates = false

  private val scrollProcessor = object : LazyListScrollProcessor() {
    override fun contentSize() = updateProcessor.size

    override fun programmaticScroll(firstIndex: Int, animated: Boolean) {
      ignoreScrollUpdates = animated // Don't forward scroll updates to scrollProcessor.
      tableView.scrollToRowAtIndexPath(
        NSIndexPath.indexPathForItem(firstIndex.toLong(), 0),
        UITableViewScrollPosition.UITableViewScrollPositionTop,
        animated = animated,
      )
    }
  }

  override val placeholder: Widget.Children<UIView> = updateProcessor.placeholder

  override val items: Widget.Children<UIView> = updateProcessor.items

  private val dataSource = object : NSObject(), UITableViewDataSourceProtocol {
    override fun tableView(
      tableView: UITableView,
      numberOfRowsInSection: NSInteger,
    ): Long {
      require(numberOfRowsInSection == 0L)
      return updateProcessor.size.toLong()
    }

    override fun tableView(
      tableView: UITableView,
      cellForRowAtIndexPath: NSIndexPath,
    ): LazyListContainerCell {
      val index = cellForRowAtIndexPath.item.toInt()
      return updateProcessor.getOrCreateView(index) { binding ->
        createView(tableView, binding, index)
      }
    }

    private fun createView(
      tableView: UITableView,
      binding: Binding<LazyListContainerCell, UIView>,
      index: Int,
    ): LazyListContainerCell {
      val result = tableView.dequeueReusableCellWithIdentifier(
        identifier = REUSE_IDENTIFIER,
        forIndexPath = NSIndexPath.indexPathForItem(index.convert(), 0.convert()),
      ) as LazyListContainerCell
      require(result.binding == null)
      result.binding = binding
      println("REDWOOD_DEBUG: LazyList.createView on ${result.identifier}")
      return result
    }
  }

  private val tableViewDelegate: UITableViewDelegateProtocol =
    object : NSObject(), UITableViewDelegateProtocol {

      var cachedFirstIndex: Int = 0
      var cachedLastIndex: Int = 0

      override fun scrollViewDidScroll(scrollView: UIScrollView) {
        if (ignoreScrollUpdates) return // Only notify of user scrolls.

        val visibleIndexPaths = tableView.indexPathsForVisibleRows ?: return
        if (visibleIndexPaths.isEmpty()) return

        val firstIndex = visibleIndexPaths.minOf { (it as NSIndexPath).item.toInt() }
        val lastIndex = visibleIndexPaths.maxOf { (it as NSIndexPath).item.toInt() }

        if (firstIndex == cachedFirstIndex && lastIndex == cachedLastIndex) {
          println("REDWOOD_DEBUG: ❌❌ Redundant scrollProcessor.onUserScroll call firstIndex: $firstIndex lastIndex: $lastIndex")
        } else {
          cachedFirstIndex = firstIndex
          cachedLastIndex = lastIndex
        }

        scrollProcessor.onUserScroll(firstIndex, lastIndex)
      }

      /**
       * If the user begins a drag while we’re programmatically scrolling, well then we're not
       * programmatically scrolling anymore.
       */
      override fun scrollViewWillBeginDragging(scrollView: UIScrollView) {
        ignoreScrollUpdates = false
      }

      override fun scrollViewDidEndScrollingAnimation(scrollView: UIScrollView) {
        ignoreScrollUpdates = false
      }
    }

  init {
    tableView.apply {
      dataSource = this@UIViewLazyList.dataSource
      delegate = tableViewDelegate
      rowHeight = UITableViewAutomaticDimension
      separatorStyle = UITableViewCellSeparatorStyleNone
      backgroundColor = UIColor.clearColor

      registerClass(
        cellClass = LazyListContainerCell(UITableViewCellStyle.UITableViewCellStyleDefault, REUSE_IDENTIFIER)
          .initWithFrame(CGRectZero.readValue()).classForCoder() as ObjCClass?,
        forCellReuseIdentifier = REUSE_IDENTIFIER,
      )
    }
  }

  final override fun onViewportChanged(onViewportChanged: (Int, Int) -> Unit) {
    scrollProcessor.onViewportChanged(onViewportChanged)
  }

  override fun isVertical(isVertical: Boolean) {
    // TODO: support horizontal LazyLists.
  }

  // TODO Dynamically update width and height of UIViewLazyList when set
  override fun width(width: Constraint) {}

  override fun height(height: Constraint) {}

  override fun margin(margin: Margin) {
    tableView.contentInset = UIEdgeInsetsMake(
      margin.top.value,
      margin.start.value,
      margin.end.value,
      margin.bottom.value,
    )
  }

  override fun crossAxisAlignment(crossAxisAlignment: CrossAxisAlignment) {
    // TODO Support CrossAxisAlignment in `redwood-lazylayout-uiview`
  }

  override fun scrollItemIndex(scrollItemIndex: ScrollItemIndex) {
    scrollProcessor.scrollItemIndex(scrollItemIndex)
  }

  override fun itemsBefore(itemsBefore: Int) {
    updateProcessor.itemsBefore(itemsBefore)
  }

  override fun itemsAfter(itemsAfter: Int) {
    updateProcessor.itemsAfter(itemsAfter)
  }

  override fun onEndChanges() {
    updateProcessor.onEndChanges()
    scrollProcessor.onEndChanges()
  }
}

private const val REUSE_IDENTIFIER = "LazyListContainerCell"

internal class LazyListContainerCell(
  style: UITableViewCellStyle,
  reuseIdentifier: String?,
) : UITableViewCell(style, reuseIdentifier) {
  internal var binding: Binding<LazyListContainerCell, UIView>? = null

  val identifier = UUID().UUIDString.removeRange(0 until 7)
  internal var content: Widget<UIView>? = null
    set(value) {
      field = value

      removeAllSubviews()
      if (value != null) {
        contentView.addSubview(value.value)
        value.value.setNeedsLayout()
      }
    }

  override fun initWithStyle(
    style: UITableViewCellStyle,
    reuseIdentifier: String?,
  ): UITableViewCell {
    println("REDWOOD_DEBUG: 🐣INIT LazyListContainerCell.initWithStyle reuseIdentifier: ${reuseIdentifier ?: "?? null"} $identifier")
    return LazyListContainerCell(style, reuseIdentifier)
  }

  override fun initWithFrame(
    frame: CValue<CGRect>,
  ): UITableViewCell {
    println("REDWOOD_DEBUG: 🐣INIT LazyListContainerCell.initWithFrame $identifier")
    return LazyListContainerCell(UITableViewCellStyle.UITableViewCellStyleDefault, null)
      .apply { setFrame(frame) }
  }

  override fun willMoveToSuperview(newSuperview: UIView?) {
    super.willMoveToSuperview(newSuperview)

    backgroundColor = UIColor.clearColor

    // Confirm the cell is bound when it's about to be displayed.
    if (superview == null && newSuperview != null) {
      require(binding!!.isBound) { "about to display a cell that isn't bound!" }
    }

    // Unbind the cell when its view is detached from the table.
    if (superview != null && newSuperview == null) {
      binding?.unbind()
      binding = null
    }
  }

  override fun prepareForReuse() {
    println("REDWOOD_DEBUG: LazyListContainerCell.prepareForReuse $identifier")
    super.prepareForReuse()
    binding?.unbind()
    binding = null
  }

  private var layoutSubviewsCount = 0
  override fun layoutSubviews() {
    super.layoutSubviews()

    layoutSubviewsCount += 1
    println("REDWOOD_DEBUG: ${if (layoutSubviewsCount <= 1) "🎨" else "🎨⭕"} LazyListContainerCell.layoutSubviews️ $identifier")

    val content = this.content ?: return
    content.value.setFrame(bounds)
  }

  private var sizeThatFitsCount = 0
  override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> {
    sizeThatFitsCount += 1
    println("REDWOOD_DEBUG: ${if (sizeThatFitsCount <= 1) "📐" else "📐⭕️"} LazyListContainerCell.sizeThatFits $identifier")
    return content?.value?.sizeThatFits(size) ?: return super.sizeThatFits(size)
  }


  private var removeAllSubviewsCount = 0
  private fun removeAllSubviews() {
    removeAllSubviewsCount += 1
    println("REDWOOD_DEBUG: ${if (removeAllSubviewsCount <= 1) "␡" else "␡⭕️"} LazyListContainerCell.removeAllSubviews $identifier")
    contentView.subviews.forEach {
      (it as UIView).removeFromSuperview()
    }
    selectedBackgroundView = null
  }
}

internal class UIViewRefreshableLazyList : UIViewLazyList(), RefreshableLazyList<UIView> {

  private var onRefresh: (() -> Unit)? = null

  private val refreshControl by lazy {
    UIRefreshControl().apply {
      setEventHandler(UIControlEventValueChanged) {
        onRefresh?.invoke()
      }
    }
  }

  override fun refreshing(refreshing: Boolean) {
    if (refreshing != refreshControl.refreshing) {
      if (refreshing) {
        refreshControl.beginRefreshing()
      } else {
        refreshControl.endRefreshing()
      }
    }
  }

  override fun onRefresh(onRefresh: (() -> Unit)?) {
    this.onRefresh = onRefresh

    if (onRefresh != null) {
      if (tableView.refreshControl != refreshControl) {
        tableView.refreshControl = refreshControl
      }
    } else {
      refreshControl.removeFromSuperview()
    }
  }

  override fun pullRefreshContentColor(pullRefreshContentColor: UInt) {
    refreshControl.tintColor = UIColor(pullRefreshContentColor)
  }
}

private fun UIColor(color: UInt): UIColor = UIColor(
  alpha = ((color and 0xFF000000u) shr 24).toDouble() / 255.0,
  red = ((color and 0x00FF0000u) shr 16).toDouble() / 255.0,
  green = ((color and 0x0000FF00u) shr 8).toDouble() / 255.0,
  blue = (color and 0x000000FFu).toDouble() / 255.0,
)
