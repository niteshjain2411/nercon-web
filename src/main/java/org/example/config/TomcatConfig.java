package org.example.config;

import jakarta.servlet.ServletException;
import java.io.IOException;
import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.ValveBase;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TomcatConfig {

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> rootRedirectCustomizer() {
        return factory -> factory.addEngineValves(new ValveBase() {
            @Override
            public void invoke(Request request, Response response) throws IOException, ServletException {
                String uri = request.getDecodedRequestURI();
                if ("/".equals(uri)) {
                    response.sendRedirect("/nercon/home.html");
                    return;
                }
                // Redirect any root-level path (e.g. /Nercon%20Logo.jpeg) to /nercon/<path>
                // This handles browsers/extensions that strip the context path from resource URLs
                if (!uri.startsWith("/nercon/")) {
                    response.sendRedirect("/nercon" + request.getRequestURI());
                    return;
                }
                getNext().invoke(request, response);
            }
        });
    }
}
