package com.easypan.entity.config;


import com.easypan.utils.StringTools;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("appConfig")
public class AppConfig {
    @Value("${spring.mail.username}")
    private String sendUsername;

    public String getAdminEmails() {
        return adminEmails;
    }


    @Value("${admin.emails:}")
    private String adminEmails;

    public String getProjectFolder() {
        if (!StringTools.isEmpty(projectFolder) && !projectFolder.endsWith("/")) {
            projectFolder = projectFolder + "/";
        }
        return projectFolder;
    }

    @Value("${project.folder:}")
    private String projectFolder;

    public String getSendUsername() {
        return sendUsername;
    }

    @Value("${dev:false}")
    private Boolean dev;


    @Value("${qq.app.id:}")
    private String qqAppId;

    @Value("${qq.app.key:}")
    private String qqAppKey;


    @Value("${qq.url.authorization:}")
    private String qqUrlAuthorization;


    @Value("${qq.url.access.token:}")
    private String qqUrlAccessToken;


    @Value("${qq.url.openid:}")
    private String qqUrlOpenId;

    @Value("${qq.url.user.info:}")
    private String qqUrlUserInfo;

    @Value("${qq.url.redirect:}")
    private String qqUrlRedirect;


    public Boolean getDev() {
        return dev;
    }

    public String getQqAppId() {
        return qqAppId;
    }

    public String getQqAppKey() {
        return qqAppKey;
    }

    public String getQqUrlAuthorization() {
        return qqUrlAuthorization;
    }

    public String getQqUrlAccessToken() {
        return qqUrlAccessToken;
    }

    public String getQqUrlOpenId() {
        return qqUrlOpenId;
    }

    public String getQqUrlUserInfo() {
        return qqUrlUserInfo;
    }

    public String getQqUrlRedirect() {
        return qqUrlRedirect;
    }
}
