/*
 *  Copyright 2021 the original author, Lam Tong
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.github.easylock.server.handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * {@link BannerHandler} handles banner when application starts up by displaying
 * content of a <code>banner.txt</code> in classpath if exists or displaying default
 * banner.
 *
 * @author Lam Tong
 * @version 1.0.0
 * @since 1.0.0
 */
public final class BannerHandler {

    private static final Logger logger = Logger.getLogger(BannerHandler.class.getName());

    private static final String bannerFilename = "banner.txt";

    private static final URL url = BannerHandler.class.getClassLoader().getResource(bannerFilename);

    public void handleBanner() {
        if (url == null) {
            this.displayDefaultBanner();
        } else {
            this.displayBanner();
        }
    }

    private void displayDefaultBanner() {
        System.out.println("           _                                     _                             \n" +
                "         /' `\\                                 /~_)                      /'  _/\n" +
                "       /'   ._)                            ~-/'-~                      /' _/~  \n" +
                "      (___   ____     ____                 /'      ____     ____    ,/'_/~     \n" +
                "   _-~    `/'    )  /'    )--/'    /     /'      /'    )--/'    )--/\\/~        \n" +
                " /'      /'    /'  '---,   /'    /' /~\\,'   _  /'    /' /'       /'  \\         \n" +
                "(_____, (___,/(__(___,/   (___,/(__(,/'`\\____)(___,/'  (___,/  /'     \\        \n" +
                "                             /'                                                \n" +
                "                     /     /'                                                  \n" +
                "                    (___,/'                                                   \n" +
                "\n          Copyright 2021 the original author, Lam Tong, Version 1.0.0.\n");
    }

    private void displayBanner() {
        @SuppressWarnings("all")
        File file = new File(url.getPath());
        byte[] bytes = new byte[1024];
        int len;
        try (FileInputStream in = new FileInputStream(file)) {
            while ((len = in.read(bytes)) != -1) {
                System.out.println(new String(bytes, 0, len));
            }
        } catch (IOException e) {
            if (logger.isLoggable(Level.SEVERE)) {
                logger.log(Level.SEVERE, e.getMessage());
            }
        }
    }

}
