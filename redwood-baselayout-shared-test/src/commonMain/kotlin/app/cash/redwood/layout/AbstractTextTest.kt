/*
 * Copyright (C) 2023 Square, Inc.
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
package app.cash.redwood.layout

import app.cash.redwood.baselayout.widget.Text
import app.cash.redwood.snapshot.testing.Snapshotter
import kotlin.test.Test

interface AbstractTextTest<T : Any> {

  fun widget(): Text<T>

  fun snapshotter(widget: T): Snapshotter

  @Test fun showText(text: String) {
    val widget = widget()
    widget.text(text)
    snapshotter(widget.value).snapshot()
  }

}
