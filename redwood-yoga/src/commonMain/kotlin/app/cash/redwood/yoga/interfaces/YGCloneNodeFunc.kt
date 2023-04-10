/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package app.cash.redwood.yoga.interfaces

import app.cash.redwood.yoga.YGNode

fun interface YGCloneNodeFunc {
  operator fun invoke(
    node: YGNode,
    owner: YGNode?,
    childIndex: Int,
  ): YGNode
}
