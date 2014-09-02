package uk.co.revsys.content.repository;

import java.net.URL;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.converters.DateConverter;
import org.apache.commons.beanutils.converters.DateTimeConverter;
import org.infinispan.schematic.document.ParsingException;
import org.modeshape.common.collection.Problems;
import org.modeshape.jcr.ConfigurationException;
import org.modeshape.jcr.ModeShapeEngine;
import org.modeshape.jcr.RepositoryConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServiceInitializer implements ServletContextListener {

    private final Logger LOGGER = LoggerFactory.getLogger(ServiceInitializer.class);
    private ModeShapeEngine engine;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        init();
    }

    public void init() {
        engine = new ModeShapeEngine();
        engine.start();

//        DateTimeConverter dtConverter = new DateConverter();
//        dtConverter.setPattern("E MMM dd HH:mm:ss z yyyy");
//        ConvertUtils.register(dtConverter, Date.class);

        try {
            URL url = ServiceInitializer.class.getClassLoader().getResource("repository.json");
            RepositoryConfiguration config = RepositoryConfiguration.read(url);
            config.getBinaryStorage();
            Problems problems = config.validate();
            if (problems.hasErrors()) {
                LOGGER.error("Problems starting the engine: " + problems);
                throw new RuntimeException("Problems starting the engine: " + problems);
            }

            engine.deploy(config);
            Repository repository = engine.getRepository(config.getName());
            JCRFactory.setRepository(repository);
            Session session = repository.login();
            session.getWorkspace().getNamespaceRegistry().registerNamespace("rcr", "http://www.revolutionarysystems.co.uk");
            session.logout();
        } catch (RepositoryException ex) {
            LOGGER.error("Problems loading the repository", ex);
            throw new RuntimeException("Problems loading the repository", ex);
        } catch (ParsingException ex) {
            LOGGER.error("Problems loading the repository", ex);
            throw new RuntimeException("Problems loading the repository", ex);
        } catch (ConfigurationException ex) {
            LOGGER.error("Problems loading the repository", ex);
            throw new RuntimeException("Problems loading the repository", ex);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        shutdown();
    }

    public void shutdown() {
        System.out.println("Shutting down ModeShape engine ...");
        LOGGER.info("Shutting down ModeShape engine ...");
        try {
            engine.shutdown().get();
            System.out.println("ModeShape engine shutdown successfully");
            LOGGER.info("ModeShape engine shutdown successfully");
        } catch (InterruptedException ex) {
            LOGGER.error("Could not shutdown ModeShape engine", ex);
        } catch (ExecutionException ex) {
            LOGGER.error("Could not shutdown ModeShape engine", ex);
        }
    }

}
