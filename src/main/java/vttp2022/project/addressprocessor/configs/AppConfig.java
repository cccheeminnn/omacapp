package vttp2022.project.addressprocessor.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import vttp2022.project.addressprocessor.filters.AuthenticationFilters;

@Configuration
@ConfigurationProperties
public class AppConfig {
    
    @Bean
    public FilterRegistrationBean<AuthenticationFilters> registerFilters() {
        //Create an instance of authentication filter
        AuthenticationFilters authFilter = new AuthenticationFilters();

        //Create an instance of registration filter
        FilterRegistrationBean<AuthenticationFilters> regFilter = new FilterRegistrationBean<>();
        regFilter.setFilter(authFilter);
        //attemps to access these PathVariables goes through the doFilter itself
        //same as "/protected/*"
        regFilter.addUrlPatterns("/user/*");

        return regFilter;
    }
}
