package org.jpos.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.jpos.q2.QBeanSupport;
import org.jpos.util.Log;

public class JettyService extends QBeanSupport {
    private Server server;
    private int port = 8087;
    private String contextPath = "/api";

    @Override
    protected void initService() throws Exception {
        server = new Server(port);

        ResourceConfig config = new ResourceConfig();
        config.packages("org.jpos.rest.api");

        ServletHolder servlet = new ServletHolder(new ServletContainer(config));

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath(contextPath);
        context.addServlet(servlet, "/*");

        server.setHandler(context);

        log.info("Jetty service initialized on port " + port);
    }

    @Override
    protected void startService() throws Exception {
        server.start();
        log.info("Jetty server started on port " + port);
    }

    @Override
    protected void stopService() throws Exception {
        if (server != null) {
            server.stop();
            log.info("Jetty server stopped");
        }
    }

    @Override
    protected void destroyService() throws Exception {
        if (server != null) {
            server.destroy();
        }
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setContextPath(String contextPath) {
        this.contextPath = contextPath;
    }
}
