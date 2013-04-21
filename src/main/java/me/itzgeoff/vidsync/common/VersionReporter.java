/**
 * Copyright 2013 Geoff Bourne
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package me.itzgeoff.vidsync.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * @author Geoff
 *
 */
@Component
public class VersionReporter {

    private static final Logger logger = LoggerFactory.getLogger(VersionReporter.class);
    private String ourVersion;
    
    @PostConstruct
    public void reportVersion() {
        Properties versionProps = new Properties();
        try (InputStream inStream = VersionReporter.class.getResourceAsStream("/META-INF/maven/me.itzgeoff/vidsync/pom.properties")) {
            if (inStream != null) {
                versionProps.load(inStream);
                ourVersion = versionProps.getProperty("version");
                logger.info("Running version {}", ourVersion);
            }
            else {
                logger.error("Unable to located pom.properties");
            }
        } catch (IOException e) {
            logger.error("Unable to load project version", e);
        }
    }
    
    @Bean
    public String ourVersion() {
        return ourVersion;
    }
}
