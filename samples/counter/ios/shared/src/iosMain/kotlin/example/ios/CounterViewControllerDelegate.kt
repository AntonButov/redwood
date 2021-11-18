/*
 * Copyright (C) 2021 Square, Inc.
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
package example.ios

import androidx.compose.runtime.BroadcastFrameClock
import app.cash.treehouse.protocol.widget.ProtocolDisplay
import example.ios.sunspot.IosSunspotBox
import example.ios.sunspot.IosSunspotNodeFactory
import example.shared.Counter
import example.sunspot.compose.ProtocolComposeWidgetFactory
import example.sunspot.compose.SunspotComposition
import example.sunspot.widget.ProtocolDisplayWidgetFactory
import example.sunspot.widget.ProtocolSunspotBox
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.plus
import platform.Foundation.NSLog
import platform.UIKit.UIStackView

class CounterViewControllerDelegate(
  root: UIStackView,
) {
  private val clock = BroadcastFrameClock()
  private val scope = MainScope() + clock

  init {
    val composition = SunspotComposition(
      scope = scope,
      factory = ProtocolComposeWidgetFactory(),
      onDiff = { NSLog("TreehouseDiff: $it") },
      onEvent = { NSLog("TreehouseEvent: $it") }
    )

    val display = ProtocolDisplay(
      root = ProtocolSunspotBox(IosSunspotBox(root)),
      factory = ProtocolDisplayWidgetFactory(IosSunspotNodeFactory),
      eventSink = composition
    )

    composition.start(display)

    composition.setContent {
      Counter()
    }
  }

  fun tickClock() {
    clock.sendFrame(0L) // Compose does not use frame time.
  }

  fun dispose() {
    scope.cancel()
  }
}
