package me.itzgeoff.vidsync.server;

import me.itzgeoff.vidsync.common.VidSyncConstants;

import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;

public class ServerMain {

    /**
     * @param args
     */
    public static void main(String[] args) {
        initLogging();
        start();
    }

    public static void initLogging() {
        // assume SLF4J is bound to logback in the current environment
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        try {
            JoranConfigurator configurator = new JoranConfigurator();
            configurator.setContext(context);
            // Call context.reset() to clear any previous configuration, e.g.
            // default
            // configuration. For multi-step configuration, omit calling
            // context.reset().
            context.reset();
            configurator.doConfigure(ServerMain.class.getResource("/server-logback.xml"));
        } catch (JoranException je) {
            // StatusPrinter will handle this
        }
        StatusPrinter.printInCaseOfErrorsOrWarnings(context);
    }

    public static void start() {
        @SuppressWarnings("resource")
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.getEnvironment().setActiveProfiles("server");
        ctx.scan(
        // server package
                ServerMain.class.getPackage().getName(),
                // common package
                VidSyncConstants.class.getPackage().getName());
        ctx.refresh();

        ctx.registerShutdownHook();
    }

}
