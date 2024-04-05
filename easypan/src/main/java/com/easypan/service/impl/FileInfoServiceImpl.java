package com.easypan.service.impl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import com.easypan.component.RedisCompomnent;
import com.easypan.component.RedisUtils;
import com.easypan.entity.config.AppConfig;
import com.easypan.entity.constants.Constants;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.dto.UploadResultDto;
import com.easypan.entity.dto.UserSpaceDto;
import com.easypan.entity.enums.*;
import com.easypan.entity.po.UserInfo;
import com.easypan.entity.query.UserInfoQuery;
import com.easypan.exception.BusinessException;
import com.easypan.mappers.UserInfoMapper;
import com.easypan.utils.DateUtil;
import com.easypan.utils.ProcessUtils;
import com.easypan.utils.ScaleFilter;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.easypan.entity.query.FileInfoQuery;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.vo.PaginationResultVO;
import com.easypan.entity.query.SimplePage;
import com.easypan.mappers.FileInfoMapper;
import com.easypan.service.FileInfoService;
import com.easypan.utils.StringTools;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;


/**
 * 文件信息表 业务接口实现
 */
@Service("fileInfoService")

public class FileInfoServiceImpl implements FileInfoService {

    @Resource
    private FileInfoMapper<FileInfo, FileInfoQuery> fileInfoMapper;

    @Resource
    private RedisUtils redisUtils;
    @Resource
    RedisCompomnent redisCompomnent;

    @Resource
    private UserInfoMapper<UserInfo, UserInfoQuery> userInfoMapper;


    @Resource
    private AppConfig appConfig;

    @Resource
    @Lazy
    private FileInfoServiceImpl fileInfoService;

    private static final Logger logger = LoggerFactory.getLogger(FileInfoServiceImpl.class);

    /**
     * 根据条件查询列表
     */
    @Override
    public List<FileInfo> findListByParam(FileInfoQuery param) {
        List<FileInfo> fileInfos = this.fileInfoMapper.selectList(param);
        for (FileInfo fileInfo : fileInfos)
            logger.info("文件名：{}\n", fileInfo.getFileName());
        return fileInfos;
    }

    /**
     * 根据条件查询列表
     */
    @Override
    public Integer findCountByParam(FileInfoQuery param) {
        return this.fileInfoMapper.selectCount(param);
    }

