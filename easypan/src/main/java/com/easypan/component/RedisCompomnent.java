package com.easypan.component;

import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.DownloadFileDto;
import com.easypan.entity.dto.SysSettingsDto;
import com.easypan.entity.dto.UserSpaceDto;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.mappers.FileInfoMapper;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component("RedisCompomnent")
public class RedisCompomnent {


    @Resource
    private RedisUtils redisUtils;

    @Resource
    private FileInfoMapper<FileInfo, FileInfoQuery> fileInfoMapper;

    public SysSettingsDto getSysSettingDto() {
        SysSettingsDto sysSettingsDto = (SysSettingsDto) redisUtils.get(Constants.REDIS_KEY_SYS_SETTING);
        if (null == sysSettingsDto) {
            sysSettingsDto = new SysSettingsDto();
            redisUtils.set(Constants.REDIS_KEY_SYS_SETTING, sysSettingsDto);
        }
        return sysSettingsDto;
    }

    public void saveUserSpaceUse(String userId, UserSpaceDto userSpaceDto) {
        redisUtils.setex(Constants.REDIS_KEY_USER_SPACE_USE + userId, userSpaceDto, Constants.REDIS_KEY_EXPIRES_DAY);
    }

    public UserSpaceDto getUserSpaceUse(String userId) {
        UserSpaceDto spaceDto = (UserSpaceDto) redisUtils.get(Constants.REDIS_KEY_USER_SPACE_USE + userId);
        if (spaceDto == null) {
            spaceDto = new UserSpaceDto();
            Long userSpace = fileInfoMapper.selectUseSpace(userId);
            spaceDto.setUseSpace(userSpace);
            spaceDto.setTotalSpace(getSysSettingDto().getUserInitUseSpace() * Constants.MB);
            saveUserSpaceUse(userId, spaceDto);
        }
        return spaceDto;

    }

    public void saveFileTempSize(String userId, String FileId, Long fileSize) {
        Long currentSize = getFileTempSize(userId, FileId);
        redisUtils.setex(Constants.REDIS_KEY_USER_FILE_TEMP_SIZE + userId + FileId, currentSize + fileSize, Constants.REDIS_KEY_EXPIRES_ONE_HOUR);

    }

    // 获取临时文件大小
    public Long getFileTempSize(String userId, String FileId) {
        Long currentSize = getFileSizeFromRedis(Constants.REDIS_KEY_USER_FILE_TEMP_SIZE + userId + FileId);
        return currentSize;
    }

    private Long getFileSizeFromRedis(String key) {
        Object sizeObj = redisUtils.get(key);
        if (sizeObj == null) {
            return 0L;
        }
        if (sizeObj instanceof Integer) {
            return ((Integer) (sizeObj)).longValue();
        } else if (sizeObj instanceof Long) {
            return (Long) sizeObj;
        }
        return 0L;
    }

    public void saveDownloadCode(String code, DownloadFileDto downloadFileDto) {
        redisUtils.setex(Constants.REDIS_KEY_DOWNLOAD + code, downloadFileDto, Constants.REDIS_KEY_EXPIRES_FIVE_MIN);
    }

    public DownloadFileDto getDownloadCode(String code) {
        return (DownloadFileDto) redisUtils.get(Constants.REDIS_KEY_DOWNLOAD + code);
    }
}
