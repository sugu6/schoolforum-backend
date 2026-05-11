package com.example.schoolforum.util;

import com.mybatisflex.codegen.Generator;
import com.mybatisflex.codegen.config.GlobalConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class InteractiveCodeGenerator {

    private static final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    public static void main(String[] args) {
        try {
            HikariDataSource dataSource = new HikariDataSource();
            dataSource.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/schoolforum?characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true");
            dataSource.setUsername("root");
            dataSource.setPassword("123456");
            dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");

            System.out.print("请输入作者名称: ");
            String author = reader.readLine().trim();

            System.out.print("请输入要生成的表名 (多个用逗号分隔, 输入 all 生成所有表): ");
            String input = reader.readLine().trim();
            Set<String> tables = getTables(input);

            GlobalConfig globalConfig = new GlobalConfig();

            globalConfig.setBasePackage("com.example.schoolforum");
            globalConfig.setSourceDir(System.getProperty("user.dir") + "/src/main/java");

            globalConfig.setEntityPackage("com.example.schoolforum.pojo");
            globalConfig.setMapperPackage("com.example.schoolforum.mapper");
            globalConfig.setServicePackage("com.example.schoolforum.service");
            globalConfig.setServiceImplPackage("com.example.schoolforum.service.impl");
            globalConfig.setControllerPackage("com.example.schoolforum.controller");

            globalConfig.setEntityGenerateEnable(true);
            globalConfig.setEntityWithLombok(true);
            globalConfig.setEntityJdkVersion(17);
            globalConfig.setEntityWithSwagger(false);
            globalConfig.setEntityOverwriteEnable(true);

            globalConfig.setMapperGenerateEnable(true);
            globalConfig.setMapperAnnotation(true);

            globalConfig.setServiceGenerateEnable(true);
            globalConfig.setServiceImplGenerateEnable(true);
            globalConfig.setControllerGenerateEnable(true);

            globalConfig.setAuthor(author);
            globalConfig.setGenerateTables(tables);

            Generator generator = new Generator(dataSource, globalConfig);
            generator.generate();

            System.out.println("代码生成完成！");

        } catch (Exception e) {
            System.out.println("发生错误: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Set<String> getTables(String tables) {
        return "all".equals(tables) ? Collections.emptySet() : new HashSet<>(Arrays.asList(tables.split(",")));
    }
}
