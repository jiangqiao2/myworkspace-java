package com.easypan.controller;

import com.easypan.component.RedisCompomnent;
import com.easypan.entity.config.AppConfig;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.DownloadFileDto;
import com.easypan.entity.enums.FileCategoryEnums;
import com.easypan.entity.enums.FileFolderTypeEnums;
import com.easypan.entity.enums.ResponseCodeEnum;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.exception.BusinessException;
import com.easypan.service.FileInfoService;
import com.easypan.utils.StringTools;
import org.apache.commons.lang3.StringUtils;
import org.apache.tomcat.util.bcel.Const;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.Response;
import java.io.File;
import java.net.URLEncoder;
import java.util.List;

public class CommonFileController extends ABaseController {

    @Resource
    private AppConfig appConfig;

    @Resource
    private FileInfoService fileInfoService;

    @Resource
    private RedisCompomnent redisCompomnent;

    public void getImage(HttpServletResponse response, String imageFolder, String imageName) {
        if (StringTools.isEmpty(imageFolder) || StringTools.isEmpty(imageName)) {
            return;
        }
        String imageSuffix = StringTools.getFileNameSuffix(imageName);
        String filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + imageFolder + "/" + imageName;
        imageSuffix = imageSuffix.replace(".", "");
        String contentType = "image/" + imageSuffix;
        response.setContentType(contentType);
        response.setHeader("Cache-Control", "max-age=259200");
        readFile(response, filePath);
    }

    protected void getFile(HttpServletResponse response, String fileId, String userId) {
        String filePath = null;

        if (fileId.endsWith(".ts")) {
            String[] tsArray = fileId.split("_");
            String realFileId = tsArray[0];
            FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(realFileId, userId);
            if (null == fileInfo) {
                return;
            }
            String fileName = fileInfo.getFilePath();
            fileName = StringTools.getFileNameNoSuffix(fileName) + "/" + fileId;
            filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileName;
        } else {
            FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(fileId, userId);
            if (null == fileInfo) {
                return;
            }
            if (FileCategoryEnums.VIDEO.getCategory().equals(fileInfo.getFileCategory())) {
                String fileNameNoSuffix = StringTools.getFileNameNoSuffix(fileInfo.getFilePath());
                filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileNameNoSuffix + "/" + Constants.M3U8_NAME;
            } else {
                filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fileInfo.getFilePath();
            }
            File file = new File(filePath);
            if (!file.exists()) {
                return;
            }
        }
        readFile(response, filePath);
    }

    protected ResponseVO getFolderInfo(String path, String userId) {
        String[] pathArray = path.split("/");
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        fileInfoQuery.setFileIdArray(pathArray);
        String orderBy = "field(file_id,\"" + StringUtils.join(pathArray, "\",\"") + "\")";
        fileInfoQuery.setOrderBy(orderBy);
        List<FileInfo> fileInfoList = fileInfoService.findListByParam(fileInfoQuery);
        return getSuccessResponseVO(fileInfoList);
    }

    protected ResponseVO createDownloadUrl(String fileId, String userId) {
        FileInfo fileInfo = fileInfoService.getFileInfoByFileIdAndUserId(fileId, userId);
        if (fileInfo == null) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        String code = StringTools.getRandomString(Constants.LENGTH_50);
        DownloadFileDto fileDto = new DownloadFileDto();
        fileDto.setDownloadCode(code);
        fileDto.setFilePath(fileInfo.getFilePath());
        fileDto.setFileName(fileInfo.getFileName());
        redisCompomnent.saveDownloadCode(code, fileDto);
        return getSuccessResponseVO(code);
    }

    protected void download(HttpServletRequest request, HttpServletResponse response, String code) throws Exception {
        DownloadFileDto downloadFileDto = redisCompomnent.getDownloadCode(code);
        if (null == downloadFileDto) {
            return;
        }
        String filePath = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + downloadFileDto.getFilePath();
        String fileName = downloadFileDto.getFileName();
        response.setContentType("application/x-msdownload; charset=UTF-8");
        if (request.getHeader("User-Agent").toLowerCase().indexOf("msie") > 0) {//IE浏览器
            fileName = URLEncoder.encode(fileName, "UTF-8");
        } else {
            fileName = new String(fileName.getBytes("UTF-8"), "ISO8859-1");
        }
        response.setHeader("Content-Disposition", "attachment;filename=\"" + fileName + "\"");
        readFile(response, filePath);

    }
}