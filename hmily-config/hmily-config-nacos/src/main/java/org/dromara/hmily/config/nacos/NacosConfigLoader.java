/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.dromara.hmily.config.nacos;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.dromara.hmily.common.utils.StringUtils;
import org.dromara.hmily.config.api.Config;
import org.dromara.hmily.config.api.exception.ConfigException;
import org.dromara.hmily.config.loader.ConfigLoader;
import org.dromara.hmily.config.loader.PropertyLoader;
import org.dromara.hmily.config.loader.properties.PropertiesLoader;
import org.dromara.hmily.config.loader.yaml.YamlPropertyLoader;
import org.dromara.hmily.spi.HmilySPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Nacos config loader.
 *
 * @author xiaoyu
 */
@HmilySPI("nacos")
public class NacosConfigLoader implements ConfigLoader<NacosConfig> {

    private static final Logger LOGGER = LoggerFactory.getLogger(NacosConfigLoader.class);
    
    private static final Map<String, PropertyLoader> LOADERS = new HashMap<>();
    
    private NacosClient client = new NacosClient();
    
    static {
        LOADERS.put("yml", new YamlPropertyLoader());
        LOADERS.put("properties", new PropertiesLoader());
    }
    
    /**
     * Instantiates a new Nacos config loader.
     */
    public NacosConfigLoader() {
    }
    
    /**
     * Instantiates a new Nacos config loader.
     *
     * @param client the client
     */
    public NacosConfigLoader(final NacosClient client) {
        this();
        this.client = client;
    }

    @Override
    public void load(final Supplier<Context> context, final LoaderHandler<NacosConfig> handler) {
        LoaderHandler<NacosConfig> nacosHandler = (c, config) -> nacosLoad(c, handler, config);
        againLoad(context, nacosHandler, NacosConfig.class);
    }

    private void nacosLoad(final Supplier<Context> context, final LoaderHandler<NacosConfig> handler, final NacosConfig config) {
        if (config != null) {
            check(config);
            LOGGER.info("loader nacos config: {}", config);
            String fileExtension = config.getFileExtension();
            PropertyLoader propertyLoader = LOADERS.get(fileExtension);
            if (propertyLoader == null) {
                throw new ConfigException("nacos.fileExtension setting error, The loader was not found");
            }
            InputStream pull = client.pull(config);
            Optional.ofNullable(pull)
                    .map(e -> propertyLoader.load("remote.nacos." + fileExtension, e))
                    .ifPresent(e -> context.get().getOriginal().load(() -> context.get().withSources(e), this::nacosFinish));
            handler.finish(context, config);
        } else {
            throw new ConfigException("nacos config is null");
        }
    }

    private void nacosFinish(final Supplier<Context> context, final Config config) {
        LOGGER.info("nacos loader config {}:{}", config != null ? config.prefix() : "", config);
    }

    private void check(final NacosConfig config) {
        if (StringUtils.isBlank(config.getServer())) {
            throw new ConfigException("nacos.server is null");
        }
        if (StringUtils.isBlank(config.getDataId())) {
            throw new ConfigException("nacos.dataId is null");
        }
        if (StringUtils.isBlank(config.getFileExtension())) {
            throw new ConfigException("nacos.fileExtension is null");
        }
        if (StringUtils.isBlank(config.getGroup())) {
            throw new ConfigException("nacos.group is null");
        }
    }
}
