package com.logshield.logshieldv2;

import com.logshield.logshieldv2.entity.LogEntryRepository;
import com.logshield.logshieldv2.model.LogEntryResponse;
import com.logshield.logshieldv2.service.LogShieldServiceImpl;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;


/**
 * LogShield v2 Spring Boot entry point.
 *
 * CommandLineRunner runs after the Spring context is fully loaded —
 * all beans are ready before we attempt to load from file.
 * This restores persisted log entries into the in-memory cache
 * so the system resumes exactly where it left off.
 */
@SpringBootApplication
public class LogshieldV2Application {

	public static void main(String[] args) {
		SpringApplication.run(LogshieldV2Application.class, args);
	}

	/**
	 * Runs once after Spring context is ready.
	 * Loads persisted log entries from disk into the service cache.
	 *
	 * CommandLineRunner is preferred over @PostConstruct here because
	 * it runs after the entire application context is initialized —
	 * all beans including FileHandler and LogShieldServiceImpl are ready.
	 */
	@Bean
	public CommandLineRunner loadPersistedLogs(
			LogEntryRepository repository,
			LogShieldServiceImpl service) {

		return args -> {
			long count = repository.count();

			if (count == 0) {
				System.out.println(
						"[LogShield] No persisted logs found. Starting fresh.");
				return;
			}

			// Rebuild in-memory cache and Trie from DB on startup
			repository.findAll().forEach(entity -> {
				LogEntryResponse response = new LogEntryResponse(
						entity.getId().toString(),
						entity.getTimestamp(),
						entity.getLevel(),
						entity.getMessage(),
						entity.getSeverityScore()
				);
				service.getCache().put(entity.getId().toString(), response);
				service.getTrieService().trackPattern(entity.getMessage());
			});

			System.out.println("[LogShield] Restored "
					+ count + " log entries from database.");
		};
	}
}