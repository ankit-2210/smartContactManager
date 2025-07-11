package com.smart.helper;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpSession;

@Component
public class SessionHelpher {
	public void removeMessageFromSession() {
		try {
			System.out.println("Removing message form session");
			HttpSession session = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getSession();
			session.removeAttribute("message");
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
}
