/*
 * 版权所有:杭州火图科技有限公司
 * 地址:浙江省杭州市滨江区西兴街道阡陌路智慧E谷B幢4楼
 *
 * (c) Copyright Hangzhou Hot Technology Co., Ltd.
 * Floor 4,Block B,Wisdom E Valley,Qianmo Road,Binjiang District
 * 2013-2015. All rights reserved.
 */

package com.huotu.sis.service;
/**
 * 通用变量定义
 * Created by lgh on 2015/9/23.
 */

public interface CommonConfigService {


    /**
     * 资源地址
     *
     * @return
     */
    String getResoureServerUrl();


    /**
     * 获取后台主域名
     * @return
     */
    String getMainHost();


    /**
     * 上传资源的服务地址
     * @return
     */
    String getResourcesHome();


    /**
     * 静态资源域名地址
     * @return
     */
    String getResourcesUri();


}
