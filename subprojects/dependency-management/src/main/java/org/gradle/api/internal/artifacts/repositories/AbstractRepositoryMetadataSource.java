/*
 * Copyright 2017 the original author or authors.
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
package org.gradle.api.internal.artifacts.repositories;

import com.google.common.base.Joiner;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.component.ModuleComponentIdentifier;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ComponentResolvers;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.parser.DescriptorParseContext;
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.parser.MetaDataParseException;
import org.gradle.api.internal.artifacts.repositories.resolver.ExternalResourceArtifactResolver;
import org.gradle.api.internal.artifacts.repositories.resolver.ExternalResourceResolver;
import org.gradle.api.internal.artifacts.repositories.resolver.ExternalResourceResolverDescriptorParseContext;
import org.gradle.internal.SystemProperties;
import org.gradle.internal.component.external.model.ModuleComponentArtifactMetadata;
import org.gradle.internal.component.external.model.MutableModuleComponentResolveMetadata;
import org.gradle.internal.component.model.ComponentOverrideMetadata;
import org.gradle.internal.resolve.result.BuildableModuleComponentMetaDataResolveResult;
import org.gradle.internal.resolve.result.ResourceAwareResolveResult;
import org.gradle.internal.resource.local.FileResourceRepository;
import org.gradle.internal.resource.local.LocallyAvailableExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public abstract class AbstractRepositoryMetadataSource<S extends MutableModuleComponentResolveMetadata> extends AbstractMetadataSource<S> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalResourceResolver.class);

    private final MetadataArtifactProvider metadataArtifactProvider;
    private final FileResourceRepository fileResourceRepository;

    protected AbstractRepositoryMetadataSource(MetadataArtifactProvider metadataArtifactProvider,
                                               FileResourceRepository fileResourceRepository) {
        this.metadataArtifactProvider = metadataArtifactProvider;
        this.fileResourceRepository = fileResourceRepository;
    }

    @Override
    public S create(String repositoryName, ComponentResolvers componentResolvers, ModuleComponentIdentifier moduleVersionIdentifier, ComponentOverrideMetadata prescribedMetaData, ExternalResourceArtifactResolver artifactResolver, BuildableModuleComponentMetaDataResolveResult result) {
        S parsedMetadataFromRepository = parseMetaDataFromArtifact(repositoryName, componentResolvers, moduleVersionIdentifier, artifactResolver, result);
        if (parsedMetadataFromRepository != null) {
            LOGGER.debug("Metadata file found for module '{}' in repository '{}'.", moduleVersionIdentifier, repositoryName);
        }
        return parsedMetadataFromRepository;
    }

    @Nullable
    private S parseMetaDataFromArtifact(String repositoryName, ComponentResolvers componentResolvers, ModuleComponentIdentifier moduleComponentIdentifier, ExternalResourceArtifactResolver artifactResolver, ResourceAwareResolveResult result) {
        ModuleComponentArtifactMetadata artifact = metadataArtifactProvider.getMetaDataArtifactFor(moduleComponentIdentifier);
        LocallyAvailableExternalResource metadataArtifact = artifactResolver.resolveArtifact(artifact, result);
        S metaDataFromResource = null;
        if (metadataArtifact != null) {
            ExternalResourceResolverDescriptorParseContext context = new ExternalResourceResolverDescriptorParseContext(componentResolvers, fileResourceRepository);
            metaDataFromResource = parseMetaDataFromResource(moduleComponentIdentifier, metadataArtifact, artifactResolver, context, repositoryName);
        }
        return metaDataFromResource;
    }

    void checkMetadataConsistency(ModuleComponentIdentifier expectedId, MutableModuleComponentResolveMetadata metadata) throws MetaDataParseException {
        checkModuleIdentifier(expectedId, metadata.getId());
    }

    private void checkModuleIdentifier(ModuleComponentIdentifier expectedId, ModuleVersionIdentifier actualId) {
        List<String> errors = new ArrayList<String>();
        checkEquals("group", expectedId.getGroup(), actualId.getGroup(), errors);
        checkEquals("module name", expectedId.getModule(), actualId.getName(), errors);
        checkEquals("version", expectedId.getVersion(), actualId.getVersion(), errors);
        if (errors.size() > 0) {
            throw new MetaDataParseException(
                String.format("inconsistent module metadata found. Descriptor: %s Errors: %s", actualId, joinLines(errors)));
        }
    }

    private String joinLines(List<String> lines) {
        return Joiner.on(SystemProperties.getInstance().getLineSeparator()).join(lines);
    }

    private void checkEquals(String label, String expected, String actual, List<String> errors) {
        if (!expected.equals(actual)) {
            errors.add("bad " + label + ": expected='" + expected + "' found='" + actual + "'");
        }
    }

    protected abstract S parseMetaDataFromResource(ModuleComponentIdentifier moduleComponentIdentifier, LocallyAvailableExternalResource cachedResource, ExternalResourceArtifactResolver artifactResolver, DescriptorParseContext context, String repoName);

}