    /**
     * 分页查询方法
     */
    @Override
    public PaginationResultVO<FileInfo> findListByPage(FileInfoQuery param) {
        int count = this.findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<FileInfo> list = this.findListByParam(param);
        PaginationResultVO<FileInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    /**
     * 新增
     */
    @Override
    public Integer add(FileInfo bean) {
        return this.fileInfoMapper.insert(bean);
    }

    /**
     * 批量新增
     */
    @Override
    public Integer addBatch(List<FileInfo> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.fileInfoMapper.insertBatch(listBean);
    }

    /**
     * 批量新增或者修改
     */
    @Override
    public Integer addOrUpdateBatch(List<FileInfo> listBean) {
        if (listBean == null || listBean.isEmpty()) {
            return 0;
        }
        return this.fileInfoMapper.insertOrUpdateBatch(listBean);
    }

    /**
     * 多条件更新
     */
    @Override
    public Integer updateByParam(FileInfo bean, FileInfoQuery param) {
        StringTools.checkParam(param);
        return this.fileInfoMapper.updateByParam(bean, param);
    }

    /**
     * 多条件删除
     */
    @Override
    public Integer deleteByParam(FileInfoQuery param) {
        StringTools.checkParam(param);
        return this.fileInfoMapper.deleteByParam(param);
    }

    /**
     * 根据FileIdAndUserId获取对象
     */
    @Override
    public FileInfo getFileInfoByFileIdAndUserId(String fileId, String userId) {
        return this.fileInfoMapper.selectByFileIdAndUserId(fileId, userId);
    }

    /**
     * 根据FileIdAndUserId修改
     */
    @Override
    public Integer updateFileInfoByFileIdAndUserId(FileInfo bean, String fileId, String userId) {
        return this.fileInfoMapper.updateByFileIdAndUserId(bean, fileId, userId);
    }

    /**
     * 根据FileIdAndUserId删除
     */
    @Override
    public Integer deleteFileInfoByFileIdAndUserId(String fileId, String userId) {
        return this.fileInfoMapper.deleteByFileIdAndUserId(fileId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadResultDto updaloadFile(SessionWebUserDto sessionWebUserDto, String fileId, MultipartFile file, String fileName, String filePid, String fileMd5, Integer chunkIndex, Integer chunks) {
        File tempFileFolder = null;
        Boolean uploadSuccess = true;
        try {
            UploadResultDto resultDto = new UploadResultDto();
            if (StringTools.isEmpty(fileId)) {
                fileId = StringTools.getRandomNumber(Constants.LENGTH_10);
            }
            resultDto.setFileId(fileId);
            Date curDate = new Date();
            UserSpaceDto spaceDto = redisCompomnent.getUserSpaceUse(sessionWebUserDto.getUserId());
            if (chunkIndex == 0) {
                FileInfoQuery infoQuery = new FileInfoQuery();
                infoQuery.setFileMd5(fileMd5);
                infoQuery.setSimplePage(new SimplePage(0, 1));
                infoQuery.setStatus(FileStatusEnums.USING.getStatus());
                List<FileInfo> dbFileList = this.fileInfoMapper.selectList(infoQuery);
                // 数据库已经有这个文件，直接秒传
                if (!dbFileList.isEmpty()) {
                    FileInfo dbFile = dbFileList.get(0);
                    // 判断文件使用的大小
                    if (dbFile.getFileSize() + spaceDto.getUseSpace() > spaceDto.getTotalSpace()) {
                        throw new BusinessException(ResponseCodeEnum.CODE_904);
                    }
                    dbFile.setFileId(fileId);
                    dbFile.setFilePid(filePid);
                    dbFile.setUserId(sessionWebUserDto.getUserId());
                    dbFile.setCreateTime(curDate);
                    dbFile.setLastUpdateTime(curDate);
                    dbFile.setStatus(FileStatusEnums.USING.getStatus());
                    dbFile.setDelFlag(FileDelFlagEnums.USING.getFlag());
                    dbFile.setFileMd5(fileMd5);
                    // 文件重命名
                    fileName = autoRename(filePid, sessionWebUserDto.getUserId(), fileName);

                    dbFile.setFileName(fileName);
                    this.fileInfoMapper.insert(dbFile);
                    resultDto.setStatus(UploadStatusEnums.UPLOAD_SECONDS.getCode());
                    updateUserSpace(sessionWebUserDto, dbFile.getFileSize());
                    return resultDto;
                }
            }

            //暂存临时目录
            String tempFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_TEMP;
            //可能为空
            if (!new File(tempFolderName).exists()) {
                new File(tempFolderName).mkdir();
            }
            String currentUserFolderName = sessionWebUserDto.getUserId() + fileId;
            tempFileFolder = new File(tempFolderName + currentUserFolderName);
            if (!tempFileFolder.exists()) {
                tempFileFolder.mkdir();
            }
            logger.info("文件名字,{}", tempFolderName);
            logger.info("文件江桥:{}", tempFileFolder.getPath());
            // 判断磁盘空间
            Long currentTempSize = redisCompomnent.getFileTempSize(sessionWebUserDto.getUserId(), fileId);
            if (file.getSize() + currentTempSize > spaceDto.getTotalSpace()) {
                throw new BusinessException(ResponseCodeEnum.CODE_904);
            }
            File newFile = new File(tempFileFolder.getPath() + "/" + chunkIndex);

            file.transferTo(newFile);
            if (chunkIndex < chunks - 1) {
                resultDto.setStatus(UploadStatusEnums.UPLOADING.getCode());
                //保存临时大小
                redisCompomnent.saveFileTempSize(sessionWebUserDto.getUserId(), fileId, file.getSize());
                return resultDto;
            }
            redisCompomnent.saveFileTempSize(sessionWebUserDto.getUserId(), fileId, file.getSize());
            //最后一个分片上传完成，记录数据库，异步合并分片
            String month = DateUtil.format(new Date(), DateTimePatternEnum.YYYY_MM.getPattern());
            String fileSuffix = StringTools.getFileNameSuffix(fileName);
            //真实文件名
            String realFileName = currentUserFolderName + fileSuffix;
            FileTypeEnums fileTypeEnums = FileTypeEnums.getFileTypeBySuffix(fileSuffix);
            //自动重命名
            fileName = autoRename(filePid, sessionWebUserDto.getUserId(), fileName);
            FileInfo fileInfo = new FileInfo();
            fileInfo.setFileId(fileId);
            fileInfo.setUserId(sessionWebUserDto.getUserId());
            fileInfo.setFileName(fileName);
            fileInfo.setFileMd5(fileMd5);
            fileInfo.setFilePath(month + "/" + realFileName);
            fileInfo.setFilePid(filePid);
            fileInfo.setCreateTime(curDate);
            fileInfo.setLastUpdateTime(curDate);
            fileInfo.setFileCategory(fileTypeEnums.getCategory().getCategory());
            fileInfo.setFileType(fileTypeEnums.getType());
            fileInfo.setStatus(FileStatusEnums.TRANSFER.getStatus());
            fileInfo.setFolderType(FileFolderTypeEnums.FILE.getType());
            fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
            this.fileInfoMapper.insert(fileInfo);
            //拿到用户总空间大小
            Long totalSize = redisCompomnent.getFileTempSize(sessionWebUserDto.getUserId(), fileId);
            updateUserSpace(sessionWebUserDto, totalSize);
            resultDto.setStatus(UploadStatusEnums.UPLOAD_FINISH.getCode());
            //添加到一个事务里面
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileInfoService.transferFile(fileInfo.getFileId(), sessionWebUserDto);
                }
            });
            return resultDto;
        } catch (BusinessException e) {
            uploadSuccess = false;
            logger.error("文件上传失败");
            throw e;
        } catch (Exception e) {
            uploadSuccess = false;
            logger.error("文件上传失败", e);
            throw new BusinessException("文件上传失败");

        } finally {
            if (!uploadSuccess && tempFileFolder != null) {
                try {
                    FileUtils.deleteDirectory(tempFileFolder);
                } catch (Exception e) {
                    logger.error("删除临时目录失败");
                }
            }
        }
    }

