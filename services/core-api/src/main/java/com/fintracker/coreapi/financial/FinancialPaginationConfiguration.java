/**
 * Defines safe global pagination defaults for financial collection endpoints.
 *  * A maximum page size limits accidental or abusive requests for very large result sets; a fallback page keeps endpoints predictable when clients omit page parameters.
 *  * Controllers still declare domain-specific default sort orders.
 */
package com.fintracker.coreapi.financial;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer;

/**
 * Applies a service-wide upper bound to client-requested page sizes.
 * Individual controllers continue to define their own safe default sort.
 */
@Configuration
class FinancialPaginationConfiguration {
    @Bean
    PageableHandlerMethodArgumentResolverCustomizer financialPageableCustomizer() {
        return resolver -> {
            resolver.setMaxPageSize(100);
            resolver.setFallbackPageable(PageRequest.of(0, 20));
        };
    }
}
