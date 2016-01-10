package org.piwik.sdk.dispatcher;

// FIXME javadoc this correctly

import org.piwik.sdk.storage.StorageEngine;

/**
 * The dryRun flag set to true prevents any data from being sent to Piwik.
 * The dryRun flag should be set whenever you are testing or debugging an implementation and do not want
 * test data to appear in your Piwik reports. To set the dry run flag, use:
 *
 * @param isDryRun true if you don't want to send any data to piwik
 */
public class DryRunDispatcher implements Dispatcher {

    @Override
    public void dispatch(StorageEngine storageEngine) {

    }
}
