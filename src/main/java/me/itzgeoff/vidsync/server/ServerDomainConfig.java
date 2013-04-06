package me.itzgeoff.vidsync.server;

import java.sql.SQLException;

import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.h2.jdbcx.JdbcConnectionPool;
import org.h2.tools.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.Database;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories("me.itzgeoff.vidsync.domain.server")
@EnableTransactionManagement
public class ServerDomainConfig {
	@Bean
	public HibernateJpaVendorAdapter jpaVendorAdapter() {
		HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
		vendorAdapter.setGenerateDdl(true);
		vendorAdapter.setDatabase(Database.H2);
		return vendorAdapter;
	}
	
	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactoryBean() {
		LocalContainerEntityManagerFactoryBean emf = new LocalContainerEntityManagerFactoryBean();
		emf.setPackagesToScan("me.itzgeoff.vidsync.domain.server");
		emf.setDataSource(dataSource());
		emf.setJpaVendorAdapter(jpaVendorAdapter());
		return emf;
	}
	
	@Bean
	public EntityManagerFactory entityManagerFactory() {
		return entityManagerFactoryBean().getObject();
	}
	
	@Bean
	public DataSource dataSource() {
		return JdbcConnectionPool.create("jdbc:h2:~/.vidsync/db/server", "vidsync", "vidsync");
	}
	
	@Bean
	public JpaTransactionManager transactionManager() {
		JpaTransactionManager txManager = new JpaTransactionManager();
		txManager.setEntityManagerFactory(entityManagerFactory());
		return txManager;
	}
	
	@Bean
	public Server h2WebConsole() throws SQLException {
		Server webServer = Server.createWebServer();
		webServer.start();
		return webServer;
	}

}
