package com.easypan.controller;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.annotation.VerifyParam;
import com.easypan.component.RedisCompomnent;
import com.easypan.entity.config.AppConfig;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.CreateImageCode;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.dto.UserSpaceDto;
import com.easypan.entity.enums.VerifyRegexEnum;
import com.easypan.entity.po.UserInfo;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.exception.BusinessException;
import com.easypan.service.EmailCodeService;
import com.easypan.service.UserInfoService;
import com.easypan.utils.StringTools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;


@RestController("accountController")
public class AccountController extends ABaseController {
    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    private static final String CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_VALUE = "application/json;charset=UTF-8";


    @Resource
    private UserInfoService userInfoService;

    @Resource
    private EmailCodeService emailCodeService;

    @Resource
    private AppConfig appConfig;

    @Resource
    private RedisCompomnent redisComponent;

    /**
     * 验证码
     * 图片验证码不正确，需要把session删除本次图片验证码的记录，再次重新获取图片验证码
     *
     * @param response
     * @param session  将图片验证码存在session里面，以便后面判断图片验证码是否一致
     * @param type     0 表示登录注册， 1表示邮箱验证码发送
     * @throws IOException
     */
    @RequestMapping(value = "/checkCode")
    public void checkCode(HttpServletResponse response, HttpSession session, Integer type) throws IOException {
        // 创建验证码图片
        // 通过response返回给前端
        CreateImageCode vCode = new CreateImageCode(130, 38, 5, 10);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/jpeg");
        String code = vCode.getCode();
        if (type == null || type == 0) {
            session.setAttribute(Constants.CHECK_CODE_KEY, code);
        } else {
            session.setAttribute(Constants.CHECK_CODE_KEY_EMAIL, code);
        }
        vCode.write(response.getOutputStream());
    }

