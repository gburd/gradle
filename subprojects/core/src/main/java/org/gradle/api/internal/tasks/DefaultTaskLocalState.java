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

package org.gradle.api.internal.tasks;

import com.google.common.collect.Lists;
import org.gradle.api.NonNullApi;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.TaskInternal;
import org.gradle.api.internal.file.FileResolver;
import org.gradle.api.internal.tasks.properties.GetLocalStateVisitor;
import org.gradle.api.internal.tasks.properties.PropertyVisitor;
import org.gradle.api.internal.tasks.properties.PropertyWalker;

import java.util.Collections;
import java.util.List;

@NonNullApi
public class DefaultTaskLocalState implements TaskLocalStateInternal {
    private final FileResolver resolver;
    private final TaskInternal task;
    private final TaskMutator taskMutator;
    private final PropertyWalker propertyWalker;
    private final PropertySpecFactory specFactory;
    private final List<Object> registeredPaths = Lists.newArrayList();

    public DefaultTaskLocalState(FileResolver resolver, TaskInternal task, TaskMutator taskMutator, PropertyWalker propertyWalker, PropertySpecFactory specFactory) {
        this.resolver = resolver;
        this.task = task;
        this.taskMutator = taskMutator;
        this.propertyWalker = propertyWalker;
        this.specFactory = specFactory;
    }

    @Override
    public void register(final Object... paths) {
        taskMutator.mutate("TaskLocalState.register(Object...)", new Runnable() {
            @Override
            public void run() {
                Collections.addAll(DefaultTaskLocalState.this.registeredPaths, paths);
            }
        });
    }

    @Override
    public void visitRegisteredProperties(PropertyVisitor visitor) {
        for (Object path : registeredPaths) {
            visitor.visitLocalStateProperty(path);
        }
    }

    private void visitAllProperties(PropertyVisitor visitor) {
        propertyWalker.visitProperties(specFactory, visitor, task);
        visitRegisteredProperties(visitor);
    }

    @Override
    public FileCollection getFiles() {
        GetLocalStateVisitor visitor = new GetLocalStateVisitor(task.toString(), resolver);
        visitAllProperties(visitor);
        return visitor.getFiles();
    }

}
