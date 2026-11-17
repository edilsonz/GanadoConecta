package pe.ganadoconecta.infraestructura.persistencia;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/** Adaptador opcional de conexión; no crea tablas ni implementa repositorios. */
@Configuration(proxyBeanMethods = false)
@Profile("postgres")
@EnableConfigurationProperties(DataSourceProperties.class)
public class PostgresConfiguracion {
    @Bean
    DataSource dataSource(DataSourceProperties properties) {
        HikariDataSource source = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class).build();
        source.setMaximumPoolSize(3);
        source.setConnectionTimeout(3000);
        return source;
    }
}
