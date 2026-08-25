/* Copyright (c) 2026 The Brave Authors. All rights reserved.
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at https://mozilla.org/MPL/2.0/. */

package org.chromium.chrome.browser.ntp;

import static org.junit.Assert.assertEquals;

import android.graphics.RectF;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;

import org.chromium.base.test.BaseRobolectricTestRunner;

@RunWith(BaseRobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class NewTabTakeoverSafeAreaReporterTest {
    @Test
    public void spansTheGapBetweenTheTopCardsAndTheImageCredit() {
        assertEquals(
                new RectF(0, 600, 1080, 1500),
                NewTabTakeoverSafeAreaReporter.calculateSafeArea(
                        /* safeAreaTop= */ 600,
                        /* safeAreaBottom= */ 1500,
                        /* viewportWidth= */ 1080,
                        /* viewportHeight= */ 1920,
                        /* density= */ 1f));
    }

    @Test
    public void isEmptyWhenTheCardsLeaveNoVerticalGap() {
        RectF safeArea =
                NewTabTakeoverSafeAreaReporter.calculateSafeArea(
                        /* safeAreaTop= */ 900,
                        /* safeAreaBottom= */ 800,
                        /* viewportWidth= */ 1080,
                        /* viewportHeight= */ 1920,
                        /* density= */ 1f);

        assertEquals(0f, safeArea.height(), 0f);
    }

    @Test
    public void clampsTheTopCardsToTheViewportBottom() {
        assertEquals(
                new RectF(0, 1920, 1080, 1920),
                NewTabTakeoverSafeAreaReporter.calculateSafeArea(
                        /* safeAreaTop= */ 2400,
                        /* safeAreaBottom= */ 2400,
                        /* viewportWidth= */ 1080,
                        /* viewportHeight= */ 1920,
                        /* density= */ 1f));
    }

    @Test
    public void clampsTheImageCreditToTheViewportBottom() {
        assertEquals(
                new RectF(0, 600, 1080, 1920),
                NewTabTakeoverSafeAreaReporter.calculateSafeArea(
                        /* safeAreaTop= */ 600,
                        /* safeAreaBottom= */ 2400,
                        /* viewportWidth= */ 1080,
                        /* viewportHeight= */ 1920,
                        /* density= */ 1f));
    }

    @Test
    public void convertsViewPixelsToCssPixels() {
        assertEquals(
                new RectF(0, 300, 540, 750),
                NewTabTakeoverSafeAreaReporter.calculateSafeArea(
                        /* safeAreaTop= */ 600,
                        /* safeAreaBottom= */ 1500,
                        /* viewportWidth= */ 1080,
                        /* viewportHeight= */ 1920,
                        /* density= */ 2f));

        RectF safeArea =
                NewTabTakeoverSafeAreaReporter.calculateSafeArea(
                        /* safeAreaTop= */ 600,
                        /* safeAreaBottom= */ 1500,
                        /* viewportWidth= */ 1080,
                        /* viewportHeight= */ 1920,
                        /* density= */ 2.625f);

        assertEquals(0f, safeArea.left, 0f);
        assertEquals(228.571f, safeArea.top, 0.001f);
        assertEquals(411.429f, safeArea.right, 0.001f);
        assertEquals(571.429f, safeArea.bottom, 0.001f);
    }
}
