package pe.ganadoconecta.infraestructura.persistencia;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Value;
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
    DataSource dataSource(DataSourceProperties properties,
            @Value("${spring.datasource.hikari.maximum-pool-size:3}") int maximo,
            @Value("${spring.datasource.hikari.register-mbeans:false}") boolean registrarMetricas) {
        HikariDataSource source = properties.initializeDataSourceBuilder()
                .type(HikariDataSource.class).build();
        source.setMaximumPoolSize(maximo);
        source.setConnectionTimeout(3000);
        source.setRegisterMbeans(registrarMetricas);
        return source;
    }
}
