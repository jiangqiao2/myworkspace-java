package com.easypan.service.impl;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import com.easypan.component.RedisCompomnent;
import com.easypan.entity.config.AppConfig;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.QQInfoDto;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.dto.SysSettingsDto;
import com.easypan.entity.dto.UserSpaceDto;
import com.easypan.entity.enums.UserStatusEnum;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.exception.BusinessException;
import com.easypan.mappers.FileInfoMapper;
import com.easypan.service.EmailCodeService;
import com.easypan.utils.JsonUtils;
import com.easypan.utils.OKHttpUtils;

import com.mysql.cj.log.Log;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.easypan.entity.enums.PageSize;
import com.easypan.entity.query.UserInfoQuery;
import com.easypan.entity.po.UserInfo;
import com.easypan.entity.vo.PaginationResultVO;
import com.easypan.entity.query.SimplePage;
import com.easypan.mappers.UserInfoMapper;
import com.easypan.service.UserInfoService;
import com.easypan.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;


/**
 * 业务接口实现
 */
@Service("userInfoService")

public class UserInfoServiceImpl implements UserInfoService {

    private static final Logger logger = LoggerFactory.getLogger(UserInfoServiceImpl.class);

    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;


    @Resource
    private EmailCodeService emailCodeService;


    @Resource
    private RedisCompomnent redisCompomnent;


    @Resource
    private AppConfig appConfig;


    @Resource
    private FileInfoMapper<FileInfo, FileInfoQuery> fileInfoMapper;

    /**
     * 根据条件查询列表
     */
    @Override
    public List<UserInfo> findListByParam(UserInfoQuery param) {
        return this.userInfoMapper.selectList(param);
    }

    /**
     * 根据条件查询列表
     */
    @Override
    public Integer findCountByParam(UserInfoQuery param) {
        return this.userInfoMapper.selectCount(param);
    }

