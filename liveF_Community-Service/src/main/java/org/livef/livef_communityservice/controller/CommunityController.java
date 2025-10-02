package org.livef.livef_communityservice.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class CommunityController {

    @GetMapping("/data/test1")
    public String communityServer() {

        return "test1.html";
    }
}
