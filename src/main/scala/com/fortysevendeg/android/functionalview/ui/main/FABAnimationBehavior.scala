/*
 *  Copyright (C) 2015 47 Degrees, LLC http://47deg.com hello@47deg.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fortysevendeg.android.functionalview.ui.main

import android.animation.{Animator, AnimatorListenerAdapter}
import android.content.Context
import android.support.design.widget.{CoordinatorLayout, FloatingActionButton}
import android.support.v4.view.ViewCompat
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.util.AttributeSet
import android.view.View
import macroid.Snail
import macroid.FullDsl._
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Promise

class FABAnimationBehavior
  extends FloatingActionButton.Behavior {

  def this(context: Context, attrs: AttributeSet) = this()

  var isAnimatingOut = false

  val interpolator = new FastOutSlowInInterpolator()

  val duration = 200L

  override def onStartNestedScroll(
    coordinatorLayout: CoordinatorLayout,
    child: FloatingActionButton,
    directTargetChild: View,
    target: View,
    nestedScrollAxes: Int): Boolean =
    nestedScrollAxes == ViewCompat.SCROLL_AXIS_VERTICAL ||
      super.onStartNestedScroll(coordinatorLayout, child, directTargetChild, target, nestedScrollAxes)

  override def onNestedScroll(
    coordinatorLayout: CoordinatorLayout,
    child: FloatingActionButton,
    target: View,
    dxConsumed: Int,
    dyConsumed: Int,
    dxUnconsumed: Int,
    dyUnconsumed: Int): Unit = {
    super.onNestedScroll(coordinatorLayout, child, target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed)
    (dyConsumed, Option(child), isAnimatingOut) match {
      case (d, Some(c), false) if d > 0 && c.getVisibility == View.VISIBLE =>
        runUi(Option(child) <~~ animateOut)
      case (d, Some(c), _) if d < 0 && c.getVisibility != View.VISIBLE =>
        runUi(Option(child) <~~ animateIn)
      case _ =>
    }
  }

  val animateIn = Snail[FloatingActionButton] {
    view =>
      view.setVisibility(View.VISIBLE)
      val animPromise = Promise[Unit]()
      view.animate
        .translationY(0)
        .setInterpolator(interpolator)
        .setDuration(duration)
        .setListener(new AnimatorListenerAdapter {
          override def onAnimationEnd(animation: Animator): Unit = {
            super.onAnimationEnd(animation)
            animPromise.success((): Unit)
          }
        }).start()
      animPromise.future
  }

  val animateOut = Snail[FloatingActionButton] {
    view =>
      val animPromise = Promise[Unit]()
      val y = view.getHeight + (view.getPaddingBottom * 2)
      view.animate
        .translationY(y)
        .setInterpolator(interpolator)
        .setDuration(duration)
        .setListener(new AnimatorListenerAdapter {
          override def onAnimationStart(animation: Animator): Unit = {
            super.onAnimationStart(animation)
            isAnimatingOut = true
          }
          override def onAnimationCancel(animation: Animator): Unit = {
            super.onAnimationCancel(animation)
            isAnimatingOut = false
          }
          override def onAnimationEnd(animation: Animator): Unit = {
            super.onAnimationEnd(animation)
            isAnimatingOut = false
            view.setVisibility(View.GONE)
            animPromise.success((): Unit)
          }
        }).start()
      animPromise.future
  }
}