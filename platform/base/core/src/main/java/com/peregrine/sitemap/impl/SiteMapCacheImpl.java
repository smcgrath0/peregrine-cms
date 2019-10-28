package com.peregrine.sitemap.impl;

/*-
 * #%L
 * platform base - Core
 * %%
 * Copyright (C) 2017 headwire inc.
 * %%
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * #L%
 */

import com.peregrine.sitemap.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.*;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.*;

import static com.peregrine.commons.util.PerConstants.SLASH;
import static com.peregrine.commons.util.PerConstants.SLING_FOLDER;

@Component(service = SiteMapCache.class)
@Designate(ocd = SiteMapCacheImplConfig.class)
public final class SiteMapCacheImpl implements SiteMapCache {

    private final Map<String, Object> authenticationInfo = new HashMap<>();

    @Reference
    private SiteMapExtractorsContainer siteMapExtractorsContainer;

    @Reference
    private SiteMapBuilder siteMapBuilder;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private int maxEntriesCount;
    private int maxFileSize;
    private String location;

    @Activate
    public void activate(final SiteMapCacheImplConfig config) {
        authenticationInfo.put(ResourceResolverFactory.SUBSERVICE, config.sling_service_subservice());

        maxEntriesCount = config.maxEntriesCount();
        if (maxEntriesCount <= 0) {
            maxEntriesCount = Integer.MAX_VALUE;
        }

        maxFileSize = config.maxFileSize();
        if (maxFileSize <= 0) {
            maxFileSize = Integer.MAX_VALUE;
        }

        location = config.location();
    }

    @Override
    public String get(final Resource root, final int index, final SiteMapUrlBuilder siteMapUrlBuilder) {
        try (final ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(authenticationInfo)) {
            final String path = location + root.getPath();
            final Resource resource = getOrCreateCacheResource(resourceResolver, path);
            if (resource == null) {
                return null;
            }

            final ValueMap properties = resource.getValueMap();
            final String key = Integer.toString(index);
            if (!properties.containsKey(key)) {
                final SiteMapExtractor extractor = siteMapExtractorsContainer.findFirstFor(root);
                if (extractor == null) {
                    return null;
                }

                final Collection<SiteMapEntry> entries = extractor.extract(root);
                final LinkedList<List<SiteMapEntry>> splitEntries = splitEntries(entries);
                final ArrayList<String> strings = new ArrayList<>();
                final int numberOfParts = splitEntries.size();
                if (numberOfParts > 1) {
                    final SiteMapUrlBuilder shortUrlBuilder = extractor.getSiteMapUrlBuilder(resourceResolver, siteMapUrlBuilder);
                    strings.add(siteMapBuilder.buildSiteMapIndex(root, shortUrlBuilder, numberOfParts));
                }

                for (final List<SiteMapEntry> list : splitEntries) {
                    strings.add(siteMapBuilder.buildUrlSet(list));
                }

                final ModifiableValueMap modifiableValueMap = resource.adaptTo(ModifiableValueMap.class);
                for (int i = 0; i < strings.size(); i++) {
                    modifiableValueMap.put(Integer.toString(i), strings.get(i));
                }

                resourceResolver.commit();
            }

            return properties.get(key, String.class);
        } catch (final LoginException | RepositoryException | PersistenceException e) {
            return null;
        }
    }

    private Resource getOrCreateCacheResource(final ResourceResolver resourceResolver, final String path) throws RepositoryException {
        String existingPath = path;
        Resource resource = null;
        final List<String> missing = new ArrayList<>();
        while (StringUtils.isNotBlank(existingPath) &&
                (resource = resourceResolver.getResource(existingPath)) == null) {
            missing.add(0, StringUtils.substringAfterLast(existingPath, SLASH));
            existingPath = StringUtils.substringBeforeLast(existingPath, SLASH);
        }

        if (resource == null) {
            resource = resourceResolver.getResource(SLASH);
        }

        Node node = resource.adaptTo(Node.class);
        for (final String name : missing) {
            node = node.addNode(name, SLING_FOLDER);
        }

        node.getSession().save();
        return resourceResolver.getResource(node.getPath());
    }

    private LinkedList<List<SiteMapEntry>> splitEntries(final Collection<SiteMapEntry> entries) {
        final LinkedList<List<SiteMapEntry>> result = new LinkedList<>();
        int index = 0;
        int size = siteMapBuilder.getBaseSiteMapLength();
        List<SiteMapEntry> split = new LinkedList<>();
        result.add(split);
        for (final SiteMapEntry entry : entries) {
            final int entrySize = siteMapBuilder.getSize(entry);
            if (index < maxEntriesCount && size + entrySize <= maxFileSize) {
                split.add(entry);
                index++;
                size += entrySize;
            } else {
                index = 0;
                size = siteMapBuilder.getBaseSiteMapLength();
                split = new LinkedList<>();
                result.add(split);
            }
        }

        return result;
    }
}
