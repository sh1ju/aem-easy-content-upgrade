/*
 * Copyright 2018 Valtech GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package de.valtech.aecu.core.installhook;

import de.valtech.aecu.service.AecuService;

import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

/**
 * Collects groovy script paths to potentially execute based on the given actions.
 */
public class AecuTrackerListener implements ProgressTrackerListener {

    private static final Logger LOG = LoggerFactory.getLogger(AecuTrackerListener.class);

    private static final Set<String> ACTIONS = new HashSet<>(Arrays.asList("A", "M", "U"));

    private static final int VALID_ACTION_LENGTH = 1;

    private static final String LOG_PREFIX = "AECU InstallHook: ";

    private final ProgressTrackerListener originalListener;
    private final AecuService aecuService;
    private final List<String> paths;

    /**
     * Constructor.
     * 
     * @param originalListener the original ProgressTrackerListener.
     * @param aecuService      an AecuService instance.
     */
    public AecuTrackerListener(ProgressTrackerListener originalListener, AecuService aecuService) {
        this.originalListener = originalListener;
        this.aecuService = aecuService;
        this.paths = new LinkedList<>();
        logMessage("Starting install hook...");
    }

    /**
     * Returns an unmodifiable list of the modified or added paths encountered during the
     * installation phase.
     * 
     * @return a list of modified or added paths, can be empty.
     */
    @Nonnull
    public List<String> getModifiedOrAddedPaths() {
        return Collections.unmodifiableList(paths);
    }

    @Override
    public void onMessage(Mode mode, String action, String path) {
        originalListener.onMessage(mode, action, path);

        if (StringUtils.length(action) != VALID_ACTION_LENGTH) {
            // skip actions like 'Collecting import information... ', 'Package imported.' etc.
            return;
        }

        if (StringUtils.endsWith(path, "always.groovy")) {
            logMessage(String.format("Adding %s due to having 'always' in name.", path));
            paths.add(path);
            return;
        }

        if (!ACTIONS.contains(action)) {
            logMessage(String.format("Skipping %s due to non matching action '%s'", path, action));
            return;
        }

        // in case a script was updated the update will actually be shown on jcr:content and not on
        // the groovy script node
        if (StringUtils.endsWith(path, "/jcr:content")) {
            path = StringUtils.substringBefore(path, "/jcr:content");
        }

        if (aecuService.isValidScriptName(path)) {
            logMessage(String.format("Found valid script path '%s'", path));
            paths.add(path);
        }
    }

    @Override
    public void onError(Mode mode, String action, Exception e) {
        originalListener.onError(mode, action, e);
    }

    public void logMessage(String message) {
        onMessage(Mode.TEXT, LOG_PREFIX + message, "");
    }

    public void logError(String message, Exception e) {
        onError(Mode.TEXT, LOG_PREFIX + message, e);
    }
}
