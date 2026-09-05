package com.logshield.logshieldv2;

import com.logshield.logshieldv2.model.LogEntryResponse;
import com.logshield.logshieldv2.service.LogShieldServiceImpl;
import com.logshield.logshieldv2.storage.FileHandler;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
			FileHandler fileHandler,
			LogShieldServiceImpl service) {

		return args -> {
			List<LogEntryResponse> persisted = fileHandler.loadLogs();

			if (persisted.isEmpty()) {
				System.out.println(
						"[LogShield] No persisted logs found. Starting fresh.");
				return;
			}

			Map<String, LogEntryResponse> cache = service.getCache();
			for (LogEntryResponse entry : persisted) {
				cache.put(entry.getTimestamp(), entry);
				service.getTrieService().trackPattern(entry.getMessage());
			}

			// Rewrite file to match HashMap state — removes duplicate timestamps
			// that accumulated across multiple runs
			fileHandler.rewriteAll(new ArrayList<>(cache.values()));

			System.out.println("[LogShield] Restored "
					+ cache.size()
					+ " log entries from persistence file.");
		};
	}
}