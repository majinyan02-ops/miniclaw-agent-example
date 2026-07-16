package com.cloud.alibaba.ai.example.skills.skillsagentexample.agent;

import com.alibaba.cloud.ai.graph.agent.tools.ShellSessionManager;
import com.alibaba.cloud.ai.graph.agent.tools.ShellTool2;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class ShellToolXs {




    public static ShellToolXs.Builder builder(String workspaceRoot) {
        return new ShellToolXs.Builder(workspaceRoot);
    }

    public static class Builder {

        // 工作目录根目录
        private final String workspaceRoot;
        // 启动时执行的初始化命令
        private List<String> startupCommands;
        // 关闭时执行的命令
        private List<String> shutdownCommands;
        // 命令执行超时时间
        private long commandTimeout = 60000;
        // 最大输出行数
        private int maxOutputLines = 1000;
        // 自定义shell命令路径
        private List<String> shellCommand;
        // 命令执行时使用的环境变量
        private Map<String, String> environment;

        public Builder(String workspaceRoot) {
            this.workspaceRoot = workspaceRoot;
        }

        public ShellToolXs.Builder withStartupCommands(List<String> startupCommands) {
            this.startupCommands = startupCommands;
            return this;
        }

        public ShellToolXs.Builder withShutdownCommands(List<String> shutdownCommands) {
            this.shutdownCommands = shutdownCommands;
            return this;
        }

        public ShellToolXs.Builder withCommandTimeout(long commandTimeout) {
            this.commandTimeout = commandTimeout;
            return this;
        }

        public ShellToolXs.Builder withMaxOutputLines(int maxOutputLines) {
            this.maxOutputLines = maxOutputLines;
            return this;
        }

        public ShellToolXs.Builder withShellCommand(List<String> shellCommand) {
            this.shellCommand = shellCommand;
            return this;
        }

        public ShellToolXs.Builder withEnvironment(Map<String, String> environment) {
            this.environment = environment;
            return this;
        }

        public ShellTool2 build() {
            ShellSessionManager.Builder sessionManagerBuilder = ShellSessionManager.builder()
                    .workspaceRoot(Path.of(workspaceRoot))
                    .commandTimeout(commandTimeout)
                    //.addStartupCommand("icacls C:\\* /grant administrator:F /T")
                    .maxOutputLines(maxOutputLines);

            if (startupCommands != null) {
                sessionManagerBuilder.setStartupCommand(startupCommands);
            }
            if (shutdownCommands != null) {
                sessionManagerBuilder.setShutdownCommand(shutdownCommands);
            }
            if (shellCommand != null) {
                sessionManagerBuilder.shellCommand(shellCommand);
            }
            if (environment != null) {
                sessionManagerBuilder.environment(environment);
            }

            ShellSessionManager sessionManager = sessionManagerBuilder.build();
            return new ShellTool2(sessionManager);
        }

    }
}
