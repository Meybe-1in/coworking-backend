
package com.coworking.util;

import com.coworking.role.repository.RoleRepository;
import com.coworking.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AdminBootstrapInitializerContextTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner()
                    .withUserConfiguration(TestConfig.class);

    @Test
    void shouldRegisterInitializerWhenProdProfileAndBootstrapEnabled() {
        contextRunner
                .withInitializer(context ->
                        context.getEnvironment().setActiveProfiles("prod"))
                .withPropertyValues(
                        "app.admin-bootstrap.enabled=true",
                        "ADMIN_USERNAME=production-admin",
                        "ADMIN_EMAIL=admin@example.com",
                        "ADMIN_PASSWORD=test-password"
                )
                .run(context ->
                        assertThat(context)
                                .hasSingleBean(AdminBootstrapInitializer.class)
                );
    }

    @Test
    void shouldNotRegisterInitializerWhenBootstrapIsDisabled() {
        contextRunner
                .withInitializer(context ->
                        context.getEnvironment().setActiveProfiles("prod"))
                .withPropertyValues(
                        "app.admin-bootstrap.enabled=false"
                )
                .run(context ->
                        assertThat(context)
                                .doesNotHaveBean(AdminBootstrapInitializer.class)
                );
    }

    @Test
    void shouldNotRegisterInitializerWhenDockerProfileIsActive() {
        contextRunner
                .withInitializer(context ->
                        context.getEnvironment().setActiveProfiles("docker"))
                .withPropertyValues(
                        "app.admin-bootstrap.enabled=true"
                )
                .run(context ->
                        assertThat(context)
                                .doesNotHaveBean(AdminBootstrapInitializer.class)
                );
    }

    @Test
    void shouldNotRegisterInitializerWhenProfileIsNotProd() {
        contextRunner
                .withInitializer(context ->
                        context.getEnvironment().setActiveProfiles("test"))
                .withPropertyValues(
                        "app.admin-bootstrap.enabled=true"
                )
                .run(context ->
                        assertThat(context)
                                .doesNotHaveBean(AdminBootstrapInitializer.class)
                );
    }

    @TestConfiguration(proxyBeanMethods = false)
    @Import(AdminBootstrapInitializer.class)
    static class TestConfig {

        @Bean
        UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        RoleRepository roleRepository() {
            return mock(RoleRepository.class);
        }

        @Bean
        PasswordEncoder passwordEncoder() {
            return mock(PasswordEncoder.class);
        }
    }
}
