package org.livef.livef_dataservice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DataController {

    @GetMapping("/data/test1")
    public String dataServer() {

        return "test1.html";
    }
}
