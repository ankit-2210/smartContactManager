console.log("Heello");

// first request - to server to create order
const paymentStart = () => {
	console.log("Payment Started..");	
	let amount=$("#payment_field").val();
	console.log(amount);
	
	if(amount == '' || amount == null){
		//alert("Amount is required !!");
		swap("Failed!", "Amount is required !!", "error"); 
		return;
	}
		
	// Ajax to send request to server to create order
	$.ajax({
		url: "/user/create_order",
		data: JSON.stringify({amount: amount, info: "order_request"}),
		contentType: "application/json",
		type: "POST",
		dataType: "json",
		success: function(response){
			if(response.status == 'created'){
				 // open payment form
				 let options={
					key: 'rzp_test_YfFqvXPL8JPBdJ',
					amount: response.ammount,
					currency: 'INR',
					name: 'Smart Contact',
					description: 'Donation',
					image: '',
					order_id:response.id,
					handler:function(response){
						console.log(response.razorpay_payment_id);
						console.log(response.razorpay_order_id);
						console.log(response.razorpay_signature);
						console.log("Payment Sucessfull !!");
						//alert("Congrats!! Payment Successfull !!");
						
						updatePaymentOnServer(response.razorpay_payment_id, response.razorpay_order_id, "paid");
						
						swal("Good Job!", "Congrats!! Payment Successfull !!", "success")
					},
					prefill: {
						name: "",
					    email: "",
					    contact: ""
					},
					notes:{
						address: "Learn Spring Boot"	
					},
					theme:{
						color: "#3399cc"
					}
				 } 
				 
				 let rzp=new Razorpay(options);
				 rzp.on('payment.failed', function (response){
				 	console.log(response.error.code);
				    console.log(response.error.description);
				    console.log(response.error.source);
				    console.log(response.error.step);
				    console.log(response.error.reason);
				    console.log(response.error.metadata.order_id);
				    console.log(response.error.metadata.payment_id);
					// alert("Oops !! Payment failed")
					swap("Failed!", "Oops !! Payment failed", "error"); 
				});
				 
				 rzp.open();
			}
			
			// invoked when success
			console.log(response);
		},
		error: function(error){
			alert("Something went wrong !!")
		}
	})
}

const updatePaymentOnServer=(payment_id, order_id, status)=>{
	
	$.ajax({
		url: "/user/update_order",
		data: JSON.stringify({payment_id: payment_id, order_id: order_id, status: status}),
		contentType: "application/json",
		type: "POST",
		dataType: "json",
		success: function(response){
			swal("Good Job!", "Congrats!! Payment Successfull !!", "success")
		},
		error: function(error){
			swap("Failed!", "Your payment is Successful, but we did not get on server, we will contact you as soon as possible !!", "error"); 
		}

	});
}


