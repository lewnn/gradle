/*
 * Copyright 2010 the original author or authors.
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

package org.gradle.initialization;

import com.google.common.collect.ImmutableList;
import org.gradle.StartParameter;
import org.gradle.api.internal.ExceptionAnalyser;
import org.gradle.api.internal.GradleInternal;
import org.gradle.api.internal.tasks.cache.TaskExecutionStatisticsEventAdapter;
import org.gradle.api.logging.Logging;
import org.gradle.api.logging.StandardOutputListener;
import org.gradle.api.logging.configuration.ShowStacktrace;
import org.gradle.configuration.BuildConfigurer;
import org.gradle.deployment.internal.DeploymentRegistry;
import org.gradle.execution.BuildConfigurationActionExecuter;
import org.gradle.execution.BuildExecuter;
import org.gradle.internal.buildevents.BuildLogger;
import org.gradle.internal.buildevents.CacheStatisticsReporter;
import org.gradle.internal.buildevents.TaskExecutionLogger;
import org.gradle.internal.classpath.ClassPath;
import org.gradle.internal.concurrent.Stoppable;
import org.gradle.internal.event.ListenerManager;
import org.gradle.internal.featurelifecycle.LoggingDeprecatedFeatureHandler;
import org.gradle.internal.featurelifecycle.ScriptUsageLocationReporter;
import org.gradle.internal.logging.LoggingManagerInternal;
import org.gradle.internal.logging.progress.ProgressLoggerFactory;
import org.gradle.internal.logging.text.StyledTextOutputFactory;
import org.gradle.internal.progress.BuildOperationExecutor;
import org.gradle.internal.progress.BuildProgressFilter;
import org.gradle.internal.progress.BuildProgressLogger;
import org.gradle.internal.progress.LoggerProvider;
import org.gradle.internal.reflect.Instantiator;
import org.gradle.internal.service.ServiceRegistry;
import org.gradle.internal.service.scopes.BuildScopeServices;
import org.gradle.internal.service.scopes.BuildSessionScopeServices;
import org.gradle.internal.service.scopes.GradleUserHomeScopeServiceRegistry;
import org.gradle.internal.service.scopes.ServiceRegistryFactory;
import org.gradle.invocation.DefaultGradle;
import org.gradle.profile.ProfileEventAdapter;
import org.gradle.profile.ReportGeneratingProfileListener;
import org.gradle.util.DeprecationLogger;

import java.util.List;

public class DefaultGradleLauncherFactory implements GradleLauncherFactory {
    private final ListenerManager globalListenerManager;
    private final GradleUserHomeScopeServiceRegistry userHomeDirServiceRegistry;
    private final NestedBuildTracker tracker;
    private final BuildProgressLogger buildProgressLogger;

    public DefaultGradleLauncherFactory(
        ListenerManager listenerManager, ProgressLoggerFactory progressLoggerFactory, GradleUserHomeScopeServiceRegistry userHomeDirServiceRegistry) {
        this.globalListenerManager = listenerManager;
        this.userHomeDirServiceRegistry = userHomeDirServiceRegistry;
        tracker = new NestedBuildTracker();

        // Register default loggers
        buildProgressLogger = new BuildProgressLogger(progressLoggerFactory);
        listenerManager.addListener(new BuildProgressFilter(buildProgressLogger));
        listenerManager.useLogger(new DependencyResolutionLogger(progressLoggerFactory));
    }

    private GradleLauncher createChildInstance(StartParameter startParameter, BuildSessionScopeServices buildSessionScopeServices, List<?> servicesToStop) {
        if (tracker.getCurrentBuild() == null) {
            throw new IllegalStateException("Must have a current build");
        }

        ServiceRegistry services = tracker.getCurrentBuild().getServices();
        BuildRequestMetaData requestMetaData = new DefaultBuildRequestMetaData(services.get(BuildClientMetaData.class));
        BuildCancellationToken cancellationToken = services.get(BuildCancellationToken.class);
        BuildEventConsumer buildEventConsumer = services.get(BuildEventConsumer.class);

        return doNewInstance(startParameter, true, cancellationToken, requestMetaData, buildEventConsumer, buildSessionScopeServices, servicesToStop);
    }

    @Override
    public GradleLauncher newInstance(StartParameter startParameter, BuildRequestContext requestContext, ServiceRegistry parentRegistry) {
        // This should only be used for top-level builds
        if (tracker.getCurrentBuild() != null) {
            throw new IllegalStateException("Cannot have a current build");
        }

        if (!(parentRegistry instanceof BuildSessionScopeServices)) {
            throw new IllegalArgumentException("Service registry must be of build session scope");
        }
        BuildSessionScopeServices sessionScopeServices = (BuildSessionScopeServices) parentRegistry;

        DefaultGradleLauncher launcher = doNewInstance(startParameter, false, requestContext.getCancellationToken(), requestContext, requestContext.getEventConsumer(), sessionScopeServices, ImmutableList.of());
        DeploymentRegistry deploymentRegistry = parentRegistry.get(DeploymentRegistry.class);
        deploymentRegistry.onNewBuild(launcher.getGradle());
        return launcher;
    }

    private DefaultGradleLauncher doNewInstance(StartParameter startParameter, boolean nestedInstance,
                                                BuildCancellationToken cancellationToken, BuildRequestMetaData requestMetaData, BuildEventConsumer buildEventConsumer, final BuildSessionScopeServices sessionScopeServices, List<?> servicesToStop) {
        BuildScopeServices serviceRegistry = BuildScopeServices.forSession(sessionScopeServices);
        serviceRegistry.add(BuildRequestMetaData.class, requestMetaData);
        serviceRegistry.add(BuildClientMetaData.class, requestMetaData.getClient());
        serviceRegistry.add(BuildEventConsumer.class, buildEventConsumer);
        serviceRegistry.add(BuildCancellationToken.class, cancellationToken);
        serviceRegistry.add(NestedBuildFactory.class, new NestedBuildFactoryImpl(sessionScopeServices));

        ListenerManager listenerManager = serviceRegistry.get(ListenerManager.class);
        LoggingManagerInternal loggingManager = serviceRegistry.newInstance(LoggingManagerInternal.class);
        loggingManager.setLevelInternal(startParameter.getLogLevel());

        //this hooks up the ListenerManager and LoggingConfigurer so you can call Gradle.addListener() with a StandardOutputListener.
        loggingManager.addStandardOutputListener(listenerManager.getBroadcaster(StandardOutputListener.class));
        loggingManager.addStandardErrorListener(listenerManager.getBroadcaster(StandardOutputListener.class));

        LoggerProvider loggerProvider = (tracker.getCurrentBuild() == null) ? buildProgressLogger : LoggerProvider.NO_OP;
        listenerManager.useLogger(new TaskExecutionLogger(serviceRegistry.get(ProgressLoggerFactory.class), loggerProvider));
        if (tracker.getCurrentBuild() == null) {
            listenerManager.useLogger(new BuildLogger(Logging.getLogger(BuildLogger.class), serviceRegistry.get(StyledTextOutputFactory.class), startParameter, requestMetaData));
        }
        listenerManager.addListener(tracker);

        if (startParameter.isTaskOutputCacheEnabled()) {
            listenerManager.addListener(serviceRegistry.get(TaskExecutionStatisticsEventAdapter.class));
            listenerManager.addListener(new CacheStatisticsReporter(serviceRegistry.get(StyledTextOutputFactory.class)));
        }

        listenerManager.addListener(serviceRegistry.get(ProfileEventAdapter.class));
        if (startParameter.isProfile()) {
            listenerManager.addListener(new ReportGeneratingProfileListener());
        }
        ScriptUsageLocationReporter usageLocationReporter = new ScriptUsageLocationReporter();
        listenerManager.addListener(usageLocationReporter);
        ShowStacktrace showStacktrace = startParameter.getShowStacktrace();
        switch (showStacktrace) {
            case ALWAYS:
            case ALWAYS_FULL:
                LoggingDeprecatedFeatureHandler.setTraceLoggingEnabled(true);
                break;
            default:
                LoggingDeprecatedFeatureHandler.setTraceLoggingEnabled(false);
        }
        DeprecationLogger.useLocationReporter(usageLocationReporter);

        SettingsLoaderFactory settingsLoaderFactory = serviceRegistry.get(SettingsLoaderFactory.class);
        SettingsLoader settingsLoader = nestedInstance ? settingsLoaderFactory.forNestedBuild() : settingsLoaderFactory.forTopLevelBuild();

        GradleInternal gradle = serviceRegistry.get(Instantiator.class).newInstance(DefaultGradle.class, tracker.getCurrentBuild(), startParameter, serviceRegistry.get(ServiceRegistryFactory.class));
        return new DefaultGradleLauncher(
            gradle,
            serviceRegistry.get(InitScriptHandler.class),
            settingsLoader,
            serviceRegistry.get(BuildConfigurer.class),
            serviceRegistry.get(ExceptionAnalyser.class),
            loggingManager,
            gradle.getBuildListenerBroadcaster(),
            listenerManager.getBroadcaster(ModelConfigurationListener.class),
            listenerManager.getBroadcaster(BuildCompletionListener.class),
            serviceRegistry.get(BuildOperationExecutor.class),
            gradle.getServices().get(BuildConfigurationActionExecuter.class),
            gradle.getServices().get(BuildExecuter.class),
            serviceRegistry,
            globalListenerManager,
            servicesToStop
        );
    }

    private class NestedBuildFactoryImpl implements NestedBuildFactory {
        private final BuildSessionScopeServices sessionScopeServices;

        public NestedBuildFactoryImpl(BuildSessionScopeServices sessionScopeServices) {
            this.sessionScopeServices = sessionScopeServices;
        }

        @Override
        public GradleLauncher nestedInstance(StartParameter startParameter) {
            return createChildInstance(startParameter, sessionScopeServices, ImmutableList.of());
        }

        @Override
        public GradleLauncher nestedInstanceWithNewSession(StartParameter startParameter) {
            final ServiceRegistry userHomeServices = userHomeDirServiceRegistry.getServicesFor(startParameter.getGradleUserHomeDir());
            BuildSessionScopeServices sessionScopeServices = new BuildSessionScopeServices(userHomeServices, startParameter, ClassPath.EMPTY);
            return createChildInstance(startParameter, sessionScopeServices, ImmutableList.of(sessionScopeServices, new Stoppable() {
                @Override
                public void stop() {
                    userHomeDirServiceRegistry.release(userHomeServices);
                }}));
        }
    }
}
