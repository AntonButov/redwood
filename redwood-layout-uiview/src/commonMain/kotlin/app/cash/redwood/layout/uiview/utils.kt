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
package app.cash.redwood.layout.uiview

import app.cash.redwood.yoga.Node
import app.cash.redwood.yoga.Size
import kotlinx.cinterop.cValue
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectZero
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIView

@Suppress("UNCHECKED_CAST")
internal val UIView.typedSubviews: List<UIView>
  get() = subviews as List<UIView>

internal val Node.view: UIView
  get() = (measureCallback as UIViewMeasureCallback).view

internal val undefinedSize = CGSizeMake(Size.UNDEFINED.toDouble(), Size.UNDEFINED.toDouble())

internal val zeroSize = cValue<CGRect> { CGRectZero }
