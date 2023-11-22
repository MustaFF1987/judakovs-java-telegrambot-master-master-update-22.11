package ebe.P_Judakov.s.JAVABOT;
import ebe.P_Judakov.s.JAVABOT.service.interfaces.TelegramBotService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@Configuration
@EnableJpaRepositories(basePackages = "com.potholeapi.repositories")
@EnableTransactionManagement
@ComponentScan(basePackages = {
        "ebe.P_Judakov.s.JAVABOT.service.interfaces",
        "ebe.P_Judakov.s.JAVABOT.service.jpa",
        "ebe.P_Judakov.s.JAVABOT.scheduler",
        "ebe.P_Judakov.s.JAVABOT.repository.mysql",
        "ebe.P_Judakov.s.JAVABOT.controller"
})
@EntityScan(basePackages = {
        "ebe.P_Judakov.s.JAVABOT.domen.entity.interfaces",
        "ebe.P_Judakov.s.JAVABOT.domen.entity.jpa",
        "ebe.P_Judakov.s.JAVABOT.domen.entity.role"
})
public class Application {
	public static void main(String[] args) {
		ConfigurableApplicationContext context = SpringApplication.run(Application.class, args);

		TelegramBotService bot = context.getBean(TelegramBotService.class);
		bot.init();
	}
}

