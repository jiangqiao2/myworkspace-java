package com.easypan.controller;

import com.easypan.annotation.GlobalInterceptor;
import com.easypan.annotation.VerifyParam;
import com.easypan.entity.dto.SessionWebUserDto;
import com.easypan.entity.enums.FileDelFlagEnums;
import com.easypan.entity.po.FileInfo;
import com.easypan.entity.query.FileInfoQuery;
import com.easypan.entity.vo.PaginationResultVO;
import com.easypan.entity.vo.ResponseVO;
import com.easypan.service.FileInfoService;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;

/**
 * 回收站控制器
 */
@RestController("RecycleController")
@RequestMapping("/recycle")
public class RecycleController extends ABaseController {

    @Resource
    private FileInfoService fileInfoService;

    /**
     * 显示回收站文件
     * @param session
     * @param pageNo
     * @param pageSize
     * @return
     */
    @RequestMapping("/loadRecycleList")
    @GlobalInterceptor
    public ResponseVO loadRecycleList(HttpSession session, Integer pageNo, Integer pageSize) {
        FileInfoQuery query = new FileInfoQuery();
        query.setPageNo(pageNo);
        query.setPageSize(pageSize);
        query.setUserId(getUserInfoFromSession(session).getUserId());
        query.setOrderBy("recovery_time desc");
        query.setDelFlag(FileDelFlagEnums.RECYCLE.getFlag());
        PaginationResultVO result = fileInfoService.findListByPage(query);
        return getSuccessResponseVO(convert2PaginationVO(result, FileInfo.class));
    }

    /**
     * 将文件放在回收站
     * @param session
     * @param fileIds
     * @return
     */
    @RequestMapping("/recoverFile")
    @GlobalInterceptor
    public ResponseVO recoverFile(HttpSession session, @VerifyParam(required = true) String fileIds) {
        SessionWebUserDto webUserDto = new SessionWebUserDto();
        fileInfoService.recoverFileBatch(webUserDto.getUserId(), fileIds);
        return getSuccessResponseVO(null);
    }

    /**
     * 删除文件
     * @param session
     * @param fileIds
     * @return
     */
    @RequestMapping("/delFile")
    @GlobalInterceptor
    public ResponseVO delFile(HttpSession session, @VerifyParam(required = true) String fileIds) {
        SessionWebUserDto webUserDto = new SessionWebUserDto();
        fileInfoService.delFileBatch(webUserDto.getUserId(), fileIds, false);
        return getSuccessResponseVO(null);
    }
}
