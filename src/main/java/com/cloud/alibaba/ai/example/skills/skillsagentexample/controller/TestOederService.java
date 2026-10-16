java
package com.cloud.alibaba.ai.example.skills.skillsagentexample.controller;

public class TestOrderService {

    public void createOrder(Long id) {
        orderMapper.insert(id);
    }
}
