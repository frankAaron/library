package com.library.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;
import java.io.InputStream;
import java.util.Properties;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final Logger log = LoggerFactory.getLogger(WebMvcConfig.class);

    private static String resolveUploadPath() {
        String fromSys = System.getProperty("library.upload.path");
        if (fromSys != null && !fromSys.trim().isEmpty()) {
            return fromSys.trim();
        }
        try (InputStream is = WebMvcConfig.class.getClassLoader().getResourceAsStream("upload.properties")) {
            if (is != null) {
                Properties props = new Properties();
                props.load(is);
                String fromProps = props.getProperty("upload.path");
                if (fromProps != null && !fromProps.trim().isEmpty()) {
                    return fromProps.trim();
                }
            }
        } catch (Exception e) {
            log.warn("读取 upload.properties 失败：{}", e.getMessage());
        }
        String catalinaBase = System.getProperty("catalina.base");
        if (catalinaBase != null) {
            File parent = new File(catalinaBase).getParentFile();
            if (parent != null) {
                return parent.getAbsolutePath() + File.separator + "library-uploads";
            }
        }
        File parent = new File(System.getProperty("user.dir")).getParentFile();
        return (parent != null ? parent.getAbsolutePath() : System.getProperty("user.dir")) + File.separator + "library-uploads";
    }

    @jakarta.annotation.PostConstruct
    public void init() {
        log.info("========== WebMvcConfig 初始化 ==========");
        log.info("WebMvcConfig: 工作目录(user.dir)={}, Tomcat实例(catalina.base)={}",
                System.getProperty("user.dir"), System.getProperty("catalina.base"));
        String path = resolveUploadPath();
        log.info("WebMvcConfig: 解析到上传路径 = {}", path);
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String path = resolveUploadPath();
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String absolute = dir.getAbsolutePath().replace('\\', '/');
        if (!absolute.endsWith("/")) {
            absolute += "/";
        }
        log.info("WebMvcConfig 映射 /uploads/** → file:{}", absolute);
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + absolute);
    }
}