package com.android.launcher3.folder;

import com.android.launcher3.Flags;

public class ClippedFolderIconLayoutRule {

    /**
     * LC: Number of icons shown in the closed folder preview, 4 (2x2) or 9 (3x3). Set from the
     * preferences through {@link #setMaxNumItemsInPreview(int)}.
     */
    public static volatile int MAX_NUM_ITEMS_IN_PREVIEW = 4;
    private static final int MIN_NUM_ITEMS_IN_PREVIEW = 2;
    // Up to this many items, the preview lays them out in a circle.
    private static final int MAX_NUM_ITEMS_IN_CIRCLE = 4;

    public static final float MIN_SCALE = 0.44f;
    public static final float MAX_SCALE = 0.51f;
    // LC: Scale and spacing of the items in the 3x3 preview.
    private static final float GRID_3X3_SCALE = 0.27f;
    private static final float GRID_3X3_SPACING = 0.28f;

    // TODO: figure out exact radius for different icons
    private static final float MAX_RADIUS_DILATION = 0.25f;
    // The max amount of overlap the preview items can go outside of the background bounds.
    public static final float ICON_OVERLAP_FACTOR = 1 + (MAX_RADIUS_DILATION / 2f);
    private static final float ITEM_RADIUS_SCALE_FACTOR = 1.15f;
    private static final float ITEM_RADIUS_SCALE_FACTOR_SHAPES = 1.2f;

    public static final int EXIT_INDEX = -2;
    public static final int ENTER_INDEX = -3;

    private float[] mTmpPoint = new float[2];

    private float mAvailableSpace;
    private float mRadius;
    private float mIconSize;
    private boolean mIsRtl;
    private float mBaselineIconScale;
    private int mNumFolderColumns;

    /** LC: Sets how many icons the closed folder preview shows, 4 (2x2) or 9 (3x3). */
    public static void setMaxNumItemsInPreview(int maxNumItems) {
        MAX_NUM_ITEMS_IN_PREVIEW = maxNumItems > MAX_NUM_ITEMS_IN_CIRCLE ? 9 : 4;
    }

    /** LC: Number of rows and columns of the closed folder preview. */
    public static int getPreviewGridSize() {
        return MAX_NUM_ITEMS_IN_PREVIEW > MAX_NUM_ITEMS_IN_CIRCLE ? 3 : 2;
    }

    /**
     * initialize the layout rule
     */
    public void init(int availableSpace, float intrinsicIconSize, boolean rtl,
            int numFolderColumns) {
        mAvailableSpace = availableSpace;
        mRadius = (
                Flags.enableLauncherIconShapes()
                        ? ITEM_RADIUS_SCALE_FACTOR_SHAPES
                        : ITEM_RADIUS_SCALE_FACTOR) * availableSpace / 2f;
        mIconSize = intrinsicIconSize;
        mIsRtl = rtl;
        mBaselineIconScale = availableSpace / intrinsicIconSize;
        mNumFolderColumns = numFolderColumns;
    }

    /**
     * Computes positions for icons in Preview.
     *
     * @param index       index of icon in folder
     * @param curNumItems current number of preview items
     * @param params      params to update for icon
     */
    public PreviewItemDrawingParams computePreviewItemDrawingParams(int index, int curNumItems,
            PreviewItemDrawingParams params) {
        float totalScale = scaleForItem(curNumItems, 0);
        float transX;
        float transY;

        if (index == EXIT_INDEX) {
            // 0 1 * <-- Exit position (row 0, col 2)
            // 2 3
            getGridPosition(0, getPreviewGridSize(), mTmpPoint);
        } else if (index == ENTER_INDEX) {
            // 0 1
            // 2 3 * <-- Enter position (row 1, col 2)
            getGridPosition(1, getPreviewGridSize(), mTmpPoint);
        } else if (index >= MAX_NUM_ITEMS_IN_PREVIEW) {
            // Items beyond those displayed in the preview are animated to the center
            mTmpPoint[0] = mTmpPoint[1] = mAvailableSpace / 2 - (mIconSize * totalScale) / 2;
        } else {
            getPosition(index, curNumItems, mTmpPoint);
        }

        transX = mTmpPoint[0];
        transY = mTmpPoint[1];

        if (params == null) {
            params = new PreviewItemDrawingParams(transX, transY, totalScale);
        } else {
            params.update(transX, transY, totalScale);
        }
        return params;
    }

    /**
     * Computes positions for icons in folder as part of spring animation. Here both preview icons
     * and the rest of the content icons are animated along the same grid.
     *
     * @param index          index of icon in folder
     * @param numItemsInPage current number of items in page
     * @param params         params to update for icon
     */
    public PreviewItemDrawingParams computeSpringAnimationItemParams(int index, int numItemsInPage,
            int page, PreviewItemDrawingParams params) {
        float totalScale = scaleForItem(numItemsInPage, page);
        float transX;
        float transY;

        if (numItemsInPage <= MAX_NUM_ITEMS_IN_PREVIEW) {
            getPosition(index, numItemsInPage, mTmpPoint);
        } else {
            getGridPosition(index / mNumFolderColumns, index % mNumFolderColumns, mTmpPoint);
        }

        transX = mTmpPoint[0];
        transY = mTmpPoint[1];

        if (params == null) {
            params = new PreviewItemDrawingParams(transX, transY, totalScale);
        } else {
            params.update(transX, transY, totalScale);
        }
        return params;
    }