    /**
     * 文件转码，将切片文件合并
     * 同时异步请求，加快速度
     *
     * @param fileId
     * @param webUserDto
     */
    @Async
    public void transferFile(String fileId, SessionWebUserDto webUserDto) {
        Boolean transferSuccess = true;
        String targetFilePath = null;
        String cover = null;
        FileTypeEnums fileTypeEnums = null;
        FileInfo fileInfo = this.fileInfoMapper.selectByFileIdAndUserId(fileId, webUserDto.getUserId());
        try {
            //文件找不到，或者不是在转码中，就不用处理了
            if (fileInfo == null || !FileStatusEnums.TRANSFER.getStatus().equals(fileInfo.getStatus())) {
                return;
            }
            String tempFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_TEMP;
            String currentUserFolderName = webUserDto.getUserId() + fileId;
            File fileFolder = new File(tempFolderName + currentUserFolderName);
            String fileSuffix = StringTools.getFileNameSuffix(fileInfo.getFileName());
            String month = DateUtil.format(fileInfo.getCreateTime(), DateTimePatternEnum.YYYY_MM.getPattern());
            //目标目录
            String targetFolderName = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
            File targetFolder = new File(targetFolderName + "/" + month);
            if (!targetFolder.exists()) {
                targetFolder.mkdir();
            }
            //真实的文件名
            String realFileName = currentUserFolderName + fileSuffix;
            targetFilePath = targetFolder.getPath() + "/" + realFileName;
            // 合并文件
            union(fileFolder.getPath(), targetFilePath, fileInfo.getFileName(), true);
            // 视频文件切割

            fileTypeEnums = FileTypeEnums.getFileTypeBySuffix(fileSuffix);
            if (FileTypeEnums.VIDEO == fileTypeEnums) {
                cutFile4Video(fileId, targetFilePath);
                //视频生成缩略图
                cover = month + "/" + currentUserFolderName + Constants.IMAGE_PNG_SUFFIX;
                String coverPath = targetFolderName + "/" + cover;
                ScaleFilter.createCover4Video(new File(targetFilePath), Constants.LENGTH_150, new File(coverPath));
            } else if (FileTypeEnums.IMAGE == fileTypeEnums) {
                //生成图片缩略图
                cover = month + "/" + realFileName.replace(".", "_.");
                String coverPath = targetFolderName + "/" + cover;
                Boolean created = ScaleFilter.createThumbnailWidthFFmpeg(new File(targetFilePath), Constants.LENGTH_150, new File(coverPath), false);
                if (!created) {
                    FileUtils.copyFile(new File(targetFilePath), new File(coverPath));
                }

            }
        } catch (Exception e) {
            logger.error("文件转码失败，文件ID:{},userId:{}", fileId, webUserDto.getUserId(), e);
            transferSuccess = false;
        } finally {
            FileInfo updateInfo = new FileInfo();
            updateInfo.setFileSize(new File(targetFilePath).length());
            updateInfo.setFileCover(cover);
            updateInfo.setStatus(transferSuccess ? FileStatusEnums.USING.getStatus() : FileStatusEnums.TRANSFER_FAIL.getStatus());
            fileInfoMapper.updateFileStatusWithOldStatus(fileId, webUserDto.getUserId(), updateInfo, FileStatusEnums.TRANSFER.getStatus());
        }
    }

