package com.huotu.sis.controller.sis;

/*
 * 版权所有:杭州火图科技有限公司
 * 地址:浙江省杭州市滨江区西兴街道阡陌路智慧E谷B幢4楼
 *
 * (c) Copyright Hangzhou Hot Technology Co., Ltd.
 * Floor 4,Block B,Wisdom E Valley,Qianmo Road,Binjiang District
 * 2013-2015. All rights reserved.
 */

import com.huotu.sis.model.sis.ResultModel;
import com.huotu.sis.service.StaticResourceService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.net.URI;
import java.util.UUID;

/**
 * 图片上传
 * Created by slt on 2016/1/26.
 */
@Controller
@RequestMapping(value = "/sis")
public class ImageUploadController {
    private static Log log = LogFactory.getLog(ImageUploadController.class);

    @Autowired
    StaticResourceService staticResourceService;



    /**
     * 图片上传,(如：后台上传分享图片,富文本，目前只有后台上传)
     * @return
     * @throws Exception
     */
    @RequestMapping(value = "/UploadImg",method = RequestMethod.POST)
    @ResponseBody
    public ResultModel UploadImg(@RequestParam(value = "imgFile")MultipartFile shareImage, HttpServletResponse response) throws Exception{
        ResultModel resultModel=new ResultModel();
        //文件格式判断
        if (ImageIO.read(shareImage.getInputStream()) == null) {
            resultModel.setCode(0);
            resultModel.setMessage("请上传图片文件！");
            return resultModel;
        }
        if (shareImage.getSize() == 0) {
            resultModel.setCode(0);
            resultModel.setMessage("请上传图片！");
            return resultModel;
        }
        //保存图片
        String fileName = StaticResourceService.IMG + UUID.randomUUID().toString() + ".png";
        URI uri=staticResourceService.uploadResource(fileName, shareImage.getInputStream());
//        log.info("imageUrl:"+uri.toString());
        String path=uri.getPath();
//        response.setHeader("X-frame-Options", "SAMEORIGIN");
        resultModel.setCode(1);
        resultModel.setError(0);
        resultModel.setMessage("/"+fileName);
        resultModel.setUrl(uri.toString());
        return resultModel;

    }
}
