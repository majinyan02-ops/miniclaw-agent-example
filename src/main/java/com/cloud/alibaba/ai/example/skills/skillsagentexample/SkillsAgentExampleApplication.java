package com.cloud.alibaba.ai.example.skills.skillsagentexample;

import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeConnectionProperties;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@SpringBootApplication
public class SkillsAgentExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkillsAgentExampleApplication.class, args);
    }

    @Bean
    public DashScopeApi dashScopeApi(DashScopeConnectionProperties connectionProperties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(60 * 1000);    // 连接超时时间，单位毫秒
        requestFactory.setReadTimeout(3 * 60 * 1000);    // 读取超时时间，单位毫秒
        return DashScopeApi.builder()
                .apiKey(connectionProperties.getApiKey())    // 这个是自动配置类，从配置文件读取配置
                .restClientBuilder(RestClient.builder().requestFactory(requestFactory))
                .build();
    }
}
