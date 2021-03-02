package org.egov.egf.web.controller.microservice;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class InBoxController {

	@GetMapping("/inbox")
	public String showInbox(){
		return "inbox-view";
	}
}
