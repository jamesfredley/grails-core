package org.grails.forge.feature.micronaut;

import jakarta.inject.Singleton;
import org.grails.forge.application.ApplicationType;
import org.grails.forge.feature.Feature;
import org.grails.forge.feature.reloading.SpringBootDevTools;
import org.grails.forge.feature.validation.FeatureValidator;
import org.grails.forge.options.Options;

import java.util.Set;

@Singleton
public class GrailsMicronautValidator implements FeatureValidator {
    @Override
    public void validatePreProcessing(Options options, ApplicationType applicationType, Set<Feature> features) {
        if (features.stream().anyMatch(f -> f instanceof GrailsMicronaut)) {
            if (features.stream().anyMatch(f -> (f instanceof SpringBootDevTools))) {
                // TODO: https://github.com/micronaut-projects/micronaut-spring/issues/769
                throw new IllegalArgumentException("Spring Boot Dev Tools are not supported with Grails Micronaut");
            }
        }
    }

    @Override
    public void validatePostProcessing(Options options, ApplicationType applicationType, Set<Feature> features) {

    }
}