    /**
     * Builds a grid based on the positioning of the items when there are
     * {@link #MAX_NUM_ITEMS_IN_PREVIEW} in the preview.
     *
     * Positions in the grid: 0 1  // 0 is row 0, col 1
     * 2 3  // 3 is row 1, col 1`
     */
    private void getGridPosition(int row, int col, float[] result) {
        if (getPreviewGridSize() == 3) {
            getGridPosition3x3(row, col, result);
            return;
        }
        // We use position 0 and 3 to calculate the x and y distances between items.
        getPosition(0, 4, result);
        float left = result[0];
        float top = result[1];

        getPosition(3, 4, result);
        float dx = result[0] - left;
        float dy = result[1] - top;

        result[0] = left + (col * dx);
        result[1] = top + (row * dy);
    }

    /** LC: Position of the item at the given row and column of the 3x3 preview grid. */
    private void getGridPosition3x3(int row, int col, float[] result) {
        float spacing = mAvailableSpace * GRID_3X3_SPACING;
        float iconSize = mIconSize * scaleForItem(MAX_NUM_ITEMS_IN_PREVIEW, 0);
        float start = (mAvailableSpace - 2 * spacing - iconSize) / 2;
        result[0] = start + (mIsRtl ? 2 - col : col) * spacing;
        result[1] = start + row * spacing;
    }

    private void getPosition(int index, int curNumItems, float[] result) {
        if (curNumItems > MAX_NUM_ITEMS_IN_CIRCLE) {
            // LC: More items than fit the circle are laid out in reading order on the 3x3 grid,
            // vertically centered when they don't fill it.
            int numRows = (curNumItems + 2) / 3;
            getGridPosition3x3(index / 3, index % 3, result);
            result[1] += (3 - numRows) * mAvailableSpace * GRID_3X3_SPACING / 2;
            return;
        }
        // The case of two items is homomorphic to the case of one.
        curNumItems = Math.max(curNumItems, 2);

        // We model the preview as a circle of items starting in the appropriate piece of the
        // upper left quadrant (to achieve horizontal and vertical symmetry).
        double theta0 = mIsRtl ? 0 : Math.PI;

        // In RTL we go counterclockwise
        int direction = mIsRtl ? 1 : -1;

        double thetaShift = 0;
        if (curNumItems == 3) {
            thetaShift = Math.PI / 2;
        } else if (curNumItems == 4) {
            thetaShift = Math.PI / 4;
        }
        theta0 += direction * thetaShift;

        // We want the items to appear in reading order. For the case of 1, 2 and 3 items, this
        // is natural for the circular model. With 4 items, however, we need to swap the 3rd and
        // 4th indices to achieve reading order.
        if (curNumItems == 4 && index == 3) {
            index = 2;
        } else if (curNumItems == 4 && index == 2) {
            index = 3;
        }

        float radius = getRadius(curNumItems);
        double theta = theta0 + index * (2 * Math.PI / curNumItems) * direction;
        float halfIconSize = (mIconSize * scaleForItem(curNumItems, 0)) / 2;

        // Map the location along the circle, and offset the coordinates to represent the center
        // of the icon, and to be based from the top / left of the preview area. The y component
        // is inverted to match the coordinate system.
        result[0] = mAvailableSpace / 2 + (float) (radius * Math.cos(theta) / 2) - halfIconSize;
        result[1] = mAvailableSpace / 2 + (float) (-radius * Math.sin(theta) / 2) - halfIconSize;
    }

    private float getRadius(int numItems) {
        float radiusDilation = Flags.enableLauncherIconShapes() ? radiusDilationForItems(numItems)
                : MAX_RADIUS_DILATION;
        if (Flags.enableLauncherIconShapes()) {
            // Just give custom radius weights for each # of icons.
            return mRadius * (1 + radiusDilation);
        } else {
            // Increase radius from 0 up to MAX_RADIUS_DILATION as the number of items increases.
            return mRadius * (1 + radiusDilation * (numItems - MIN_NUM_ITEMS_IN_PREVIEW)
                    / (MAX_NUM_ITEMS_IN_CIRCLE - MIN_NUM_ITEMS_IN_PREVIEW));
        }
    }

    /**
     * Calculate Scale for Preview Icons based on current page and number of items in page.
     * @param numItems number of items in page
     * @param page current page of Folder
     * @return scale for icons in Folder
     */
    public float scaleForItem(int numItems, int page) {
        float scale;
        if (getPreviewGridSize() == 3 && (page > 0 || numItems > MAX_NUM_ITEMS_IN_CIRCLE)) {
            scale = GRID_3X3_SCALE;
        } else if (page > 0) {
            scale = MIN_SCALE;
        } else if (numItems <= 3) {
            scale = MAX_SCALE;
        } else {
            scale = MIN_SCALE;
        }
        return scale * mBaselineIconScale;
    }

    private float radiusDilationForItems(int numItems) {
        if (numItems == 3) {
            return 0.15f;
        } else if (numItems == MAX_NUM_ITEMS_IN_CIRCLE) {
            return 0.12f;
        } else {
            return 0;
        }
    }

    public float getIconSize() {
        return mIconSize;
    }
}
