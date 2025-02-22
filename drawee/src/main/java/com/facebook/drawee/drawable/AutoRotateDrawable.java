/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.drawable;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import androidx.annotation.VisibleForTesting;
import com.facebook.common.internal.Preconditions;

/** Drawable that automatically rotates underlying drawable. */
public class AutoRotateDrawable extends ForwardingDrawable implements Runnable, CloneableDrawable {
  private static final int DEGREES_IN_FULL_ROTATION = 360;
  private static final int FRAME_INTERVAL_MS = 20;

  // Specified duration in milliseconds for one complete rotation.
  private int mInterval;
  // Specified rotation direction
  private boolean mClockwise;

  // Current angle by which the drawable is rotated.
  @VisibleForTesting float mRotationAngle = 0;

  // Whether we have our next frame scheduled for update
  private boolean mIsScheduled = false;

  /**
   * Creates a new AutoRotateDrawable with given underlying drawable, interval and a clockwise
   * rotation.
   *
   * @param drawable underlying drawable to apply the rotation to
   * @param interval duration in milliseconds of one complete rotation
   */
  public AutoRotateDrawable(Drawable drawable, int interval) {
    this(drawable, interval, true);
  }

  /**
   * Creates a new AutoRotateDrawable with given underlying drawable and interval.
   *
   * @param drawable underlying drawable to apply the rotation to
   * @param interval duration in milliseconds of one complete rotation
   * @param clockwise defines whether the rotation is clockwise or not
   */
  public AutoRotateDrawable(Drawable drawable, int interval, boolean clockwise) {
    super(Preconditions.checkNotNull(drawable));
    setParams(interval, clockwise);
  }

  // sets the interval and clockwise instances variables
  private void setParams(int interval, boolean clockwise) {
    this.mInterval = interval;
    this.mClockwise = clockwise;
  }

  /** Resets to the initial state. */
  public void reset() {
    this.mRotationAngle = 0;
    this.mIsScheduled = false;
    unscheduleSelf(this);
    invalidateSelf();
  }

  /** Define whether the rotation is clockwise or not. By default is the rotation clockwise. */
  public void setClockwise(boolean clockwise) {
    this.mClockwise = clockwise;
  }

  @Override
  public void draw(Canvas canvas) {
    int saveCount = canvas.save();
    Rect bounds = getBounds();
    float angle = this.mRotationAngle;
    rotateCanvas(canvas, bounds, angle);
    super.draw(canvas);
    canvas.restoreToCount(saveCount);
    scheduleNextFrame();
  }

  // rotates given canvas given the bounds of the canvas and desired angle
  private void rotateCanvas(Canvas canvas, Rect bounds, float angle) {
    int width = bounds.right - bounds.left;
    int height = bounds.bottom - bounds.top;
    if (!mClockwise) {
      angle = DEGREES_IN_FULL_ROTATION - this.mRotationAngle;
    }
    canvas.rotate(angle, bounds.left + width / 2, bounds.top + height / 2);
  }

  @Override
  public void run() {
    this.mIsScheduled = false;
    this.mRotationAngle += getIncrement();
    invalidateSelf();
  }

  @Override
  public AutoRotateDrawable cloneDrawable() {
    Drawable delegateCopy = DrawableUtils.cloneDrawable(getDrawable());
    return new AutoRotateDrawable(delegateCopy, this.mInterval, this.mClockwise);
  }

  /**
   * Schedule the next frame for drawing.
   *
   * <p>Ideally, we'd like to call this from the callback (i.e. {@code run()}), but if we do there's
   * no place where we can call scheduleNextFrame() for the first time. As a tradeoff, we call this
   * from draw(), which means scheduleNextFrame() could technically be called multiple times for the
   * same frame, so we must handle that gracefully.
   */
  private void scheduleNextFrame() {
    if (!mIsScheduled) {
      this.mIsScheduled = true;
      scheduleSelf(this, SystemClock.uptimeMillis() + FRAME_INTERVAL_MS);
    }
  }

  private int getIncrement() {
    return (int) (((float) FRAME_INTERVAL_MS) / this.mInterval * DEGREES_IN_FULL_ROTATION);
  }
}
