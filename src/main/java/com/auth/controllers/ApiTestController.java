package com.auth.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ApiTestController {

	@GetMapping({ "/api-test", "/api-test/" })
	public String apiTest() {
		return "redirect:/api-test/index.html";
	}
}
