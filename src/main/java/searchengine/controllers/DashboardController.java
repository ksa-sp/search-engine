package searchengine.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Application web GUI controller.
 */
@Controller
public class DashboardController {
    /**
     * Метод формирует страницу из HTML-файла index.html,
     * который находится в папке resources/templates.
     * Это делает библиотека Thymeleaf
     *
     * @return Root file name without extension.
     */
    @RequestMapping("/")
    public String index() {
        return "index";
    }
}
