package com.cloud.alibaba.ai.example.skills.skillsagentexample.controller;

import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.graph.agent.ReactAgent;
import com.alibaba.cloud.ai.graph.exception.GraphRunnerException;
import com.cloud.alibaba.ai.example.skills.skillsagentexample.agent.SkillsAgent;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.web.bind.annotation.*;

@RestController
public class ChatController {

    private final SkillsAgent skillsAgent;
    private final ChatModel chatModel;
    private ReactAgent agent;

    public ChatController(SkillsAgent skillsAgent, DashScopeChatModel chatModel) {
        this.skillsAgent = skillsAgent;
        this.chatModel = chatModel;
    }

    @GetMapping("/chat")
    public String chat(@RequestParam("message")String message) throws GraphRunnerException {
            System.out.println("开始执行");
            System.out.println("pr test");
            System.out.println("pr2");
            if (agent == null) {
                agent = skillsAgent.buildAgent(chatModel);
            }
            return String.valueOf(agent.call(message));
    }
}
