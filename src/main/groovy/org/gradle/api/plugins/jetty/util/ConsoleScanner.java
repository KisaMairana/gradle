/*
 * Copyright 2009 the original author or authors.
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

/**
 *
 */
package org.gradle.api.plugins.jetty.util;

import java.io.IOException;

import org.gradle.api.plugins.jetty.AbstractJettyRunTask;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleScanner extends Thread {
    private static Logger logger = LoggerFactory.getLogger(ConsoleScanner.class);

    private final AbstractJettyRunTask task;

    public ConsoleScanner(AbstractJettyRunTask task) {
        this.task = task;
        setName("Console scanner");
        setDaemon(true);
    }

    public void run() {
        try {
            while (true) {
                checkSystemInput();
                getSomeSleep();
            }
        }
        catch (IOException e) {
            logger.warn("Error when checking console input.", e);
        }
    }

    private void getSomeSleep() {
        try {
            Thread.sleep(500);
        }
        catch (InterruptedException e) {
            logger.debug("Error while sleeping.", e);
        }
    }

    private void checkSystemInput() throws IOException {
        while (System.in.available() > 0) {
            int inputByte = System.in.read();
            if (inputByte >= 0) {
                char c = (char) inputByte;
                if (c == '\n') {
                    restartWebApp();
                }
            }
        }
    }


    /**
     * Skip buffered bytes of system console.
     */
    private void clearInputBuffer() {
        try {
            while (System.in.available() > 0) {
                // System.in.skip doesn't work properly. I don't know why
                long available = System.in.available();
                for (int i = 0; i < available; i++) {
                    if (System.in.read() == -1) {
                        break;
                    }
                }
            }
        }
        catch (IOException e) {
            logger.warn("Error discarding console input buffer", e);
        }
    }

    private void restartWebApp() {
        try {
            task.restartWebApp(false);
            // Clear input buffer to discard anything entered on the console
            // while the application was being restarted.
            clearInputBuffer();
        }
        catch (Exception e) {
            logger.error(
                    "Error reconfiguring/restarting webapp after a new line on the console",
                    e);
        }
    }
}