    /**
     * 分页查询方法
     */
    @Override
    public PaginationResultVO<UserInfo> findListByPage(UserInfoQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<UserInfo> list = this.findListByParam(param);
        PaginationResultVO<UserInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    @Override
    public Integer add(UserInfo bean) {
        return this.userInfoMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<UserInfo> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.userInfoMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或者修改
     */
    @Override
    public Integer addOrUpdateBatch(List<UserInfo> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.userInfoMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 多条件更新
     */
    @Override
    public Integer updateByParam(UserInfo bean, UserInfoQuery param) {
        StringTools.checkParam(param);
        return this.userInfoMapper.updateByParam(bean, param);
    }

    /**
     * 多条件删除
     */
    @Override
    public Integer deleteByParam(UserInfoQuery param) {
        StringTools.checkParam(param);
        return this.userInfoMapper.deleteByParam(param);
    }

    /**
     * 根据UserId获取对象
     */
    @Override
    public UserInfo getUserInfoByUserId(String userId) {
        return this.userInfoMapper.selectByUserId(userId);
    }

    /**
     * 根据UserId修改
     */
    @Override
    public Integer updateUserInfoByUserId(UserInfo bean, String userId) {
        return this.userInfoMapper.updateByUserId(bean, userId);
    }

    /**
     * 根据UserId删除
     */
    @Override
    public Integer deleteUserInfoByUserId(String userId) {
        return this.userInfoMapper.deleteByUserId(userId);
    }

    /**
     * 根据Email获取对象
     */
    @Override
    public UserInfo getUserInfoByEmail(String email) {
        return this.userInfoMapper.selectByEmail(email);
    }

    /**
     * 根据Email修改
     */
    @Override
    public Integer updateUserInfoByEmail(UserInfo bean, String email) {
        return this.userInfoMapper.updateByEmail(bean, email);
    }

    /**
     * 根据Email删除
     */
    @Override
    public Integer deleteUserInfoByEmail(String email) {
        return this.userInfoMapper.deleteByEmail(email);
    }

    /**
     * 根据QqOpenId获取对象
     */
    @Override
    public UserInfo getUserInfoByQqOpenId(String qqOpenId) {
        return this.userInfoMapper.selectByQqOpenId(qqOpenId);
    }

    /**
     * 根据QqOpenId修改
     */
    @Override
    public Integer updateUserInfoByQqOpenId(UserInfo bean, String qqOpenId) {
        return this.userInfoMapper.updateByQqOpenId(bean, qqOpenId);
    }

    /**
     * 根据QqOpenId删除
     */
    @Override
    public Integer deleteUserInfoByQqOpenId(String qqOpenId) {
        return this.userInfoMapper.deleteByQqOpenId(qqOpenId);
    }

    /**
     * 根据NickName获取对象
     */
    @Override
    public UserInfo getUserInfoByNickName(String nickName) {
        return this.userInfoMapper.selectByNickName(nickName);
    }

    /**
     * 根据NickName修改
     */
    @Override
    public Integer updateUserInfoByNickName(UserInfo bean, String nickName) {
        return this.userInfoMapper.updateByNickName(bean, nickName);
    }

    /**
     * 根据NickName删除
     */
    @Override
    public Integer deleteUserInfoByNickName(String nickName) {
        return this.userInfoMapper.deleteByNickName(nickName);
    }

    /**
     * 注册
     *
     * @param email     邮箱
     * @param nickName  用户名
     * @param password  密码
     * @param emailCode 邮箱验证码
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(String email, String nickName, String password, String emailCode) {
        UserInfo userInfo = this.userInfoMapper.selectByEmail(email);
        if (null != userInfo) {
            throw new BusinessException("邮箱账号已经存在");
        }
        UserInfo nickNameUser = this.userInfoMapper.selectByNickName(nickName);
        if (null != nickNameUser) {
            throw new BusinessException("昵称已经存在");
        }

        emailCodeService.checkCode(email, emailCode);
        String userId = StringTools.getRandomString(Constants.LENGTH_10);
        userInfo = new UserInfo();
        userInfo.setUserId(userId);
        userInfo.setEmail(email);
        userInfo.setNickName(nickName);
        userInfo.setPassword(StringTools.encodeByMd5(password));
        userInfo.setJoinTime(new Date());
        userInfo.setStatus(UserStatusEnum.ENABLE.getStatus());
        userInfo.setUseSpace(0L);
        SysSettingsDto sysSettingsDto = redisCompomnent.getSysSettingDto();
        userInfo.setTotalSpace(sysSettingsDto.getUserInitUseSpace() * Constants.MB);
        this.userInfoMapper.insert(userInfo);
    }

    /**
     * 登录
     *
     * @param email    邮箱
     * @param password 密码
     * @return
     */
    @Override
    public SessionWebUserDto login(String email, String password) {
        UserInfo userInfo = this.userInfoMapper.selectByEmail(email);
        if (null == userInfo || !userInfo.getPassword().equals(password)) {
            throw new BusinessException("账号或者密码错误");
        }
        if (UserStatusEnum.DISABLE.getStatus().equals(userInfo.getStatus())) {
            throw new BusinessException("账号已禁用");
        }
        UserInfo updateuserInfo = new UserInfo();
        //更新修改时间
        updateuserInfo.setLastLoginTime(new Date());
        this.userInfoMapper.updateByUserId(updateuserInfo, userInfo.getUserId());
        SessionWebUserDto sessionWebUserDto = new SessionWebUserDto();
        sessionWebUserDto.setNickName(userInfo.getNickName());
        sessionWebUserDto.setUserId(userInfo.getUserId());
        if (ArrayUtils.contains(appConfig.getAdminEmails().split(","), email)) {
            sessionWebUserDto.setAdmin(true);
        } else sessionWebUserDto.setAdmin(false);
        UserSpaceDto userSpaceDto = new UserSpaceDto();
        Long userSpace = fileInfoMapper.selectUseSpace(userInfo.getUserId());
        userSpaceDto.setUserSpace(userSpace);
        userSpaceDto.setTotalSpace(userInfo.getTotalSpace());
        redisCompomnent.saveUserSpaceUse(userInfo.getUserId(), userSpaceDto);
        return sessionWebUserDto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void restPwd(String email, String passwd, String emailCode) {
        UserInfo userInfo = this.userInfoMapper.selectByEmail(email);
        if (null == userInfo) {
            throw new BusinessException("邮箱账号不存在");
        }
        emailCodeService.checkCode(email, emailCode);

        UserInfo updateInfo = new UserInfo();
        updateInfo.setPassword(StringTools.encodeByMd5(passwd));
        this.userInfoMapper.updateByEmail(updateInfo, email);
    }

    @Override
    public SessionWebUserDto qqLogin(String code) {
        String accessToken = getQQAccessToken(code);
        String openID = getQQopenID(accessToken);
        UserInfo user = this.userInfoMapper.selectByUserId(openID);
        String avatar = null;
        if (null == user) {
            // 自动注册
            QQInfoDto qqInfoDto = getQQUserInfo(accessToken, openID);
            user = new UserInfo();
            String nickName = qqInfoDto.getNickname();
            nickName = nickName.length() > Constants.LENGTH_20 ? nickName.substring(0, Constants.LENGTH_20) : nickName;
            avatar = StringTools.isEmpty(qqInfoDto.getFigureurl_qq_2()) ? qqInfoDto.getFigureurl_qq_1() : qqInfoDto.getFigureurl_qq_2();
            Date curDate = new Date();
            user.setQqOpenId(openID);
            user.setNickName(nickName);
            user.setJoinTime(curDate);
            user.setQqAvatar(avatar);
            user.setUserId(StringTools.getRandomNumber(Constants.LENGTH_10));
            user.setLastLoginTime(curDate);
            user.setStatus(UserStatusEnum.ENABLE.getStatus());
            user.setUseSpace(0L);
            user.setTotalSpace(redisCompomnent.getSysSettingDto().getUserInitUseSpace() * Constants.MB);
            this.userInfoMapper.insert(user);
            user = userInfoMapper.selectByQqOpenId(openID);
        } else {
            UserInfo updateInfo = new UserInfo();
            updateInfo.setLastLoginTime(new Date());
            avatar = user.getQqAvatar();
            this.userInfoMapper.updateByUserId(updateInfo, openID);
        }
        SessionWebUserDto sessionWebUserDto = new SessionWebUserDto();
        sessionWebUserDto.setUserId(user.getUserId());
        sessionWebUserDto.setNickName(user.getNickName());
        sessionWebUserDto.setAvatar(avatar);
        if (ArrayUtils.contains(appConfig.getAdminEmails().split(","), user.getEmail() == null ? "" : user.getEmail())) {
            sessionWebUserDto.setAdmin(true);
        } else sessionWebUserDto.setAdmin(false);
        UserSpaceDto userSpaceDto = new UserSpaceDto();
        Long userSpacce = fileInfoMapper.selectUseSpace(user.getUserId());
        userSpaceDto.setUserSpace(userSpacce);
        userSpaceDto.setTotalSpace(user.getTotalSpace());
        redisCompomnent.saveUserSpaceUse(user.getUserId(), userSpaceDto);
        return sessionWebUserDto;
    }

    private String getQQAccessToken(String code) {
        String accessToken = null;
        String url = null;
        try {
            url = String.format(appConfig.getQqUrlAccessToken(), appConfig.getQqAppId(), appConfig.getQqAppKey(), code, URLEncoder.encode(appConfig.getQqUrlRedirect(), "utf-8"));
        } catch (UnsupportedEncodingException e) {
            logger.error("encode失败");
        }
        String tokenResult = OKHttpUtils.getRequest(url);
        if (tokenResult == null || tokenResult.indexOf(Constants.VIEW_OBJ_RESULT_KEY) != -1) {
            logger.error("获取qqToken失败,{}", tokenResult);
            throw new BusinessException("获取qq头像失败");
        }
        String[] parms = tokenResult.split("&");
        if (parms != null && parms.length > 0) {
            for (String p : parms) {
                if (p.indexOf("access_token") != -1) {
                    accessToken = p.split("=")[1];
                    break;
                }
            }
        }
        return accessToken;
    }

    private String getQQopenID(String accessToken) {
        String url = String.format(appConfig.getQqUrlOpenId(), accessToken);
        String openUrlIDResult = OKHttpUtils.getRequest(url);
        String tmpJson = this.getQQresp(openUrlIDResult);
        if (tmpJson == null) {
            logger.error("调qq接口获取openId失败：tempJson", tmpJson);
            throw new BusinessException("调qq接口获取openID失败");
        }
        Map jsonData = JsonUtils.convertJson2Obj(tmpJson, Map.class);
        if (jsonData == null || jsonData.containsKey(Constants.VIEW_OBJ_RESULT_KEY)) {
            logger.error("调qq接口获取openId失败");
        }
        return String.valueOf(jsonData.get("openid"));
    }

    private String getQQresp(String openUrlIDResult) {
        if (StringUtils.isNoneBlank("callback")) {
            int pos = openUrlIDResult.indexOf("callback");
            if (pos != -1) {
                int start = openUrlIDResult.indexOf("(");
                int end = openUrlIDResult.lastIndexOf(")");
                String jsonStr = openUrlIDResult.substring(start + 1, end - 1);
                return jsonStr;
            }
        }
        return null;
    }

    private QQInfoDto getQQUserInfo(String accessToken, String qqOpenId) {
        String url = String.format(appConfig.getQqUrlUserInfo(), accessToken, appConfig.getQqAppId(), qqOpenId);
        String response = OKHttpUtils.getRequest(url);
        if (StringUtils.isNoneBlank(response)) {
            QQInfoDto qqInfoDto = JsonUtils.convertJson2Obj(response, QQInfoDto.class);
            if (qqInfoDto.getRet() != 0) {
                logger.error("qqinfo,{}", response);
                throw new BusinessException("调qq接口获取用户信息异常");
            }
            return qqInfoDto;
        }
        throw new BusinessException("调qq接口获取用户信息异常");
    }
}