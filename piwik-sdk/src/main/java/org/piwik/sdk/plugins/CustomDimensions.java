package org.piwik.sdk.plugins;

import android.support.annotation.Size;

import org.piwik.sdk.Piwik;
import org.piwik.sdk.TrackMe;
import org.piwik.sdk.tools.Logy;

/**
 * This plugins allows you to track any Custom Dimensions.
 * In order to use this functionality install and configure the
 * <a href="https://plugins.piwik.org/CustomDimensions">Custom Dimensions plugin</a>.
 */
public class CustomDimensions extends TrackMe {
    protected static final String LOGGER_TAG = Piwik.LOGGER_PREFIX + "CustomDimensions";

    /**
     * Given {@code dimensionId = 1}, and {@code dimensionValue = "foo"}, this will add a tracking API parameter
     * {@code dimension1=foo}.
     * <br/>Given {@code dimensionId = 42}, and {@code dimensionValue = "bar"}, this will add a tracking API parameter
     * {@code dimension42=bar}.
     *
     * @param dimensionId    accepts values greater than 0
     * @param dimensionValue is limited to 255 characters
     * @return instance of CustomDimensions plugin
     */
    public synchronized CustomDimensions set(@Size(min = 1) int dimensionId, @Size(max = 255) String dimensionValue) {
        if (dimensionId < 1) {
            throw new IllegalArgumentException("dimensionId <" + dimensionId + "> should be great than 0");
        }
        if (dimensionValue != null && dimensionValue.length() > 255) {
            Logy.w(LOGGER_TAG, "dimensionValue will be truncated to 255 chars");
            dimensionValue = dimensionValue.substring(0, 255);
        }
        set("dimension" + dimensionId, dimensionValue);
        return this;
    }
}