    private void cutFile4Video(String fileId, String videoFilePath) {
        //创建同名切片目录
        File tsFolder = new File(videoFilePath.substring(0, videoFilePath.lastIndexOf(".")));
        if (!tsFolder.exists()) {
            tsFolder.mkdir();
        }
        final String CMD_TRANSFER_2TS = "ffmpeg -y -i %s  -vcodec copy -acodec copy -vbsf h264_mp4toannexb %s";
        final String CMD_CUT_TS = "ffmpeg -i %s -c copy -map 0 -f segment -segment_list %s -segment_time 30 %s/%s_%%4d.ts";
        String tsPath = tsFolder + "/" + Constants.TS_NAME;
        //生成.ts
        String cmd = String.format(CMD_TRANSFER_2TS, videoFilePath, tsPath);
        ProcessUtils.executeCommand(cmd, false);
        //生成索引文件.m3u8和切片.ts
        cmd = String.format(CMD_CUT_TS, tsPath, tsFolder.getPath() + "/" + Constants.M3U8_NAME, tsFolder.getPath(), fileId);
        ProcessUtils.executeCommand(cmd, false);
        new File(tsPath).delete();

    }

    private void union(String dirPath, String toFilePath, String fileName, Boolean delSource) {
        File dir = new File(dirPath);
        if (!dir.exists()) {
            throw new BusinessException("目录不存在");
        }
        File[] fileList = dir.listFiles();
        File targetFile = new File(toFilePath);
        RandomAccessFile writeFile = null;
        try {
            writeFile = new RandomAccessFile(targetFile, "rw");
            byte[] b = new byte[1024 * 10];
            for (int i = 0; i < fileList.length; i++) {
                int len = -1;
                File chunkFile = new File(dirPath + "/" + i);
                RandomAccessFile readFile = null;
                try {
                    readFile = new RandomAccessFile(chunkFile, "r");
                    while ((len = readFile.read(b)) != -1) {
                        writeFile.write(b, 0, len);
                    }
                } catch (Exception e) {
                    logger.error("合并分片失败");
                    throw new BusinessException("合并分片失败");
                } finally {
                    readFile.close();
                }
            }
        } catch (Exception e) {
            logger.error("合并文件:{}失败", fileName, e);
            throw new BusinessException("合并文件" + fileName + "出错了");
        } finally {
            if (null != writeFile) {
                try {
                    writeFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (delSource && dir.exists()) {
                try {
                    FileUtils.deleteDirectory(dir);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private String autoRename(String filePid, String userId, String fileName) {
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setFileId(filePid);
        fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
        fileInfoQuery.setFileName(fileName);
        Integer count = this.fileInfoMapper.selectCount(fileInfoQuery);
        if (count > 0) {
            fileName = StringTools.rename(fileName);
        }
        return fileName;
    }

    private void updateUserSpace(SessionWebUserDto sessionWebUserDto, Long useSpace) {
        Integer count = userInfoMapper.updateUserSpace(sessionWebUserDto.getUserId(), useSpace, null);
        if (count == 0) {
            throw new BusinessException(ResponseCodeEnum.CODE_904);
        }
        UserSpaceDto spaceDto = redisCompomnent.getUserSpaceUse(sessionWebUserDto.getUserId());
        spaceDto.setUseSpace(spaceDto.getUseSpace() + useSpace);
        redisCompomnent.saveUserSpaceUse(sessionWebUserDto.getUserId(), spaceDto);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfo newFolder(String filePid, String userId, String folderName) {
        checkFileName(filePid, userId, folderName, FileFolderTypeEnums.FOLDER.getType());
        Date date = new Date();
        FileInfo fileInfo = new FileInfo();
        fileInfo.setFileId(StringTools.getRandomString(Constants.LENGTH_10));
        fileInfo.setUserId(userId);
        fileInfo.setFilePid(filePid);
        fileInfo.setFileName(folderName);
        fileInfo.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        fileInfo.setCreateTime(date);
        fileInfo.setLastUpdateTime(date);
        fileInfo.setStatus(FileStatusEnums.USING.getStatus());
        fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
        this.fileInfoMapper.insert(fileInfo);
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setFilePid(filePid);
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setFileName(folderName);
        fileInfoQuery.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
        Integer count = this.fileInfoMapper.selectCount(fileInfoQuery);
        if (count > 1) {
            throw new BusinessException("文件夹" + folderName + "已经存在");
        }
        fileInfo.setFileName(folderName);
        fileInfo.setLastUpdateTime(date);
        return fileInfo;
    }

    /**
     * 判断同目录是有没有相同文件名
     *
     * @param filePid
     * @param userId
     * @param fileName
     * @param folderType
     */
    private void checkFileName(String filePid, String userId, String fileName, Integer folderType) {
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setFolderType(folderType);
        fileInfoQuery.setFileName(fileName);
        fileInfoQuery.setFilePid(filePid);
        fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
        Integer count = this.fileInfoMapper.selectCount(fileInfoQuery);
        if (count > 0) {
            throw new BusinessException("从目录下已经存在同名文件，请修改昵称");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfo rename(String fileId, String userId, String fileName) {
        FileInfo fileInfo = this.fileInfoMapper.selectByFileIdAndUserId(fileId, userId);
        if (null == fileInfo) {
            throw new BusinessException("文件不存在");
        }
        String filePid = fileInfo.getFilePid();
        checkFileName(filePid, userId, fileName, fileInfo.getFolderType());
        if (FileFolderTypeEnums.FILE.getType().equals(fileInfo.getFolderType())) {
            fileName = fileName + StringTools.getFileNameSuffix(fileInfo.getFileName());
        }
        Date date = new Date();
        FileInfo dnInfo = new FileInfo();
        dnInfo.setFileName(fileName);
        dnInfo.setLastUpdateTime(date);
        this.fileInfoMapper.updateByFileIdAndUserId(dnInfo, fileId, userId);
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setFilePid(filePid);
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setFileName(fileName);
        fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
        Integer count = this.fileInfoMapper.selectCount(fileInfoQuery);
        if (count > 1) {
            throw new BusinessException("文件名" + fileName + "已经存在");
        }
        fileInfo.setFileName(fileName);
        fileInfo.setLastUpdateTime(date);
        return fileInfo;
    }

    @Override
    public void changeFileFolder(String fileIds, String filePid, String userId) {
        if (fileIds.equals(filePid)) {
            throw new BusinessException(ResponseCodeEnum.CODE_600);
        }
        if (!Constants.ZERO_STR.equals(filePid)) {
            FileInfo fileInfo = this.getFileInfoByFileIdAndUserId(filePid, userId);
            if (fileInfo == null || FileDelFlagEnums.USING.getFlag().equals(fileInfo.getDelFlag())) {
                throw new BusinessException(ResponseCodeEnum.CODE_600);
            }
        }
        String[] fileIdArray = fileIds.split(",");
        FileInfoQuery query = new FileInfoQuery();
        query.setFilePid(filePid);
        query.setUserId(userId);
        List<FileInfo> dbFileList = fileInfoService.findListByParam(query);
        Map<String, FileInfo> dbFileNameMap = dbFileList.stream().collect(Collectors.toMap(FileInfo::getFileName, Function.identity(), (file1, file2) -> file2));
        // 查询选中的文件
        query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFileIdArray(fileIdArray);
        List<FileInfo> selectFileList = this.findListByParam(query);
        //将所选文件重命名
        for (FileInfo item : selectFileList) {
            FileInfo rootFileInfo = dbFileNameMap.get(item.getFileName());
            //文件名已经存在，重命名被还原的文件名
            FileInfo updateInfo = new FileInfo();
            if (rootFileInfo != null) {
                String fileName = StringTools.rename(item.getFileName());
                updateInfo.setFileName(fileName);
            }
            updateInfo.setFilePid(filePid);
            this.fileInfoMapper.updateByFileIdAndUserId(updateInfo, item.getFileId(), userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeFile2RecycleBatch(String userId, String fileIds) {
        String[] fileIdArray = fileIds.split(",");
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setUserId(userId);
        fileInfoQuery.setFileIdArray(fileIdArray);
        fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
        List<FileInfo> fileInfoList = this.fileInfoMapper.selectList(fileInfoQuery);
        if (fileInfoList.isEmpty()) {
            return;
        }
        List<String> delFilePidList = new ArrayList<>();
        for (FileInfo fileInfo : fileInfoList) {
            findAllsubFolderFileList(delFilePidList, userId, fileInfo.getFileId(), FileDelFlagEnums.USING.getFlag());
        }
        if (!delFilePidList.isEmpty()) {

            FileInfo updateInfo = new FileInfo();

            updateInfo.setDelFlag(FileDelFlagEnums.DEL.getFlag());
            this.fileInfoMapper.updateFileDelFlagBatch(updateInfo, userId, delFilePidList, null, FileDelFlagEnums.USING.getFlag());
        }
        //将选中的文件更新为回收站
        List<String> delFileList = Arrays.asList(fileIdArray);
        FileInfo fileInfo = new FileInfo();
        fileInfo.setRecoveryTime(new Date());
        fileInfo.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        this.fileInfoMapper.updateFileDelFlagBatch(fileInfo, userId, null, delFileList, FileDelFlagEnums.USING.getFlag());
    }

    private void findAllsubFolderFileList(List<String> fileIdList, String userId, String fileId, Integer delFlag) {
        fileIdList.add(fileId);
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFilePid(fileId);
        query.setDelFlag(delFlag);
        query.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        List<FileInfo> fileInfoList = this.fileInfoMapper.selectList(query);
        for (FileInfo fileInfo : fileInfoList) {
            findAllsubFolderFileList(fileIdList, userId, fileInfo.getFileId(), delFlag);
        }
    }

    @Override
    public void recoverFileBatch(String userId, String fileIds) {
        String[] fileIdArray = fileIds.split(",");
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFileIdArray(fileIdArray);
        query.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        List<FileInfo> fileInfoList = this.fileInfoMapper.selectList(query);
        List<String> delFileSubFloderIdList = new ArrayList<>();
        for (FileInfo fileInfo : fileInfoList) {
            if (FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFolderType())) {
                findAllsubFolderFileList(delFileSubFloderIdList, userId, fileInfo.getFileId(), FileDelFlagEnums.DEL.getFlag());
            }
        }
        //查到所有根目录的文件
        query = new FileInfoQuery();
        query.setFileId(userId);
        query.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        query.setFilePid(Constants.ZERO_STR);
        List<FileInfo> allRootFileList = this.findListByParam(query);
        Map<String, FileInfo> rootFileMap = allRootFileList.stream().collect(Collectors.toMap(FileInfo::getFileName, Function.identity(), (data1, data2) -> data2));
        //查询所有所选文件，将目录所有删除的文件给你更新为使用中
        if (!delFileSubFloderIdList.isEmpty()) {
            FileInfo fileInfo = new FileInfo();
            fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
            this.fileInfoMapper.updateFileDelFlagBatch(fileInfo, userId, delFileSubFloderIdList, null, FileDelFlagEnums.DEL.getFlag());
        }
        // 将选中的文件更新为正常，且父级目录到根目录
        List<String> delFileList = new ArrayList<>();
        FileInfo fileInfo = new FileInfo();
        fileInfo.setDelFlag(FileDelFlagEnums.USING.getFlag());
        fileInfo.setFilePid(Constants.ZERO_STR);
        fileInfo.setLastUpdateTime(new Date());
        this.fileInfoMapper.updateFileDelFlagBatch(fileInfo, userId, null, delFileList, FileDelFlagEnums.RECYCLE.getFlag());
        //将所选文件进行重命名
        for (FileInfo item : fileInfoList) {
            FileInfo rootFileInfo = rootFileMap.get(item);
            //文件名已经存在，重命名，被还原到文件名
            if (rootFileInfo != null) {
                String fileName = StringTools.rename(item.getFileName());
                FileInfo updateInfo = new FileInfo();
                updateInfo.setFileName(fileName);
                this.fileInfoMapper.updateByFileIdAndUserId(updateInfo, item.getFileId(), userId);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delFileBatch(String userId, String fileIds, Boolean adminOp) {
        String[] fileIdArray = fileIds.split(",");
        FileInfoQuery query = new FileInfoQuery();
        query.setUserId(userId);
        query.setFileIdArray(fileIdArray);
        query.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        List<FileInfo> fileInfoList = this.fileInfoMapper.selectList(query);
        List<String> delFileSubFileFolderFileList = new ArrayList<>();
        //找到所选文件夹子目录文件id
        for (FileInfo fileInfo : fileInfoList) {
            if (FileFolderTypeEnums.FOLDER.getType().equals(fileInfo.getFileType())) {
                findAllsubFolderFileList(delFileSubFileFolderFileList, userId, fileInfo.getFileId(), FileDelFlagEnums.DEL.getFlag());
            }
        }
        //删除所有选文件，子目录中的文件
        if (!delFileSubFileFolderFileList.isEmpty()) {
            this.fileInfoMapper.delFileBatch(userId, delFileSubFileFolderFileList, null, adminOp ? null : FileDelFlagEnums.DEL.getFlag());
        }
        // 删除所选文件
        this.fileInfoMapper.delFileBatch(userId, Arrays.asList(fileIdArray), null, adminOp ? null : FileDelFlagEnums.RECYCLE.getFlag());
        Long userSpace = this.fileInfoMapper.selectUseSpace(userId);
        UserInfo userInfo = new UserInfo();
        userInfo.setUseSpace(userSpace);
        this.userInfoMapper.updateByUserId(userInfo, userId);
        //更新缓存
        UserSpaceDto userSpaceDto = redisCompomnent.getUserSpaceUse(userId);
        userSpaceDto.setUseSpace(userSpace);
        redisCompomnent.saveUserSpaceUse(userId, userSpaceDto);
    }
}