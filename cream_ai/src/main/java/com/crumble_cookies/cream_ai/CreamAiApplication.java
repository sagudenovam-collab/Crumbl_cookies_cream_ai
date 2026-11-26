package com.crumble_cookies.cream_ai;

import com.crumble_cookies.cream_ai.config.GeminiProperties;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableConfigurationProperties(GeminiProperties.class)
@EnableAsync
public class CreamAiApplication {

	public static void main(String[] args) {
		loadDotEnv();
		SpringApplication.run(CreamAiApplication.class, args);
	}

	/**
	 * Загружает переменные из файла .env (если он есть) в системные свойства,
	 * чтобы GEMINI_API_KEY можно было задать локально без экспорта в shell.
	 * Отсутствие .env не является ошибкой — переменные окружения ОС имеют приоритет.
	 */
	private static void loadDotEnv() {
		try {
			Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
			dotenv.entries().forEach(entry -> {
				if (System.getProperty(entry.getKey()) == null && System.getenv(entry.getKey()) == null) {
					System.setProperty(entry.getKey(), entry.getValue());
				}
			});
			String apiKey = System.getenv("GEMINI_API_KEY") != null
					? System.getenv("GEMINI_API_KEY")
					: System.getProperty("GEMINI_API_KEY");
			if (apiKey != null && !apiKey.isBlank()) {
				System.setProperty("gemini.api-key", apiKey);
			}
		} catch (Exception e) {
			System.err.println("Предупреждение: не удалось загрузить .env (" + e.getMessage() + "), продолжаем без него.");
		}
	}
}
