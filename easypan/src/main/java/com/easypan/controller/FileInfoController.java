package com.easypan.controller;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.annotation.VerifyParam;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.dto.UploadResultDto;
import com.easypan.entity.enums.FileCategoryEnums;
import com.easypan.entity.enums.FileDelFlagEnums;
import com.easypan.entity.enums.FileFolderTypeEnums;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.vo.FileInfoVO;
import com.easypan.entity.vo.PaginationResultVO;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.service.FileInfoService;
import com.easypan.utils.CopyTools;
import com.easypan.utils.StringTools;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

/**
 * 文件信息表 Controller
 */
@RestController("fileInfoController")
@RequestMapping("/file")
public class FileInfoController extends CommonFileController {

    @Resource
    private FileInfoService fileInfoService;

    /**
     * 根据条件分页查询
     */
    private List<FileInfoVO> getFileInfoVOList(List<FileInfo> list) {
        List<FileInfoVO> ans = new ArrayList<>();
        for (FileInfo fileInfo : list) {
            FileInfoVO cur = new FileInfoVO();
            cur.setFileCover(fileInfo.getFileCover());
            cur.setFileId(fileInfo.getFileId());
            cur.setFileCategory(fileInfo.getFileCategory());
            cur.setFileName(fileInfo.getFileName());
            cur.setFilePid(fileInfo.getFilePid());
            cur.setFileSize(fileInfo.getFileSize());
            cur.setFileType(fileInfo.getFileType());
            cur.setCreateTime(fileInfo.getCreateTime());
            cur.setStatus(fileInfo.getStatus());
            cur.setLastUpdateTime(fileInfo.getLastUpdateTime());
            ans.add(cur);
        }
        return ans;
    }

    private PaginationResultVO<FileInfoVO> getFileInfoVo(PaginationResultVO<FileInfo> resultVO1) {
        PaginationResultVO<FileInfoVO> resultVO = new PaginationResultVO<>();
        List<FileInfoVO> list = getFileInfoVOList(resultVO1.getList());
        resultVO.setList(list);
        resultVO.setPageNo(resultVO1.getPageNo());
        resultVO.setPageSize(resultVO1.getPageSize());
        resultVO.setPageTotal(resultVO1.getPageTotal());
        resultVO.setTotalCount(resultVO1.getTotalCount());
        return resultVO;
    }

    /**
     * 分页查询
     * @param session
     * @param query
     * @param category
     * @return
     */
    @RequestMapping("/loadDataList")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO loadDataList(HttpSession session, FileInfoQuery query, String category) {
        FileCategoryEnums categoryEnum = FileCategoryEnums.getByCode(category);
        if (null != categoryEnum) {
            query.setFileCategory(categoryEnum.getCategory());
        }
        query.setUserId(getUserInfoFromSession(session).getUserId());
        query.setOrderBy("last_update_time desc");
        query.setDelFlag(FileDelFlagEnums.USING.getFlag());
        PaginationResultVO<FileInfo> result = fileInfoService.findListByPage(query);
        PaginationResultVO<FileInfoVO> resultVO = getFileInfoVo(result);
        return getSuccessResponseVO(resultVO);
    }

    @RequestMapping("/uploadFile")
    @GlobalInterceptor(checkParams = true)
    public ResponseVO uploadFile(HttpSession session, String fileId, MultipartFile file,

                                 @VerifyParam(required = true) String fileName,

                                 @VerifyParam(required = true) String filePid,

                                 @VerifyParam(required = true) String fileMd5,

                                 @VerifyParam(required = true) Integer chunkIndex,

                                 @VerifyParam(required = true) Integer chunks) {

        SessionWebUserDto sessionWebUserDto = getUserInfoFromSession(session);
        UploadResultDto uploadResultDto = fileInfoService.updaloadFile(sessionWebUserDto, fileId, file, fileName, filePid, fileMd5, chunkIndex, chunks);
        return getSuccessResponseVO(uploadResultDto);
    }


    @RequestMapping("/getImage/{imageFolder}/{imageName}")
    public void getImage(HttpServletResponse response, @PathVariable("imageFolder") String imageFolder, @PathVariable("imageName") String imageName) {
        super.getImage(response, imageFolder, imageName);
    }


    @RequestMapping("/ts/getVideoInfo/{fileId}")

