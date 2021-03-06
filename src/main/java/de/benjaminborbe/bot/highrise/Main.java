package de.benjaminborbe.bot.highrise;

import de.benjaminborbe.bot.highrise.guice.GuiceModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Guice;
import com.google.inject.Injector;

import de.benjaminborbe.bot.agent.Address;
import de.benjaminborbe.bot.agent.Runner;

public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  private static final String botname = "highrise";

  public static final String AUTH_APPLICATION_PASSWORD = "AUTH_APPLICATION_PASSWORD";

  public static final String AUTH_APPLICATION_NAME = "AUTH_APPLICATION_NAME";

  public static final String AUTH_URL = "AUTH_URL";

  public static void main(final String[] args) {
    try {
      logger.debug("bot started");
      final Address nsqdAddress = Address.fromEnv("NSQD_ADDRESS");
      final Address nsqLookupdAddress = Address.fromEnv("NSQ_LOOKUPD_ADDRESS");

      final String auth_application_name = System.getenv(AUTH_APPLICATION_NAME);
      if (auth_application_name == null || auth_application_name.isEmpty()) {
        System.out.println("param " + AUTH_APPLICATION_NAME + " is missing");
        System.exit(1);
      }

      final String auth_application_password = System.getenv(AUTH_APPLICATION_PASSWORD);
      if (auth_application_password == null || auth_application_password.isEmpty()) {
        System.out.println("param " + AUTH_APPLICATION_PASSWORD + " is missing");
        System.exit(1);
      }

      final String auth_url = System.getenv(AUTH_URL);
      if (auth_url == null || auth_url.isEmpty()) {
        System.out.println("param " + AUTH_URL + " is missing");
        System.exit(1);
      }

      final Injector injector = Guice.createInjector(new GuiceModule());
      final Config config = injector.getInstance(Config.class);
      config.setAuthPassword(auth_application_password);
      config.setAuthUser(auth_application_name);
      config.setAuthUrl(auth_url);

      final Runner instance = injector.getInstance(Runner.class);

      System.out.println("starting bot");
      instance.run(nsqdAddress, nsqLookupdAddress, botname);
    } catch (final Exception e) {
      System.out.println("bot died");
      logger.warn("bot failed", e);
    }
  }
}
