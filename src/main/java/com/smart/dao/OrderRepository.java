package com.smart.dao;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smart.entities.MyOrder;

public interface OrderRepository extends JpaRepository<MyOrder, Long>{

	public MyOrder findByOrderId(String orderId);
	
}