    public void getImage(HttpServletResponse response, HttpSession session, @PathVariable("fileId") String fileId) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        super.getFile(response, fileId, webUserDto.getUserId());
    }

    @RequestMapping("/getFile/{fileId}")
    public void getFile(HttpServletResponse response, HttpSession session, @PathVariable("fileId") String fileId) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        super.getFile(response, fileId, webUserDto.getUserId());
    }

    /**
     * 创建新目录
     *
     * @param session
     * @param fileId
     * @param fileName
     * @return
     */
    @RequestMapping("/newFoloder")
    public ResponseVO newFoloder(HttpSession session,

                                 @VerifyParam(required = true) String fileId, @VerifyParam(required = true) String fileName) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        FileInfo fileInfo = fileInfoService.newFolder(fileId, webUserDto.getUserId(), fileName);
        return getSuccessResponseVO(CopyTools.copy(fileInfo, FileInfoVO.class));
    }

    /**
     * 获取当前目录
     *
     * @param session
     * @param path
     * @return
     */
    @RequestMapping("/getFolderInfo")
    public ResponseVO getFolderInfo(HttpSession session,

                                    @VerifyParam(required = true) String path) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        return super.getFolderInfo(path, webUserDto.getUserId());
    }


    /**
     * 重命名
     *
     * @param session
     * @param fileId
     * @param fileName
     * @return
     */
    @RequestMapping("/rename")
    public ResponseVO rename(HttpSession session,

                             @VerifyParam(required = true) String fileId, @VerifyParam(required = true) String fileName) {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        FileInfo fileInfo = fileInfoService.rename(fileId, webUserDto.getUserId(), fileName);
        return getSuccessResponseVO(CopyTools.copy(fileInfo, FileInfoVO.class));
    }

    @RequestMapping("/loadAllFolder")
    public ResponseVO loadAllFolder(HttpSession session, @VerifyParam(required = true) String filePid,

                                    String currentFileIds) {

        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        FileInfoQuery fileInfoQuery = new FileInfoQuery();
        fileInfoQuery.setUserId(webUserDto.getUserId());
        fileInfoQuery.setFilePid(filePid);
        fileInfoQuery.setFolderType(FileFolderTypeEnums.FOLDER.getType());
        if (StringTools.isEmpty(currentFileIds)) {
            fileInfoQuery.setExcludeFileIdArray(currentFileIds.split(","));
        }
        fileInfoQuery.setDelFlag(FileDelFlagEnums.USING.getFlag());
        fileInfoQuery.setOrderBy("create_time desc");
        List<FileInfo> fileInfoList = fileInfoService.findListByParam(fileInfoQuery);
        return getSuccessResponseVO(getFileInfoVOList(fileInfoList));
    }

    /**
     * 移动文件
     *
     * @param session
     * @param fileIdS
     * @param filePid
     * @return
     */
    @RequestMapping("/changeFileFolders")
    public ResponseVO changeFileFolders(HttpSession session, @VerifyParam(required = true) String fileIdS,

                                        @VerifyParam(required = true) String filePid) {

        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        fileInfoService.changeFileFolder(fileIdS, filePid, webUserDto.getUserId());
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/createDownloadUrl/{fileId}")
    public ResponseVO createDownloadUrl(HttpSession session, @VerifyParam(required = true) @PathVariable("fileId") String fileId,

                                        @VerifyParam(required = true) String filePid) {

        SessionWebUserDto webUserDto = getUserInfoFromSession(session);

        return super.createDownloadUrl(fileId, webUserDto.getUserId());
    }

    @RequestMapping("/download/{code}")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public void dowload(HttpServletRequest request, HttpServletResponse response,

                        @VerifyParam(required = true) @PathVariable("code") String code) throws Exception {

        super.download(request, response, code);
    }

    @RequestMapping("/delFile")
    @GlobalInterceptor(checkParams = true, checkLogin = false)
    public ResponseVO delFile(HttpSession session,

                              @VerifyParam(required = true) @PathVariable("fileIds") String fileIds) throws Exception {
        SessionWebUserDto webUserDto = getUserInfoFromSession(session);
        fileInfoService.removeFile2RecycleBatch(webUserDto.getUserId(), fileIds);
        return null;
    }


}