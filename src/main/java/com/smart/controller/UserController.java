package com.smart.controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.*;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.razorpay.*;

import com.smart.dao.ContactRepository;
import com.smart.dao.OrderRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.MyOrder;
import com.smart.entities.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private BCryptPasswordEncoder bcryptPasswordEncoder;
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private ContactRepository contactRepository;
	
	@Autowired
	private OrderRepository orderRepository;
	
	// method for adding common data to response
	@ModelAttribute
	public void addCommonData(Model model, Principal principal) {
		String userName = principal.getName();
		System.out.println("USERNAME " + userName);
		
		// get the user using username
		User user = userRepository.getUserByUserName(userName);
		System.out.println("USER " + user);
		
		model.addAttribute("user", user);
	}
	
	//dashboard
	@RequestMapping("/index")
	public String dashboard(Model model, Principal principal) {
		
		model.addAttribute("title", "User Dashboard");
		
		return "normal/user_dashboard";
	}
	
	// open add-form handler
	@GetMapping("/add-contact")
	public String openAddContactForm(Model model) {
		
		model.addAttribute("title", "Add Contact");
		model.addAttribute("contact", new Contact());
		
		return "normal/add_contact_form";
	}
	
	// processing add contact form
	@PostMapping("/process-contact")
	public String processContact(@ModelAttribute Contact contact, @RequestParam("Profileimage") MultipartFile file, Principal principal, HttpSession session) {	
		
		try {
			// Associate contact with user
			String name = principal.getName();
			User user = this.userRepository.getUserByUserName(name);
	    
	    
			// processing and uploading file....
			if(file.isEmpty()) {
				// if the file is empty then try our message
				System.out.println("File is Empty");
				contact.setImageProfile("contact.png");
			}
			else {
				// file the file to folder and update the name to contact
				contact.setImageProfile(file.getOriginalFilename());
	    	
				File saveFile =  new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
	  	   		Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
	    
	  	   		System.out.println("Image is Uploaded");
			}
	    
			contact.setUser(user);
			user.getContacts().add(contact);
			this.userRepository.save(user);
			session.setAttribute("message", new Message("Your contact is added !!", "success"));
			
			System.out.println("Data " + contact);
	    
		}
		catch(Exception e) {
			System.out.println("ERROR " + e.getMessage());
			session.setAttribute("message", new Message("Something went wrong !!", "danger"));
			e.printStackTrace();
		}

	    return "normal/add_contact_form";
	}
	
	
	// show contacts handler
	@GetMapping("/show-contacts/{page}")
	public String showContacts(@PathVariable("page") Integer page, Model model, Principal principal) {
		
		model.addAttribute("title", "Show Contacts Page");
		
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		// currentPage-page
		// Contact per page = 5
		Pageable pageable=PageRequest.of(page, 5);
		Page<Contact> contacts = this.contactRepository.findContactByUser(user.getId(), pageable);
		model.addAttribute("contacts", contacts);
		model.addAttribute("currentPage", page);
		model.addAttribute("totalPage", contacts.getTotalPages());
		
		return "normal/show_contacts";
	}
	
	// showing particular contact details
	@RequestMapping("/{cid}/contact")
	public String showContactdetail(@PathVariable("cid") Integer cid, Model model, Principal principal) {
		
		System.out.println("CID is" + cid);
		
		Optional<Contact> contactopt =  this.contactRepository.findById(cid);
		Contact contact = contactopt.get();
		
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		if(user.getId() == contact.getUser().getId()) {
			model.addAttribute("contact", contact);
			model.addAttribute("title", contact.getName());
		}
		
		return "normal/contact_detail";
	}
	
	// delete contact handler
	@GetMapping("/delete/{cid}")
	public String deleteContact(@PathVariable("cid") Integer cid, Model model, Principal principal, HttpSession session) {
	
		Contact contact = this.contactRepository.findById(cid).get();
		
		String userName = principal.getName();
		User user = this.userRepository.getUserByUserName(userName);
		
		if(user.getId() == contact.getUser().getId()) {
			System.out.println("Contact " + contact.getCid());
			
//			contact.setUser(null);
			String imagePath = contact.getImageProfile();

	        if (imagePath != null && !imagePath.isEmpty() && imagePath != "contact.png") {
	            try {
	                File saveFile = new ClassPathResource("static/img").getFile();
	                Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + imagePath);
	                System.out.println(imagePath+ " " + saveFile + " "  +path);
	                Files.deleteIfExists(path);
	                System.out.println("Deleted image file: " + imagePath);
	                
	            } 
	            catch (Exception e) {
	                e.printStackTrace();
	            }
	        }
			
			user.getContacts().remove(contact);
			this.userRepository.save(user);
			
			System.out.println("Deleted !!");
			
			session.setAttribute("message", new Message("Contact deleted successfully...", "success"));
		}
		
		return "redirect:/user/show-contacts/0";
	}
	
	// open update form handler
	@PostMapping("/update-contact/{cid}")
	public String updateForm(@PathVariable("cid") Integer cid, Model model) {
		
		model.addAttribute("title", "Update Contact");
		
		Contact contact = this.contactRepository.findById(cid).get();
		model.addAttribute("contact", contact);
		
		return "normal/update_form";
	}
	
	// update contact handler
	@RequestMapping(value="/process-update", method=RequestMethod.POST)
	public String updateHandler(@ModelAttribute Contact contact, @RequestParam("Profileimage") MultipartFile file, Model model, Principal principal, HttpSession session) {
		
		try {	
			Contact oldcontact = this.contactRepository.findById(contact.getCid()).get();
			
			// file image..
			if(!file.isEmpty()) {
				// delete old photo
				String imagePath = contact.getImageProfile();
		        if (imagePath != null && !imagePath.isEmpty() && imagePath != "contact.png") {
		        	File saveFile = new ClassPathResource("static/img").getFile();
		            Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + imagePath);
		            System.out.println(imagePath+ " " + saveFile + " "  +path);
		            Files.deleteIfExists(path);
		            System.out.println("Deleted image file: " + imagePath);
		        } 
				
				// update new photo
				File saveFile =  new ClassPathResource("static/img").getFile();
				Path path = Paths.get(saveFile.getAbsolutePath()+File.separator+file.getOriginalFilename());
	  	   		Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
	    
	  	   		contact.setImageProfile(file.getOriginalFilename());
			}
			else {
				contact.setImageProfile(oldcontact.getImageProfile());
			}
			
			User user = this.userRepository.getUserByUserName(principal.getName());
			contact.setUser(user);
			this.contactRepository.save(contact);
			
			session.setAttribute("message", new Message("Your contact is updated !", "success"));
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		System.out.println("Contact name " + contact.getName());
		System.out.println("Contact id " + contact.getCid());
		
		return "redirect:/user/"+contact.getCid()+"/contact";
	}
	
	// profile handler
	@GetMapping("/profile")
	public String profileHandler(Model model) {
		
		model.addAttribute("title", "Profile Page");
		return "normal/profile";
	}
	
	// setting handler
	@GetMapping("/settings")
	public String settingHandler(Model model) {
		
		model.addAttribute("title", "Settings Page");
		return "normal/settings";
	}
	
	// change password handler
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("oldPassword") String oldPassword, @RequestParam("newPassword") String newPassword, Principal principal, HttpSession session) {
		
		System.out.println("OldPassword " + oldPassword);
		System.out.println("NewPassword " + newPassword);
		
		String userName = principal.getName();
		User currUser = this.userRepository.getUserByUserName(userName);
		System.out.println(currUser);
		
		
		if(this.bcryptPasswordEncoder.matches(oldPassword, currUser.getPassword())) {
			currUser.setPassword(this.bcryptPasswordEncoder.encode(newPassword));
			this.userRepository.save(currUser);
			session.setAttribute("message", new Message("Your Password has successfully changed !!", "success"));
		}
		else {
			session.setAttribute("message", new Message("Please enter correct old password !!", "danger"));
			return "redirect:/user/settings";
		}
	
		
		return "redirect:/user/index";
	}
	
	
	// creating order for payment
	@PostMapping("/create_order")
	@ResponseBody
	public String createOrder(@RequestBody Map<String, Object> data, Principal principal) throws Exception{
		System.out.println(data);
		String userName = principal.getName();
		User currUser = this.userRepository.getUserByUserName(userName);
		int amt = Integer.parseInt(data.get("amount").toString());
		
		var client = new RazorpayClient("rzp_test_YfFqvXPL8JPBdJ", "BvhNZhnzP15Umh3AfSTDp3bm");

		JSONObject ob= new JSONObject();
		ob.put("amount", amt);
		ob.put("currency", "INR");
		ob.put("receipt", "txn_235425");
		
		// creating new order
		Order order = client.orders.create(ob);
		System.out.println(order);
		
		// save the order on database..
		MyOrder myorder = new MyOrder();
		myorder.setAmount(order.get("amount").toString());
		myorder.setOrderId(order.get("id"));
		myorder.setPaymentId(null);
		myorder.setStatus("created");
		myorder.setUser(currUser);
		myorder.setReceipt(order.get("receipt"));
		
		this.orderRepository.save(myorder);
		
		return order.toString();
	}
	
	
	@PostMapping("/update_order")
	public ResponseEntity<?> updateOrder(@RequestBody Map<String, Object> data){
		
		MyOrder myorder=this.orderRepository.findByOrderId(data.get("order_id").toString());
		myorder.setPaymentId(data.get("payment_id").toString());
		myorder.setStatus(data.get("status").toString());
		
		this.orderRepository.save(myorder);
		
		System.out.println(data);
		return ResponseEntity.ok(Map.of("msg", "updated"));

	}
	
	
	
	
}
