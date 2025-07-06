package com.smart.controller;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.service.EmailService;

import jakarta.servlet.http.HttpSession;

@Controller
public class ForgotController {
	
	@Autowired
	private EmailService emailService;
	
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private BCryptPasswordEncoder bcrypt;
	
	Random random = new Random(1000);
	
	
	// email id form open handler
	@RequestMapping("/forgot")
	public String openEmailForm() {
	
		return "forgot_email_form";
	}
	
	@PostMapping("/send-otp")
	public String sendOTP(@RequestParam("email") String email, HttpSession session) {
		
		System.out.println("Email " + email);
		
		User user = this.userRepository.getUserByUserName(email);
		if(user == null) {
			// send error message
			session.setAttribute("message", "User does not exist with this email !!");
			return "forgot_email_form";
		}
		
		// generating otp of 4 digit
		int otp = random.nextInt(999999);
		System.out.println("OTP " + otp);
		
		// code for send otp to email...
		String subject="OTP from SCM";
		String message="<p>Hello " + user.getName() + "</p>"
	            + "<p>For security reason, you're required to use the following "
	            + "One Time Password to login:</p>"
	            + "<p><b>" + otp + "</b></p>"
	            + "<br>"
	            + "<p>Note: this OTP is set to expire in 5 minutes.</p>";
		String to=email;
		
		boolean flag = this.emailService.sendEmail(subject, message, to);
		System.out.println(flag);
		if(flag) {
			
			session.setAttribute("myotp", otp);
			session.setAttribute("email", email);
			return "verify_otp";
		}
		else {
			session.setAttribute("message", "Check your email id !!");			
			return "forgot_email_form";
		}
		
	}
	
	
	// verify otp
	@PostMapping("/verify-otp")
	public String verifyOtp(@RequestParam("otp") int otp, HttpSession session) {
		
		int myOtp = (int)session.getAttribute("myotp");
		String email = (String)session.getAttribute("email");
		
		if(myOtp == otp) {
			// password change form
			
			User user = this.userRepository.getUserByUserName(email);
			if(user == null) {
				// send error message
				session.setAttribute("message", "User does not exist with this email !!");
				return "forgot_email_form";
			}
			else {
				// send change password form
				
			}
			
			return "password_change_form";
		}
		else {
			session.setAttribute("message", "You have entered wrong otp !!");
			return "/verify-otp";
		}
	
	}
	
	
	// change password
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("newpassword") String newpassword, HttpSession session) {
		
		String email = (String)session.getAttribute("email");
		
		User user = this.userRepository.getUserByUserName(email);
		user.setPassword(this.bcrypt.encode(newpassword));
		this.userRepository.save(user);
		
		return "redirect:/signin?change=Password Changed Successfully..";
		
	}
	
	
	
}
