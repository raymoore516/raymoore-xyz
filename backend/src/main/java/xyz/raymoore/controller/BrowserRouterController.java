package xyz.raymoore.controller;

import org.springframework.boot.autoconfigure.condition.ConditionalOnResource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@ConditionalOnResource(resources = "classpath:/static/index.html")
public class BrowserRouterController {

    @GetMapping({
            "/",
            "/madisonsc",
            "/madisonsc/picks/latest",
            "/madisonsc/picks/{year}",
            "/madisonsc/picks/{year}/{week}"
    })
    public String serveReactApplication() {
        return "forward:/index.html";
    }
}