    /**
     * 发送邮箱验证码
     *
     * @param session   用于检查图片验证码是否一致
     * @param email     邮箱
     * @param checkCode 用户输入的图片验证码
     * @param type
     * @return
     */
    @RequestMapping("/sendEmailCode")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public ResponseVO sendEmailCode(HttpSession session, @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL, max = 150) String email, @VerifyParam(required = true) String checkCode, @VerifyParam(required = true) Integer type) {
        try {
            // 检验图片验证码是否正确
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY_EMAIL))) {
                throw new BusinessException("图片验证码不正确");
            }
            // 发送邮箱验证码
            emailCodeService.sendEmailCode(email, type);
            return getSuccessResponseVO(null);
        } finally {
            // 图片验证码不正确，重新获取验证码
            session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
        }
    }

    /**
     * 注册
     *
     * @param session   图片验证码信息
     * @param email     邮箱
     * @param nickName  用户名
     * @param password  密码
     * @param checkCode 图片验证码
     * @param emailCode 邮箱验证码
     * @return
     */
    @RequestMapping("/register")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public ResponseVO register(HttpSession session, @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL, max = 150) String email,

                               @VerifyParam(required = true, max = 20) String nickName, @VerifyParam(required = true, regex = VerifyRegexEnum.PASSWORD, min = 8, max = 18) String password,

                               @VerifyParam(required = true) String checkCode, @VerifyParam(required = true) String emailCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new BusinessException("图片验证码不正确");
            }
            // 注册
            userInfoService.register(email, nickName, password, emailCode);
            return getSuccessResponseVO(null);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    /**
     * 登录之后，将session、存储此时用户信息
     *
     * @param session   图片验证码信息
     * @param email     邮箱
     * @param password  密码
     * @param checkCode 图片验证码
     * @return
     */
    @RequestMapping("/login")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public ResponseVO login(HttpSession session, @VerifyParam(required = true) String email, @VerifyParam(required = true) String password, @VerifyParam(required = true) String checkCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new BusinessException("图片验证码不正确");
            }
            SessionWebUserDto sessionWebUserDto = userInfoService.login(email, password);
            session.setAttribute(Constants.SESSION_KEY, sessionWebUserDto);
            return getSuccessResponseVO(sessionWebUserDto);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    /**
     * 重置密码
     *
     * @param session   图片验证码信息
     * @param email     邮箱
     * @param password  密码
     * @param checkCode 图片验证码
     * @param emailCode 邮箱验证码
     * @return
     */
    @RequestMapping("/resetPwd")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public ResponseVO resetPwd(HttpSession session, @VerifyParam(required = true, regex = VerifyRegexEnum.EMAIL, max = 150) String email, @VerifyParam(required = true, regex = VerifyRegexEnum.PASSWORD, min = 8, max = 18) String password, @VerifyParam(required = true) String checkCode, @VerifyParam(required = true) String emailCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new BusinessException("图片验证码不正确");
            }
            userInfoService.restPwd(email, password, emailCode);
            return getSuccessResponseVO(null);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    /**
     * 得到用户头像，没有设置头像使用默认头像
     *
     * @param response 给前端返回的数据
     * @param userId   用户名
     */
    @RequestMapping("/getAvatar/{userId}")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public void getAvatar(HttpServletResponse response, @VerifyParam(required = true) @PathVariable("userId") String userId) {
        String avatarFolderName = Constants.FILE_FOLDER_FILE + Constants.FILE_FOLDER_AVATAR_NAME;
        File folder = new File(appConfig.getProjectFolder() + avatarFolderName);
        // 存放头像的文件目录
        if (!folder.exists()) {
            folder.mkdirs();
        }
        String avatarPath = appConfig.getProjectFolder() + avatarFolderName + userId + Constants.AVATAR_SUFFIX;
        File file = new File(avatarPath);
        if (!file.exists()) {
            if (!new File(appConfig.getProjectFolder() + avatarFolderName + Constants.AVATAR_DEFUALT).exists()) {
                printNoDefaultImage(response);
                return;
            }
            avatarPath = appConfig.getProjectFolder() + avatarFolderName + Constants.AVATAR_DEFUALT;
        }
        response.setContentType("image/jpg");
        readFile(response, avatarPath);
    }

    private void printNoDefaultImage(HttpServletResponse response) {
        response.setHeader(CONTENT_TYPE, CONTENT_TYPE_VALUE);
        response.setStatus(HttpStatus.OK.value());
        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            writer.print("请在头像目录下放置默认头像default_avatar.jpg");
            writer.close();
        } catch (Exception e) {
            logger.error("输出无默认图失败", e);
        } finally {
            writer.close();
        }
    }

    /**
     * 得到用户信息
     *
     * @param session 登录之后返回的session
     * @return
     */
    @RequestMapping("/getUserInfo")
    @GlobalInterceptor
    public ResponseVO getUserInfo(HttpSession session) {
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        return getSuccessResponseVO(sessionWebUserDto);
    }

    @RequestMapping("/getUseSpace")
    @GlobalInterceptor
    public ResponseVO getUseSpace(HttpSession session) {
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        UserSpaceDto userSpaceUse = redisComponent.getUserSpaceUse(sessionWebUserDto.getUserId());
        return getSuccessResponseVO(userSpaceUse);
    }

    @RequestMapping("/logout")
    public ResponseVO logout(HttpSession session) {
        session.invalidate();
        return getSuccessResponseVO(null);
    }

    /**
     * 更换头像
     * @param session
     * @param avatar 图片文件
     * @return
     */
    @RequestMapping("/updateUserAvatar")
    @GlobalInterceptor
    public ResponseVO updateUserAvatar(HttpSession session, MultipartFile avatar) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        String baseFolder = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
        File targetFileFolder = new File(baseFolder + Constants.FILE_FOLDER_AVATAR_NAME);
        if (!targetFileFolder.exists()) {
            targetFileFolder.mkdirs();
        }
        File targetFile = new File(targetFileFolder.getPath() + "/" + webUserDto.getUserId() + Constants.AVATAR_SUFFIX);
        try {
            avatar.transferTo(targetFile);
        } catch (Exception e) {
            logger.error("上传头像失败", e);
        }
        UserInfo userInfo = new UserInfo();
        userInfo.setQqAvatar("");
        userInfoService.updateUserInfoByUserId(userInfo, webUserDto.getUserId());
        webUserDto.setAvatar(null);
        session.setAttribute(Constants.SESSION_KEY, webUserDto);
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/updatePassword")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO updatePassword(HttpSession session, @VerifyParam(required = true, regex = VerifyRegexEnum.PASSWORD, min = 8, max = 18) String password) {
        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        UserInfo userInfo = new UserInfo();
        userInfo.setPassword(StringTools.encodeByMd5(password));
        userInfoService.updateUserInfoByUserId(userInfo, sessionWebUserDto.getUserId());
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/qqlogin")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public ResponseVO qqlogin(HttpSession session, String callbackUrl) throws UnsupportedEncodingException {
        String state = StringTools.getRandomNumber(Constants.LENGTH_30);
        if (!StringTools.isEmpty(callbackUrl)) {
            session.setAttribute(state, callbackUrl);
        }
        String url = String.format(appConfig.getQqUrlAuthorization(), appConfig.getQqAppId(), URLEncoder.encode(appConfig.getQqUrlRedirect(), "utf-8"), state);
        return getSuccessResponseVO(url);
    }

    @RequestMapping("/qqlogin/callback")
    @GlobalInterceptor(checkLogin = false, checkParams = true)
    public ResponseVO qqLoginCallback(HttpSession session, @VerifyParam(required = true) String code, @VerifyParam(required = true) String state) {
        SessionWebUserDto sessionWebUserDto = userInfoService.qqLogin(code);
        session.setAttribute(Constants.SESSION_KEY, sessionWebUserDto);
        Map<String, Object> result = new HashMap<>();
        result.put("callbackUrl", session.getAttribute(state));
        result.put("userInfo", sessionWebUserDto);
        return getSuccessResponseVO(result);
    }
}
