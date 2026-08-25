/* Copyright (c) 2026 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/. */

package org.chromium.chrome.browser.ntp;

import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import androidx.annotation.VisibleForTesting;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.chromium.build.annotations.NullMarked;
import org.chromium.build.annotations.Nullable;

/**
 * Observes the New Tab Page, measures the region that is free of native browser UI and reports it
 * in CSS pixels to the sponsored rich media WebView.
 */
@NullMarked
public class NewTabTakeoverSafeAreaReporter extends RecyclerView.OnScrollListener
        implements ViewTreeObserver.OnGlobalLayoutListener {
    // Time interval to debounce multiple measurements. Only one measurement is posted within this
    // interval.
    private static final long MEASUREMENT_DELAY_MS = 120;

    private final BraveNewTabPageLayout mNtpLayout;
    private final RecyclerView mRecyclerView;
    private final BraveNtpAdapter mNtpAdapter;
    private final FrameLayout mRichMediaContainer;
    private final SponsoredRichMediaWebView mRichMediaWebView;

    private @Nullable Handler mMeasurementHandler = new Handler(Looper.getMainLooper());

    public NewTabTakeoverSafeAreaReporter(
            BraveNewTabPageLayout ntpLayout,
            RecyclerView recyclerView,
            BraveNtpAdapter ntpAdapter,
            FrameLayout richMediaContainer,
            SponsoredRichMediaWebView richMediaWebView) {
        mNtpLayout = ntpLayout;
        mRecyclerView = recyclerView;
        mNtpAdapter = ntpAdapter;
        mRichMediaContainer = richMediaContainer;
        mRichMediaWebView = richMediaWebView;

        mNtpLayout.getViewTreeObserver().addOnGlobalLayoutListener(this);
        mRecyclerView.addOnScrollListener(this);

        scheduleMeasurement();
    }

    public void destroy() {
        if (mMeasurementHandler != null) {
            mMeasurementHandler.removeCallbacksAndMessages(null);
            mMeasurementHandler = null;
        }

        mRecyclerView.removeOnScrollListener(this);
        mNtpLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
    }

    public void scheduleMeasurement() {
        if (mMeasurementHandler == null) {
            return;
        }

        mMeasurementHandler.removeCallbacksAndMessages(null);
        mMeasurementHandler.postDelayed(this::measureAndReportSafeArea, MEASUREMENT_DELAY_MS);
    }

    /** Covers rotation and showing or hiding Brave Stats, top sites or Brave News. */
    @Override
    public void onGlobalLayout() {
        scheduleMeasurement();
    }

    @Override
    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
        // If a layout change landed while scrolled, it can only be measured once the New
        // Tab Page scroll settles back to its initial position.
        if (newState == RecyclerView.SCROLL_STATE_IDLE) {
            scheduleMeasurement();
        }
    }

    private void measureAndReportSafeArea() {
        LinearLayoutManager layoutManager = getSettledLayoutManager();
        if (layoutManager == null) {
            return;
        }

        final int viewportWidth = mRichMediaContainer.getWidth();
        final int viewportHeight = mRichMediaContainer.getHeight();
        if (viewportWidth == 0 || viewportHeight == 0) {
            return;
        }

        final int imageCreditPosition = mNtpAdapter.getImageCreditPosition();
        mRichMediaWebView.setSafeArea(
                calculateSafeArea(
                        getSafeAreaTop(layoutManager, imageCreditPosition),
                        getSafeAreaBottom(layoutManager, imageCreditPosition, viewportHeight),
                        viewportWidth,
                        viewportHeight,
                        mNtpLayout.getResources().getDisplayMetrics().density));
    }

    private @Nullable LinearLayoutManager getSettledLayoutManager() {
        // The safe area is calculated for the unscrolled New Tab Page, so skip calculation unless
        // the scroll is in its initial position.
        if (mRecyclerView.computeVerticalScrollOffset() != 0) {
            return null;
        }

        if (!(mRecyclerView.getLayoutManager() instanceof LinearLayoutManager layoutManager)
                || layoutManager.findFirstVisibleItemPosition() == RecyclerView.NO_POSITION) {
            return null;
        }
        return layoutManager;
    }

    private int getSafeAreaTop(LinearLayoutManager layoutManager, int imageCreditPosition) {
        int safeAreaTop = 0;
        for (int position = 0; position < imageCreditPosition; position++) {
            View contentView = layoutManager.findViewByPosition(position);
            if (contentView != null) {
                safeAreaTop =
                        Math.max(
                                safeAreaTop,
                                getRichMediaContainerY(contentView, contentView.getHeight()));
            }
        }
        return safeAreaTop;
    }

    private int getSafeAreaBottom(
            LinearLayoutManager layoutManager, int imageCreditPosition, int viewportHeight) {
        View imageCreditView = layoutManager.findViewByPosition(imageCreditPosition);
        if (imageCreditView != null && imageCreditView.getHeight() > 0) {
            return getRichMediaContainerY(imageCreditView, 0);
        }

        View belowImageCreditView = layoutManager.findViewByPosition(imageCreditPosition + 1);
        return belowImageCreditView != null
                ? getRichMediaContainerY(belowImageCreditView, 0)
                : viewportHeight;
    }

    private int getRichMediaContainerY(View view, int yInView) {
        Rect rect = new Rect(0, yInView, 0, yInView);
        mNtpLayout.offsetDescendantRectToMyCoords(view, rect);
        return rect.top - mRichMediaContainer.getTop();
    }

    /**
     * Returns the region between `safeAreaTop` and `safeAreaBottom` in CSS pixels clamped to the
     * viewport.
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    static RectF calculateSafeArea(
            int safeAreaTop,
            int safeAreaBottom,
            int viewportWidth,
            int viewportHeight,
            float density) {
        final float top = Math.min(safeAreaTop, viewportHeight);
        final float bottom = Math.min(safeAreaBottom, viewportHeight);

        return new RectF(
                0, top / density, viewportWidth / density, Math.max(top, bottom) / density);
    }
}
