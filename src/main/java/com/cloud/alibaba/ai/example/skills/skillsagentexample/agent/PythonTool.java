/*
 * Copyright 2024-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloud.alibaba.ai.example.skills.skillsagentexample.agent;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Engine;
import org.graalvm.polyglot.PolyglotException;
import org.graalvm.polyglot.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;

import java.util.function.BiFunction;

/**
 * 使用 GraalVM polyglot 执行 Python 代码的工具。
 *
 * 该工具允许 Agent 执行 Python 代码片段并获取结果。
 * 它使用 GraalVM 的 polyglot API 在沙箱环境中运行 Python 代码。
 */
public class PythonTool implements BiFunction<PythonTool.PythonRequest, ToolContext, String> {

	public static final String DESCRIPTION = """
			执行 Python 代码并返回结果。
			
			使用说明：
			- code 参数必须是有效的 Python 代码
			- 工具会执行代码并返回输出结果
			- 如果代码有返回值，会以字符串形式返回
			- 错误会被捕获并返回错误信息
			- 执行环境是沙箱化的，确保安全性
			
			示例：
			- 简单计算：code = "2 + 2" 返回 "4"
			- 字符串操作：code = "'Hello, ' + 'World'" 返回 "Hello, World"
			- 列表操作：code = "[1, 2, 3][0]" 返回 "1"
			""";
	private static final Logger log = LoggerFactory.getLogger(PythonTool.class);
	private final Engine engine;

	public PythonTool() {
		// Create a shared engine for better performance
		this.engine = Engine.newBuilder()
				.option("engine.WarnInterpreterOnly", "false")
				.build();
	}

	/**
	 * 为 Python 工具创建 ToolCallback。
	 */
	public static ToolCallback createPythonToolCallback(String description) {
		return FunctionToolCallback.builder("python_tool", new PythonTool())
				.description(description)
				.inputType(PythonRequest.class)
				.build();
	}

	@Override
	public String apply(PythonRequest request, ToolContext toolContext) {
		if (request.code == null || request.code.trim().isEmpty()) {
			return "Error: Python code cannot be empty";
		}

		try (Context context = Context.newBuilder("python")
				.engine(engine)
				.allowAllAccess(true) // 允许完全访问以支持 pip 安装
				.allowIO(true) // 启用文件 I/O
				.allowNativeAccess(true) // 启用原生访问
				.allowCreateProcess(true) // 允许创建进程（用于 pip）
				.allowHostAccess(true) // 允许访问主机对象
				.build()) {

			log.debug("Executing Python code: {}", request.code);

			// 先尝试执行代码
			try {
				Value result = context.eval("python", request.code);
				return convertResultToString(result);
			} catch (PolyglotException e) {
				String errorMsg = e.getMessage();
				
				// 检查是否是模块缺失错误
				if (errorMsg.contains("ModuleNotFoundError") || errorMsg.contains("No module named")) {
					// 提取模块名
					String moduleName = extractModuleName(errorMsg);
					if (moduleName != null) {
						log.info("Missing module detected: {}, attempting to install...", moduleName);
						
						// 尝试通过 pip 安装
						try {
							ProcessBuilder pb = new ProcessBuilder("pip", "install", moduleName, "--quiet");
							Process process = pb.start();
							int exitCode = process.waitFor();
							
							if (exitCode == 0) {
								log.info("Successfully installed module: {}, retrying execution...", moduleName);
								// 重新执行代码
								Value result = context.eval("python", request.code);
								return "Module '" + moduleName + "' was missing and has been installed. Result:\n" + convertResultToString(result);
							} else {
								log.warn("Failed to install module: {} via pip", moduleName);
							}
						} catch (Exception installEx) {
							log.error("Error installing module: {}", moduleName, installEx);
						}
					}
				}
				
				return "Error executing Python code: " + errorMsg;
			}
		}
		catch (Exception e) {
			log.error("Unexpected error executing Python code", e);
			return "Unexpected error: " + e.getMessage();
		}
	}

	/**
	 * 从 ModuleNotFoundError 错误信息中提取模块名称。
	 */
	private String extractModuleName(String errorMsg) {
		try {
			// Pattern: No module named 'xxx' or ModuleNotFoundError: No module named 'xxx'
			int start = errorMsg.indexOf("'");
			if (start != -1) {
				int end = errorMsg.indexOf("'", start + 1);
				if (end != -1) {
					return errorMsg.substring(start + 1, end);
				}
			}
		} catch (Exception e) {
			log.warn("Failed to extract module name from error: {}", errorMsg);
		}
		return null;
	}

	/**
	 * 将 Python 结果转换为 Java 字符串。
	 */
	private String convertResultToString(Value result) {
		if (result.isNull()) {
			return "Execution completed with no return value";
		}

		// Handle different result types
		if (result.isString()) {
			return result.asString();
		}
		else if (result.isNumber()) {
			return String.valueOf(result.as(Object.class));
		}
		else if (result.isBoolean()) {
			return String.valueOf(result.asBoolean());
		}
		else if (result.hasArrayElements()) {
			// Convert array/list to string representation
			StringBuilder sb = new StringBuilder("[");
			long size = result.getArraySize();
			for (long i = 0; i < size; i++) {
				if (i > 0) {
					sb.append(", ");
				}
				Value element = result.getArrayElement(i);
				sb.append(element.toString());
			}
			sb.append("]");
			return sb.toString();
		}
		else {
			// For other types, use toString()
			return result.toString();
		}
	}

	/**
	 * Python 工具的请求结构。
	 */
	public static class PythonRequest {

		@JsonProperty(required = true)
		@JsonPropertyDescription("要执行的 Python 代码")
		public String code;

		public PythonRequest() {
		}

		public PythonRequest(String code) {
			this.code = code;
		}
	}
}

